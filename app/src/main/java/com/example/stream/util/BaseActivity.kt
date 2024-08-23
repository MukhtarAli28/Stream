package com.example.stream.util

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.stream.ui.fragment.InfoBottomSheet

abstract class BaseActivity : AppCompatActivity {
    private var dialog: CustomProgressDialog? = null

    constructor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    fun showInfoDialog(title: String?, desc: String?) {
        InfoBottomSheet(
            this,
            title,
            desc
        ).show(
            supportFragmentManager,
            "show_info_dialog_fragment"
        )
    }

    fun showProgressBar(): CustomProgressDialog {
        if (dialog == null) dialog =
            CustomProgressDialog(this)
        return dialog!!
    }

    fun progressHide() {
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
        }
    }
}
