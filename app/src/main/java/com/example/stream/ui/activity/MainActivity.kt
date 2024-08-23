package com.example.stream.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.stream.util.BaseActivity
import com.example.stream.model.PlayerParams
import com.example.stream.R
import com.example.stream.model.VideoSubtitle
import com.example.stream.databinding.ActivityMainBinding
import com.example.stream.extension.startPlayer
import com.example.stream.ui.fragment.CommentFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var isMuted: Boolean = false
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        exoPlayer = ExoPlayer.Builder(this).build()
        mBinding.exoPlayerView.player = exoPlayer

        // Prepare media item
        val mediaItem =
            MediaItem.fromUri("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8")
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        mBinding.clParent.setOnClickListener {

        }
        showProgressBar().show()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY && exoPlayer.playbackState == Player.STATE_READY) {
                    if (exoPlayer.playWhenReady) {

                        val handler = Handler()
                        handler.postDelayed({
                            progressHide()
                            mBinding.groupVideo.visibility = View.VISIBLE
                            exoPlayer.removeListener(this)
                        }, 300)

                    }
                }
            }
        })
        // Set up mute/unmute button
        mBinding.cvMute.setOnClickListener {
            toggleMute()
        }
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")

        registerReceiver(volumeReceiver, filter)


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        if (auth.currentUser == null) {
            signInAnonymously()
        } else {
            updateLikeCount()
            updateCommentCount()
            updateViewCount()
            checkIfUidExists()
        }

        mBinding.ivLike.setOnClickListener {
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            userUid?.let {
                toggleLike(it)
            }
        }
        mBinding.ivComment.setOnClickListener {
            CommentFragment(
                this
            ).show(
                supportFragmentManager, "comment"
            )
        }

        mBinding.cvFullVideo.setOnClickListener {
            val subtitleList: MutableList<VideoSubtitle> = mutableListOf()
            startPlayer(
                PlayerParams(
                    "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                    subtitleList
                )
            )
            saveCountOfPlay()
        }
    }
    private fun saveCountOfPlay() {
        val commentRef = database.child("countOfPlay").child(auth.currentUser!!.uid).push() // Generate a unique key for each comment

        commentRef.setValue("").addOnCompleteListener { task ->

        }
    }


    private fun toggleMute() {
        if (isMuted) {
            // Unmute
            exoPlayer.volume = 1f
            mBinding.ivMute.setImageResource(R.drawable.ic_exo_unmute)
        } else {
            // Mute
            exoPlayer.volume = 0f
            mBinding.ivMute.setImageResource(R.drawable.ic_exo_mute)
        }
        isMuted = !isMuted
    }

    private fun updateLikeCount() {
        val likeRef = database.child("like")
        likeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                mBinding.tvLike.text = count.toString()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun checkIfUidExists() {
        val likeRef = auth.currentUser?.uid?.let { database.child("like").child(it) }
        likeRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if the snapshot has children, which means the UID exists
                val exists = snapshot.exists()
                if (exists){
                    mBinding.ivLike.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MainActivity, R.drawable.ic_like_filled
                        )
                    )
                }else{
                    mBinding.ivLike.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MainActivity, R.drawable.ic_like
                        )
                    )
                }

            }

            override fun onCancelled(error: DatabaseError) {
                mBinding.ivLike.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.ic_like
                    )
                )
            }
        })
    }


    private fun updateCommentCount() {
        val commentRef = database.child("comment")
        commentRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalCount = 0
                for (userSnapshot in snapshot.children) {
                    totalCount += userSnapshot.childrenCount.toInt()
                }
                mBinding.tvComment.text = totalCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }


    private fun updateViewCount() {
        val viewRef = database.child("countOfPlay")
        viewRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalCount = 0
                for (userSnapshot in snapshot.children) {
                    totalCount += userSnapshot.childrenCount.toInt()
                }
                mBinding.tvView .text = totalCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }


    private fun toggleLike(uid: String) {
        val likeRef = database.child("like").child(uid)

        likeRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                likeRef.removeValue().addOnSuccessListener {
                    mBinding.ivLike.setImageDrawable(
                        ContextCompat.getDrawable(
                            this, R.drawable.ic_like
                        )
                    )
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to remove like: ${it.message}", LENGTH_SHORT)
                        .show()
                }
            } else {
                likeRef.setValue(true).addOnSuccessListener {
                    mBinding.ivLike.setImageDrawable(
                        ContextCompat.getDrawable(
                            this, R.drawable.ic_like_filled
                        )
                    )
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to add like: ${it.message}", LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to check like status: ${it.message}", LENGTH_SHORT).show()
        }
    }


    private fun signInAnonymously() {
        auth.signInAnonymously().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Sign-in success
                val user = auth.currentUser
                val commentRef = database.child("user").child(user?.uid!!)
                commentRef.setValue(user.uid).addOnCompleteListener { _ ->
                    updateLikeCount()
                    updateCommentCount()
                    updateViewCount()
                    checkIfUidExists()

                }

            } else {
                Toast.makeText(
                    this, "Anonymous Login Failed: ${task.exception?.message}", LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        exoPlayer.playWhenReady = true

    }

    override fun onPause() {
        super.onPause()
        exoPlayer.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(volumeReceiver)
        exoPlayer.release()
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Check if volume has been changed
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                isMuted = currentVolume == 0
                mBinding.ivMute.setImageResource(
                    if (isMuted) R.drawable.ic_exo_mute else R.drawable.ic_exo_unmute
                )
                exoPlayer.volume = if (isMuted) 0f else 1f
            }
        }
    }


}

data class CommentModel(
    val uid: String,
    val userName: String,
    val comment: String,
)
