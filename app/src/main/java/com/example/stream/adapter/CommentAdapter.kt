package com.example.stream.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.stream.R
import com.example.stream.ui.activity.CommentModel
import com.example.stream.util.BaseActivity

class CommentAdapter(
    private val mContext: Context,
    private val comments: List<CommentModel>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.x_item_comment_layout, parent, false)
        return CommentViewHolder(view, mContext)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
        holder.divider.visibility = if (position == itemCount - 1) View.GONE else View.VISIBLE

    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(itemView: View, private val mContext: Context) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: AppCompatTextView = itemView.findViewById(R.id.tvUserName)
        private val commentTextView: AppCompatTextView = itemView.findViewById(R.id.tvComment)
        val divider: View = itemView.findViewById(R.id.divider)


        fun bind(comment: CommentModel) {
            userNameTextView.text = comment.userName

            if (comment.comment.length > 50) {
                val text = comment.comment.substring(0, 50) + "..."
                commentTextView.text = HtmlCompat.fromHtml(
                    "$text<font color=#24A7DF>Read More</font>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            } else {
                commentTextView.text = comment.comment
            }

            commentTextView.setOnClickListener {
                (mContext as BaseActivity).showInfoDialog(
                    "Comment",
                    comment.comment
                )
            }
        }
    }
}

