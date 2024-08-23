package com.example.stream.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import com.example.stream.R;
import com.example.stream.databinding.LayoutInfoBottomsheetBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


public class InfoBottomSheet extends BottomSheetDialogFragment {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    View view;
    LayoutInfoBottomsheetBinding mBinding;
    Context mContext;
    String title, desc;
    private boolean onlyDesc = false;

    public InfoBottomSheet() {
    }

    @SuppressLint("ValidFragment")
    public InfoBottomSheet(@NonNull Context context, String title, String desc) {
        mContext = context;
        this.title = title;
        this.desc = desc;
    }

    public InfoBottomSheet(Context context, String title, String desc, boolean onlyDesc) {
        mContext = context;
        this.title = title;
        this.desc = desc;
        this.onlyDesc = onlyDesc;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //bottom sheet round corners can be obtained but the while background appears to remove that we need to add this.
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppBottomSheetDialogTheme);
        setCancelable(false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.inflate(inflater, R.layout.layout_info_bottomsheet, container, false);

        mBinding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
                FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setPeekHeight(0);
            }
        });

        mBinding.tvTitle.setText(title);
        mBinding.tvDescription.setText(desc);
        mBinding.tvTitle.setVisibility(!onlyDesc ? View.VISIBLE : View.GONE);
        mBinding.ivClose.setOnClickListener(v -> dismiss());
        mBinding.nestedScrollView.post(() -> mBinding.nestedScrollView.fullScroll(View.FOCUS_DOWN));


        return mBinding.getRoot();
    }
}
