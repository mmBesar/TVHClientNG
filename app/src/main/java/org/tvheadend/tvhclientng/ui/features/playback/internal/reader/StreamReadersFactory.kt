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

class StreamReadersFactory {

    fun createStreamReader(streamType: String): StreamReader? {
        when (streamType.uppercase()) {
            // Video Stream Types
            "H264" -> return H264StreamReader()
            "HEVC", "H265" -> return H265StreamReader()
            "MPEG2VIDEO" -> return Mpeg2VideoStreamReader()
            // Audio Stream Types
            "AAC", "MP4A" -> return AacStreamReader()
            "AC3" -> return Ac3StreamReader()
            "EAC3" -> return Eac3StreamReader()
            "MPEG2AUDIO", "MP2A" -> return Mpeg2AudioStreamReader()
            "VORBIS" -> return VorbisStreamReader()
            // Text Stream Types
            "TEXTSUB" -> return TextsubStreamReader()
            "DVBSUB" -> return DvbsubStreamReader()
            else -> return null
        }
    }
}
