package com.example.stream.viewmodel

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler

class VolumeObserver(
    private val context: Context,
    handler: Handler,
    private val onVolumeChange: (Int) -> Unit
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        val volume = getSystemVolume()
        onVolumeChange(volume)
    }

    private fun getSystemVolume(): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
}

