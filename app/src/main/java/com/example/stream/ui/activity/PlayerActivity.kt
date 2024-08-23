package com.example.stream.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.media3.common.Player
import com.example.stream.util.CustomPlaybackState
import com.example.stream.viewmodel.PlayerViewModel
import com.example.stream.viewmodel.QualityViewModel
import com.example.stream.R
import com.example.stream.viewmodel.SubtitleViewModel
import com.example.stream.util.track.TrackEntity
import com.example.stream.ui.track.TrackSelectionDialog
import com.example.stream.viewmodel.VolumeObserver
import com.example.stream.databinding.ActivityPlayerBinding
import com.example.stream.databinding.ExoPlayerViewBinding
import com.example.stream.extension.gone
import com.example.stream.extension.hideSystemUI
import com.example.stream.extension.resolveSystemGestureConflict
import com.example.stream.extension.setImageButtonTintColor
import com.example.stream.extension.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private val binding: ActivityPlayerBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    private val exoBinding: ExoPlayerViewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ExoPlayerViewBinding.bind(binding.root)
    }
    private lateinit var audioManager: AudioManager
    private val viewModel: PlayerViewModel by viewModels()
    private val subtitleViewModel: SubtitleViewModel by viewModels()
    private val qualityViewModel: QualityViewModel by viewModels()

    private val volumeObserver = VolumeObserver(this, Handler()) { volume ->
        val isMuted = volume == 0
        exoBinding.exoControllerPlaceholder.muteButton.setMuteState(isMuted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        viewModel.onActivityCreate(this)
        qualityViewModel.onActivityCreated()
        subtitleViewModel.onActivityCrated()
        resolveSystemGestureConflict()
        initClickListeners()
        setupVolumeControl()
        val colorPrimary = ContextCompat.getColor(this, R.color.colorPrimary)
        setSeekBarTint(exoBinding.exoControllerPlaceholder.volumeSeekBar, colorPrimary)

        viewModel.playerLiveData.observe(this@PlayerActivity) { exoPlayer ->
            binding.exoPlayerView.player = exoPlayer

            // Update the start and end time initially
            updateStartEndTime()

            // Update the title when the player changes
            updateVideoTitle()

            // Listen for position changes and update the UI accordingly
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    super.onPlaybackStateChanged(state)
                    updateStartEndTime()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    updateStartEndTime()
                }
            })
        }

        with(viewModel) {
            playerLiveData.observe(this@PlayerActivity) { exoPlayer ->
                binding.exoPlayerView.player = exoPlayer
                updateVideoTitle()  // Update title when the player changes
            }

            playbackStateLiveData.observe(this@PlayerActivity) { playbackState ->
                setProgressbarVisibility(playbackState)
                setVideoControllerVisibility(playbackState)
            }

            isMuteLiveData.observe(this@PlayerActivity, exoBinding.exoControllerPlaceholder.muteButton::setMuteState)
        }

        with(qualityViewModel) {
            qualityEntitiesLiveData.observe(this@PlayerActivity, ::setupQualityButton)
            onQualitySelectedLiveData.observe(this@PlayerActivity) {
                dismissTrackSelectionDialogIfExist()
            }
        }

        with(subtitleViewModel) {
            subtitleEntitiesLiveData.observe(this@PlayerActivity, ::setupSubtitleButton)
            onSubtitleSelectedLiveData.observe(this@PlayerActivity) {
                dismissTrackSelectionDialogIfExist()
            }
        }

        // Register ContentObserver
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, volumeObserver
        )

    }


    override fun onDestroy() {
        super.onDestroy()
        // Unregister ContentObserver
        binding.exoPlayerView.player?.apply {
            // Set playWhenReady to false to pause playback
            playWhenReady = false
        }
        contentResolver.unregisterContentObserver(volumeObserver)
        releasePlayerView()
    }

    private fun initClickListeners() {
        with(exoBinding.exoControllerPlaceholder) {
            exoBackButton.setOnClickListener { onBackPressed() }
            playPauseButton.setOnClickListener { viewModel.onPlayButtonClicked() }
            muteButton.setOnClickListener { viewModel.onMuteClicked() }
            replayButton.setOnClickListener { viewModel.onReplayClicked() }
            ivOrientation.setOnClickListener { toggleOrientation() }
        }
    }

    private fun setSeekBarTint(seekBar: SeekBar, color: Int) {
        // Set the tint for the thumb
        seekBar.thumb?.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)

        // Set the tint for the progress drawable
        seekBar.progressDrawable?.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
    }

    private fun toggleOrientation() {
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
    private fun setProgressbarVisibility(playbackState: CustomPlaybackState) {
        binding.progressBar.isVisible = playbackState == CustomPlaybackState.LOADING
    }

    private fun setVideoControllerVisibility(playbackState: CustomPlaybackState) {
        exoBinding.exoControllerPlaceholder.run {
            playPauseButton.setState(playbackState)
            when (playbackState) {
                CustomPlaybackState.PLAYING,
                CustomPlaybackState.PAUSED,
                -> {
                    root.visible()
                    replayButton.gone()
                }

                CustomPlaybackState.ERROR,
                CustomPlaybackState.ENDED,
                -> {
                    replayButton.visible()
                }

                else -> {
                    replayButton.gone()
                }
            }
        }
    }

    private fun setupQualityButton(qualities: List<TrackEntity>) {
        exoBinding.exoControllerPlaceholder.qualityButton.apply {
            if (qualities.isNotEmpty()) {
                setImageButtonTintColor(R.color.white)
                setOnClickListener { openTrackSelectionDialog(qualities) }
            }
        }
    }

    private fun setupSubtitleButton(subtitles: List<TrackEntity>) {
        exoBinding.exoControllerPlaceholder.subtitleButton.apply {
            if (subtitles.isNotEmpty()) {
                setImageButtonTintColor(R.color.white)
                setOnClickListener { openTrackSelectionDialog(subtitles) }
            }
        }
    }

    private fun openTrackSelectionDialog(items: List<TrackEntity>) {
        dismissTrackSelectionDialogIfExist()
        TrackSelectionDialog.newInstance(items).show(supportFragmentManager, DIALOG_TAG)
    }

    private fun dismissTrackSelectionDialogIfExist() {
        with(supportFragmentManager) {
            val previousDialog = findFragmentByTag(DIALOG_TAG)
            if (previousDialog != null) {
                (previousDialog as TrackSelectionDialog).dismiss()
                beginTransaction().remove(previousDialog).commit()
            }
        }
    }

    private fun updateVideoTitle() {
        val title = getCurrentMediaTitle()
        Log.e("askhbakjne",title.toString())
//        binding.videoTitleTextView.text = title
    }

    private fun getCurrentMediaTitle(): String? {
        val mediaItem = binding.exoPlayerView.player?.currentMediaItem
        return mediaItem?.mediaMetadata?.title?.toString()
    }

    private fun updateMuteButtonState() {
        val volume =
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).getStreamVolume(AudioManager.STREAM_MUSIC)
        val isMuted = volume == 0
        exoBinding.exoControllerPlaceholder.muteButton.setMuteState(isMuted)

    }

    private fun releasePlayerView() {
        with(binding.exoPlayerView) {
            removeAllViews()
            player = null
        }
    }

    companion object {
        const val PLAYER_PARAMS_EXTRA = "playerParamsExtra"
        private const val DIALOG_TAG = "dialogTag"
    }

    private fun setupVolumeControl() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Initialize SeekBar with current volume level
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        exoBinding.exoControllerPlaceholder.volumeSeekBar.max = maxVolume
        exoBinding.exoControllerPlaceholder.volumeSeekBar.progress = currentVolume

        // Set up listener for SeekBar changes
        exoBinding.exoControllerPlaceholder.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        updateVolumeSeekBar()
    }


    private fun updateVolumeSeekBar() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Initialize SeekBar with current volume level
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        exoBinding.exoControllerPlaceholder.volumeSeekBar.max = maxVolume
        exoBinding.exoControllerPlaceholder.volumeSeekBar.progress = currentVolume
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI(binding.root)
        updateMuteButtonState()
        updateVideoTitle()
        binding.exoPlayerView.player?.apply {
            playWhenReady = true
        }

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        registerReceiver(volumeReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        binding.exoPlayerView.player?.apply {
            // Set playWhenReady to false to pause playback
            playWhenReady = false
        }
        unregisterReceiver(volumeReceiver)
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                updateVolumeSeekBar()
            }
        }
    }

    private fun updateStartEndTime() {
        val player = binding.exoPlayerView.player ?: return

        // Start time is always 0 for non-live content
        val startTime = 0L

        // Check if the content is live
        if (player.isCurrentMediaItemLive) {
            // For live content, display "LIVE" instead of the end time
            exoBinding.exoControllerPlaceholder.exoEndPosition.text = "Live"
            exoBinding.exoControllerPlaceholder.ivDot1.visibility = View.VISIBLE

            exoBinding.exoControllerPlaceholder.exoEndPosition.setOnClickListener {
                if (!player.isPlaying) {
                    player.playWhenReady = true
                }
                // Optionally, seek to the current live position
                player.seekTo(player.duration)
            }

        } else {
            // Get the duration of the media item for non-live content
            val endTime = player.duration
            exoBinding.exoControllerPlaceholder.exoEndPosition.text = "Live"

//            exoBinding.exoControllerPlaceholder.exoEndPosition.text = formatTime(endTime)
            exoBinding.exoControllerPlaceholder.ivDot1.visibility = View.GONE
            exoBinding.exoControllerPlaceholder.exoEndPosition.setOnClickListener {
                player.seekTo(player.duration)
            }

        }

        // Set the start time
        exoBinding.exoControllerPlaceholder.exoPosition.text = formatTime(startTime)
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()

        return if (hours > 0) {
            // Format with hours
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            // Format without hours
            String.format("%02d:%02d", minutes, seconds)
        }
    }



}
