package com.example.stream.model

import java.io.Serializable

data class PlayerParams(
    val url: String,
    val subtitles: List<VideoSubtitle> = emptyList()
) : Serializable