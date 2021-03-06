package com.wulee.administrator.zuji.ui;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;

import com.wulee.administrator.zuji.R;
import com.wulee.administrator.zuji.base.BaseActivity;
import com.wulee.administrator.zuji.utils.ImageUtil;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by wulee on 2017/8/25 13:50
 */

public class BigSingleImgActivity extends BaseActivity {

    @InjectView(R.id.iv_bigimg)
    ImageView ivBigimg;

    public static final String IMAGE_URL = "image_url";

    private String imgUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.big_single_image);
        ButterKnife.inject(this);

        imgUrl = getIntent().getStringExtra(IMAGE_URL);
        ImageUtil.setDefaultImageView(ivBigimg,imgUrl,R.mipmap.bg_pic_def_rect,this);
    }

}
