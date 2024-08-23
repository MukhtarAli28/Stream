package com.example.stream.adapter

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.MediaController
import androidx.annotation.OptIn
import androidx.databinding.DataBindingUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.example.stream.R
import com.example.stream.databinding.VideoListLayoutBinding
import com.example.stream.ui.activity.VideoFullView

class VideoListAdapter(private val mContext: Context, private val videoUrlList: ArrayList<String>) :
    RecyclerView.Adapter<VideoListAdapter.ViewHolder>() {

    class ViewHolder(val mBinder: VideoListLayoutBinding) : RecyclerView.ViewHolder(mBinder.root)

    private var currentPlayingPosition: Int = -1
    private var exoPlayer: ExoPlayer? = null
    private var isMuted: Boolean = false
    private lateinit var mAudioManager: AudioManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(mContext),
                R.layout.video_list_layout,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return videoUrlList.size
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (videoUrlList[position].isNotEmpty()) {
            exoPlayer = ExoPlayer.Builder(mContext).build()
            val mc = MediaController(mContext)
            holder.mBinder.plVideo.setMediaController(mc)
            val videoUrl = Uri.parse(videoUrlList[position])
            holder.mBinder.plVideo.setVideoURI(videoUrl)

            holder.mBinder.plVideo.setOnPreparedListener { mp ->
                holder.mBinder.plVideo.start()
            }

            holder.mBinder.plVideo.setOnClickListener(object : View.OnClickListener {
            private var lastClickTime: Long = 0

            override fun onClick(v: View?) {
                val clickTime = System.currentTimeMillis()
                if (clickTime - lastClickTime < ViewConfiguration.getDoubleTapTimeout()) {
                    // Double-click detected
                    mContext.startActivity(
                    Intent(mContext, VideoFullView::class.java).putExtra(
                        "videoUrl",
                        videoUrlList[position]
                    )
                )
                }
                lastClickTime = clickTime
            }
        })

            mc.visibility = View.GONE

            /*holder.mBinder.cvVideoView.setOnClickListener {
                if (isMuted) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 100, 0)
                } else {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                }
                isMuted = !isMuted
            }*/
        }
    }

    private fun toggleMute(holder: ViewHolder) {
        holder.mBinder.plVideo.setOnPreparedListener { mediaPlayer ->
            isMuted = !isMuted
            mediaPlayer.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
        }
    }

    @OptIn(UnstableApi::class)
    fun playVideo() {
        exoPlayer?.playWhenReady = true
        exoPlayer?.audioComponent?.volume = if (isMuted) 0f else 1f  // Apply mute state
    }

    @OptIn(UnstableApi::class)
    fun stopVideo() {
        exoPlayer?.playWhenReady = false
        exoPlayer?.audioComponent?.volume = 0f  // Mute the volume when paused
    }
}