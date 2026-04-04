package org.tvheadend.tvhclientng.ui.features.playback.internal

import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.flv.FlvExtractor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.ts.Ac3Extractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.ts.PsExtractor
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.extractor.wav.WavExtractor

internal class TvheadendExtractorsFactory : ExtractorsFactory {

    override fun createExtractors(): Array<Extractor> {
        return arrayOf(
            HtspSubscriptionExtractor(),
            MatroskaExtractor(),
            FragmentedMp4Extractor(),
            Mp4Extractor(),
            Mp3Extractor(),
            AdtsExtractor(),
            Ac3Extractor(),
            TsExtractor(),
            FlvExtractor(),
            OggExtractor(),
            PsExtractor(),
            WavExtractor()
        )
    }
}
