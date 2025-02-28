package com.example.stream.dataSource

import android.content.Context
import androidx.annotation.StringRes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.stream.util.track.MediaTrack
import com.example.stream.util.track.TrackEntity
import com.example.stream.util.track.TrackMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class TrackSelectorDataSource @Inject constructor(
    @ApplicationContext val context: Context,
) {

    private var _trackSelector: DefaultTrackSelector? =
        DefaultTrackSelector(context, AdaptiveTrackSelection.Factory())
    val trackSelector: DefaultTrackSelector = requireNotNull(_trackSelector)

    private var _trackMapper: TrackMapper? = TrackMapper(trackSelector)
    val trackMapper: TrackMapper = requireNotNull(_trackMapper)

    fun updateSelectedTrack(
        index: Int,
        tracks: List<MediaTrack>,
        rendererIndex: Int,
    ): MediaTrack? {

        val newSelectedTrack = if (index > 0 && index <= tracks.size) {
            tracks[index - 1]
        } else {
            null
        }


        trackSelector.parameters = trackSelector.buildUponParameters()
//            .clearOverridesOfType(rendererIndex)
            .clearSelectionOverrides(rendererIndex)
            .apply {
                newSelectedTrack?.also { track ->
//                    addOverride(TrackSelectionOverride(track.trackGroupArray.get(track.rendererIndex), track.rendererIndex))
                    setSelectionOverride(
                        track.rendererIndex,
                        track.trackGroupArray,
                        track.selectionOverride
                    )
                }
            }.build()

        return newSelectedTrack
    }

    fun updateTrackEntities(
        tracks: List<MediaTrack>,
        selectedTrack: MediaTrack?,
        @StringRes defaultTextRes: Int,
        onItemClick: (Int) -> Unit,
    ): List<TrackEntity>? {
        if (tracks.isEmpty()) return null
        return tracks.mapIndexed { index, track ->
            TrackEntity(
                track.trackName,
                index = index + 1,
                isSelected = selectedTrack == track
            ) {
                onItemClick(index + 1)
            }
        }.toMutableList().apply {
            add(
                0,
                TrackEntity(
                    context.getString(defaultTextRes),
                    index = 0,
                    isSelected = selectedTrack == null
                ) {
                    onItemClick(0)
                }
            )
        }
    }

    fun nullifyTrackSelector() {
        _trackMapper = null
        _trackSelector = null
    }
}
