package com.google.photoselector.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.photoselector.R;
import com.google.photoselector.imageloader.ImageLoader;
import com.google.photoselector.ui.photoview.PhotoView;

import java.util.List;

/**
 * ============================================================
 * Copyright：${TODO}有限公司版权所有 (c) 2016
 * Author：   陈冠杰
 * Email：    815712739@qq.com
 * GitHub：   https://github.com/JackChen1999
 * <p/>
 * Project_Name：PhotoSelector
 * Package_Name：com.google.photoselector.ui.activity
 * Version：1.0
 * time：2016/8/6 21:25
 * des ：${TODO}
 * gitVersion：$Rev$
 * updateAuthor：$Author$
 * updateDate：$Date$
 * updateDes：
 * ============================================================
 **/
public class ImageDetailActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private List<String> mData;
    private LayoutInflater mInflater;
    private int mPostion;
    private ImageLoader mLoader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_imagedetail);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mInflater = LayoutInflater.from(this);
        mLoader = ImageLoader.getInstance();

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        initData();
    }

    private void initData() {
        Intent intent = getIntent();
        if (null != intent) {
            mData = intent.getStringArrayListExtra("imageUrls");
            mPostion = intent.getIntExtra("position", 0);
        }
        ImageAdapter adapter = new ImageAdapter();
        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(mPostion, false);
    }

    private class ImageAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View      view = mInflater.inflate(R.layout.layout_image, container, false);
            PhotoView iv   = (PhotoView) view.findViewById(R.id.iv);
             mLoader.loadImage(mData.get(position), iv);
            //Glide.with(ImageDetailActivity.this).load(mData.get(position)).into(iv);
            //PhotoViewAttacher attacher = new PhotoViewAttacher(iv);
            //attacher.update();

            container.addView(view);
            return view;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
