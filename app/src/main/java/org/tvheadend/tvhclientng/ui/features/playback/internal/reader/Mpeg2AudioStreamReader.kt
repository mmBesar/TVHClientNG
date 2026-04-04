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

package org.tvheadend.tvhclientng.ui.features.playback.internal.reader

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import org.tvheadend.htsp.HtspMessage
import org.tvheadend.tvhclientng.ui.features.playback.internal.utils.TvhMappings

internal class Mpeg2AudioStreamReader : PlainStreamReader(C.TRACK_TYPE_AUDIO) {

    override fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        var rate = Format.NO_VALUE
        if (stream.containsKey("rate")) {
            rate = TvhMappings.sriToRate(stream.getInteger("rate"))
        }

        var audioVersion = 2
        if (stream.containsKey("audio_version")) {
            audioVersion = stream.getInteger("audio_version")
        }

        val mimeType: String = when (audioVersion) {
            1 -> MimeTypes.AUDIO_MPEG_L1
            2 -> MimeTypes.AUDIO_MPEG_L2
            3 -> MimeTypes.AUDIO_MPEG
            else -> throw RuntimeException("Unknown MPEG Audio Version: $audioVersion")
        }

        return Format.Builder()
            .setId(streamIndex.toString())
            .setSampleMimeType(mimeType)
            .setChannelCount(stream.getInteger("channels", Format.NO_VALUE))
            .setSampleRate(rate)
            .setSelectionFlags(C.SELECTION_FLAG_AUTOSELECT)
            .setLanguage(stream.getString("language", "und"))
            .build()
    }

    override val trackType: Int
        get() = C.TRACK_TYPE_AUDIO
}
