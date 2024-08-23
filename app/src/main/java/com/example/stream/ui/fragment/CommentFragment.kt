package com.example.stream.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stream.R
import com.example.stream.adapter.CommentAdapter
import com.example.stream.databinding.FragmentCommentBinding
import com.example.stream.ui.activity.CommentModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CommentFragment(private var mContext: Context
) :
    BottomSheetDialogFragment() {

    private lateinit var mBinding: FragmentCommentBinding
    private lateinit var database: DatabaseReference

    private var callbackMember: EnterUserNameFragment.CallBack =
        object : EnterUserNameFragment.CallBack {
            override fun onItemClicked() {
                val commentText = mBinding.editTextComment.text.toString().trim()
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                saveCommentToFirebase(commentText, userId)

            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_comment,
            container,
            false
        )
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().reference
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        mBinding.ivComment.setOnClickListener {
            val commentText = mBinding.editTextComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                checkIfUserNameExists(userId) { exists ->
                    if (exists) {
                        saveCommentToFirebase(commentText, userId)
                    } else {
                        EnterUserNameFragment(
                            mContext,callbackMember
                        ).show(
                            parentFragmentManager,
                            "comment"
                        )
                    }
                }
            } else {
                Toast.makeText(mContext, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }

        }
        fetchComments()
    }

    private fun checkIfUserNameExists(userId: String?, callback: (Boolean) -> Unit) {
        if (userId != null) {
            database.child("username").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            })
        } else {
            callback(false)
        }
    }

    private fun saveCommentToFirebase(commentText: String, userId: String?) {
        val commentRef = database.child("comment").child(userId!!).push() // Generating a unique key for each comment

        commentRef.setValue(commentText).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                mBinding.editTextComment.text!!.clear()  // Clearing the EditText after saving
            } else {
                Toast.makeText(mContext, "Failed to add comment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchComments() {
        database.child("comment").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val commentsList = mutableListOf<CommentModel>()
                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key
                    for (commentSnapshot in userSnapshot.children) {
                        val commentText = commentSnapshot.value.toString()

                        if (uid != null) {
                            fetchUserName(uid) { userName ->
                                commentsList.add(CommentModel(uid, userName, commentText))
                                updateRecyclerView(commentsList)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun fetchUserName(uid: String, callback: (String) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            if (uid == currentUserId) {
                callback("You")
            } else {
                database.child("username").child(uid).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userName = snapshot.value.toString()
                        callback(userName)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }
    }

    private fun updateRecyclerView(commentsList: List<CommentModel>) {
        mBinding.rvComment.layoutManager = LinearLayoutManager(
            mContext, LinearLayoutManager.VERTICAL, false
        )
        val adapter = CommentAdapter(mContext,commentsList)
        mBinding.rvComment.adapter = adapter
    }


}