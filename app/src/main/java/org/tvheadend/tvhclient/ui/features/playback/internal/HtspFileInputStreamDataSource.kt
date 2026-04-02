/*
 * Copyright (c) 2017 Kiall Mac Innes <kiall@macinnes.ie>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tvheadend.tvhclient.ui.features.playback.internal

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import org.tvheadend.htsp.HtspConnection
import org.tvheadend.htsp.HtspMessage
import org.tvheadend.api.ServerMessageListener
import org.tvheadend.api.ServerResponseListener
import timber.log.Timber
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

class HtspFileInputStreamDataSource private constructor(val connection: HtspConnection) : DataSource, Closeable, ServerMessageListener<HtspMessage>, HtspDataSourceInterface {

    private val dataSourceCount = AtomicInteger()
    private val htspConnection: HtspConnection = connection
    private lateinit var dataSpec: DataSpec
    private var dataSourceNumber = 0
    private lateinit var byteBuffer: ByteBuffer
    private var fileName: String? = null
    private var fileId = -1
    private var fileSize: Long = -1
    private var filePosition: Long = 0

    class Factory internal constructor(htspConnection: HtspConnection) : DataSource.Factory {

        private val htspConnection: HtspConnection
        private var dataSource: HtspFileInputStreamDataSource? = null

        override fun createDataSource(): DataSource {
            Timber.d("Created new data source from factory")
            dataSource = HtspFileInputStreamDataSource(htspConnection)
            return dataSource!!
        }

        val currentDataSource: HtspFileInputStreamDataSource?
            get() {
                Timber.d("Returning data source")
                return dataSource
            }

        fun releaseCurrentDataSource() {
            Timber.d("Releasing data source")
            dataSource?.release()
        }

        init {
            Timber.d("Initializing file input stream data source factory")
            this.htspConnection = htspConnection
        }
    }

    init {
        Timber.d("Initializing file input data source")
        htspConnection.addMessageListener(this)
        dataSourceNumber = dataSourceCount.incrementAndGet()
    }

    override val timeshiftOffsetPts: Long
        get() = Long.MIN_VALUE

    override val timeshiftStartTime: Long
        get() = Long.MIN_VALUE

    override val timeshiftStartPts: Long
        get() = Long.MIN_VALUE

    override fun setSpeed(tvhSpeed: Int) {}
    override fun resume() {}
    override fun pause() {}

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun open(spec: DataSpec): Long {
        Timber.d("Opening file input data source $dataSourceNumber)")
        dataSpec = spec
        fileName = "dvrfile" + dataSpec.uri.path

        val fileReadRequest = HtspMessage()
        fileReadRequest["method"] = "fileRead"
        fileReadRequest["size"] = 1024000

        val lock = ReentrantLock()
        val condition = lock.newCondition()

        val fileReadHandler = object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                if (response.containsKey("error")) {
                    Timber.d("Error reading file at offset 0: %s", response.getString("error"))
                } else {
                    val data = response.getByteArray("data")
                    Timber.d("Fetched %s bytes of file at filePosition %s", data.size, filePosition)
                    filePosition += data.size.toLong()
                    byteBuffer = ByteBuffer.wrap(data)
                }
                lock.withLock { condition.signal() }
            }
        }

        val fileOpenRequest = HtspMessage()
        fileOpenRequest["method"] = "fileOpen"
        fileOpenRequest["file"] = fileName

        htspConnection.sendMessage(fileOpenRequest, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                if (response.containsKey("error")) {
                    Timber.d("Error opening file: %s", response.getString("error"))
                } else {
                    Timber.d("Opening file: %s", fileName)
                    fileId = response.getInteger("id")
                    if (response.containsKey("size")) {
                        fileSize = response.getLong("size")
                        Timber.v("Opened file $fileName of size $fileSize successfully")
                    } else {
                        Timber.v("Opened file $fileName successfully")
                    }
                    fileReadRequest["id"] = fileId
                    htspConnection.sendMessage(fileReadRequest, fileReadHandler)
                }
            }
        })

        lock.withLock {
            try {
                condition.await(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Timber.d(e, "Waiting for fileReadRequest message was interrupted")
            }
        }

        Timber.d("Opened file $fileName, id $fileId with size $fileSize")
        return fileSize
    }

    override fun read(bytes: ByteArray, offset: Int, readLength: Int): Int {
        Timber.d("Read %s at offset %s with length %s", bytes.size, offset, readLength)
        if (fileSize == filePosition && !byteBuffer.hasRemaining()) {
            Timber.d("File has been read, returning -1")
            return C.RESULT_END_OF_INPUT
        }

        sendFileRead(filePosition)

        if (!byteBuffer.hasRemaining() && fileSize == -1L) {
            return C.RESULT_END_OF_INPUT
        } else if (!byteBuffer.hasRemaining()) {
            Timber.d("Failed to read data for %s, returning -1", fileName)
            return C.RESULT_END_OF_INPUT
        }

        byteBuffer[bytes, offset, min(readLength, byteBuffer.remaining())]
        val bytesRead = byteBuffer.position() - offset
        Timber.d("Read %s bytes", bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? {
        Timber.d("Returning data spec uri of %s", dataSpec.uri)
        return dataSpec.uri
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        return emptyMap()
    }

    override fun close() {
        Timber.d("Closing file input data source $dataSourceNumber)")
    }

    override fun onMessage(response: HtspMessage, method: String) {}

    private fun release() {
        Timber.d("Releasing file input data source $dataSourceNumber)")
        val request = HtspMessage()
        request["method"] = "fileClose"
        request["id"] = fileId
        htspConnection.sendMessage(request, null)
        htspConnection.removeMessageListener(this)
    }

    private fun sendFileRead(offset: Long) {
        Timber.d("Sending message to read file from offset %s", offset)

        if (byteBuffer.hasRemaining()) {
            Timber.d("Buffer has elements remaining, returning")
            return
        }

        var size: Long = 1024000
        if (fileSize != -1L) {
            if (offset + size > fileSize) {
                size = fileSize - offset
            }
        }

        val request = HtspMessage()
        request["method"] = "fileRead"
        request["id"] = fileId
        request["size"] = size
        request["offset"] = offset
        Timber.d("Fetching $size bytes of file at offset $offset")

        val lock = ReentrantLock()
        val condition = lock.newCondition()

        htspConnection.sendMessage(request, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                if (response.containsKey("error")) {
                    Timber.d("Error reading file at $offset: ${response.getString("error")}")
                } else {
                    val data = response.getByteArray("data")
                    Timber.d("Fetched %s bytes of file at offset %s", data.size, offset)
                    filePosition += data.size.toLong()
                    byteBuffer = ByteBuffer.wrap(data)
                }
                lock.withLock { condition.signal() }
            }
        })

        lock.withLock {
            try {
                condition.await(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Timber.d(e, "Waiting for fileReadRequest message was interrupted.")
            }
        }
    }
}
