package com.example.stream.util;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.databinding.DataBindingUtil;

import com.example.stream.R;
import com.example.stream.databinding.LoaderLayoutBinding;

public class CustomProgressDialog extends Dialog {
    private final Context context;

    public CustomProgressDialog(Context context) {
        super(context, R.style.ProgressDialog);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoaderLayoutBinding mBinder = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.loader_layout, null, false);
        setContentView(mBinder.getRoot());
        setCanceledOnTouchOutside(false);
        setCancelable(false);
        mBinder.fintooLoader.playAnimation();
//        Glide.with(context).asGif().transition(withCrossFade()).load(R.drawable.loader).into(mBinder.fintooLoader);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }
}
