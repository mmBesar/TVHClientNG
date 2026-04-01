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

package org.tvheadend.tvhclient.ui.features.playback.internal.reader

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import org.tvheadend.htsp.HtspMessage
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.TvhMappings

internal class AacStreamReader : StreamReader {

    private var mTrackOutput: TrackOutput? = null

    override fun createTracks(stream: HtspMessage, output: ExtractorOutput) {
        val streamIndex = stream.getInteger("index")
        mTrackOutput = output.track(streamIndex, C.TRACK_TYPE_AUDIO)
        mTrackOutput!!.format(buildFormat(streamIndex, stream))
    }

    override fun consume(message: HtspMessage) {
        val pts = message.getLong("pts")
        val payload = message.getByteArray("payload")
        val pba = ParsableByteArray(payload)

        val skipLength: Int = if (hasCrc(payload[1])) {
            ADTS_HEADER_SIZE + ADTS_CRC_SIZE
        } else {
            ADTS_HEADER_SIZE
        }

        pba.skipBytes(skipLength)
        val aacFrameLength = payload.size - skipLength

        mTrackOutput!!.sampleData(pba, aacFrameLength)
        mTrackOutput!!.sampleMetadata(pts, C.BUFFER_FLAG_KEY_FRAME, aacFrameLength, 0, null)
    }

    private fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        var rate = Format.NO_VALUE
        if (stream.containsKey("rate")) {
            rate = TvhMappings.sriToRate(stream.getInteger("rate"))
        }

        val channels = stream.getInteger("channels", Format.NO_VALUE)

        val initializationData: List<ByteArray> = if (stream.containsKey("meta")) {
            listOf(stream.getByteArray("meta"))
        } else {
            listOf(buildAacLcAudioSpecificConfig(rate, channels))
        }

        return Format.Builder()
            .setId(streamIndex.toString())
            .setSampleMimeType(MimeTypes.AUDIO_AAC)
            .setChannelCount(channels)
            .setSampleRate(rate)
            .setInitializationData(initializationData)
            .setSelectionFlags(C.SELECTION_FLAG_AUTOSELECT)
            .setLanguage(stream.getString("language", "und"))
            .build()
    }

    private fun hasCrc(b: Byte): Boolean {
        val data = b.toInt() and 0xFF
        return (data and 0x1) == 0
    }

    private fun buildAacLcAudioSpecificConfig(sampleRate: Int, channelCount: Int): ByteArray {
        // AAC-LC AudioSpecificConfig
        val sampleRateTable = intArrayOf(96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350)
        val sampleRateIndex = sampleRateTable.indexOfFirst { it == sampleRate }.let { if (it == -1) 4 else it }
        val channelIndex = if (channelCount == Format.NO_VALUE) 2 else channelCount
        val config = (2 shl 11) or (sampleRateIndex shl 7) or (channelIndex shl 3)
        return byteArrayOf((config shr 8).toByte(), (config and 0xFF).toByte())
    }

    companion object {
        private const val ADTS_HEADER_SIZE = 7
        private const val ADTS_CRC_SIZE = 2
    }
}
