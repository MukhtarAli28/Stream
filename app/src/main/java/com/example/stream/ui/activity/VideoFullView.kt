package com.example.stream.ui.activity

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.stream.R
import com.example.stream.databinding.ActivityVideoFullViewBinding

class VideoFullView : AppCompatActivity() {

    private lateinit var mBinding: ActivityVideoFullViewBinding
    private var videoLink = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_video_full_view)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        videoLink = intent.getStringExtra("videoUrl")!!
        val videoUri = Uri.parse(videoLink)
        mBinding.plVideo.setVideoURI(videoUri)

        mBinding.plVideo.setOnPreparedListener { mp ->
            mBinding.plVideo.start()
        }
    }
}