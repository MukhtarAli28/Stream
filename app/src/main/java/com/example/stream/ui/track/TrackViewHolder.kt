package com.example.stream.ui.track

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.example.stream.R
import com.example.stream.util.track.TrackEntity

class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val icon: ImageView = view.findViewById(R.id.icon)
    private val title: TextView = view.findViewById(R.id.title)

    fun bind(
        entity: TrackEntity,
        position: Int
    ) {
        itemView.setOnClickListener {
            entity.onItemClick(position)
        }
        title.text = entity.title
        icon.isInvisible = !entity.isSelected
    }
}