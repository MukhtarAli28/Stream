package com.example.stream.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.stream.R
import com.example.stream.databinding.FragmentEnterUsernameBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class EnterUserNameFragment(private var mContext: Context, private var itemClickCallBack: CallBack) :
    BottomSheetDialogFragment() {

    private lateinit var mBinding: FragmentEnterUsernameBinding
    private lateinit var database: DatabaseReference

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
            R.layout.fragment_enter_username,
            container,
            false
        )
        database = FirebaseDatabase.getInstance().reference

        mBinding.btnApply.setOnClickListener {

            if (mBinding.editTextComment.text!!.isEmpty()){
                Toast.makeText(mContext, "Please enter username.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                val comment = mBinding.editTextComment.text.toString()

                if (comment.isNotEmpty()) {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        saveCommentToDatabase(userId, comment)
                    } else {
                        Toast.makeText(mContext, "User not signed in", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(mContext, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }

        }

        return mBinding.root
    }

    private fun saveCommentToDatabase(userId: String, comment: String) {
        val commentsRef = database.child("username")

        commentsRef.child(userId).setValue(comment)
            .addOnSuccessListener {
                itemClickCallBack.onItemClicked()
                dialog!!.dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(mContext, "Failed to add username: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    interface CallBack{
        fun onItemClicked()
    }
}