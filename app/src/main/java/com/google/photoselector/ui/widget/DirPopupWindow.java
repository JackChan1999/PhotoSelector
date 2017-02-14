package com.google.photoselector.ui.widget;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.google.photoselector.R;
import com.google.photoselector.bean.FolderBean;
import com.google.photoselector.imageloader.ImageLoader;
import com.google.photoselector.ui.adapter.abslistview.CommonAdapter;
import com.google.photoselector.ui.adapter.abslistview.ViewHolder;

import java.util.List;

/**
 * ============================================================
 * Copyright：Google有限公司版权所有 (c) 2016
 * Author：   陈冠杰
 * Email：    815712739@qq.com
 * GitHub：   https://github.com/JackChen1999
 * <p/>
 * Project_Name：PhotoSelector
 * Package_Name：com.google.photoselector.ui.widget
 * Version：1.0
 * time：2016/8/4 21:13
 * des ：popuwindow
 * gitVersion：
 * updateAuthor：
 * updateDate：
 * updateDes：
 * ============================================================
 **/
public class DirPopupWindow extends PopupWindow{

    private int mWidth;
    private int mHeight;
    private ListView mListView;
    private List<FolderBean> mData;
    private FolderBean preSelFolder;

    public DirPopupWindow(Context context, List<FolderBean> data) {
        super(context);
        mData = data;
        calWidthAndHeight(context);
        initViews(context);
        initListener();
    }

    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        mWidth = metrics.widthPixels;
        mHeight = (int) (metrics.heightPixels*0.7);
    }

    private void initViews(Context context) {
        View contentView = LayoutInflater.from(context).inflate(R.layout.imageup_pop,null);
        setContentView(contentView);
        setWidth(mWidth);
        setHeight(mHeight);
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE){
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        mListView = (ListView) contentView.findViewById(R.id.imgupPopLv);
        mListView.setAdapter(new DirAdapter(context,R.layout.imageup_pop_item,mData));
    }

    private void initListener() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (null != mLisetner) {
                    if (null != preSelFolder) {
                        preSelFolder.setSel(false);
                    }
                    FolderBean bean = (FolderBean) parent
                            .getItemAtPosition(position);
                    bean.setSel(true);
                    preSelFolder = bean;
                    mLisetner.onSelected(bean);
                }
            }
        });
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        super.showAsDropDown(anchor, xoff, yoff);

    }

    private class DirAdapter extends CommonAdapter<FolderBean>{

        public DirAdapter(Context context, int layoutId, List<FolderBean> datas) {
            super(context, layoutId, datas);
        }

        @Override
        protected void convert(ViewHolder holder, FolderBean item, int position) {
            ImageView img = holder.getView(R.id.popIvYl);
            img.setImageResource(R.mipmap.pictures_no);
            ImageLoader.getInstance().loadImage(item.getFirstImgPath(), img);
            holder.setText(R.id.popTvDirName, item.getDirName());
            holder.setText(R.id.popTvDirImgCount, item.getImgCountStr());

            img = holder.getView(R.id.popImgSel);
            int visible = item.isSel() ? View.VISIBLE : View.GONE;
            img.setVisibility(visible);
        }
    }

    private onDirSelectedListener mLisetner;

    public interface onDirSelectedListener {
        public void onSelected(FolderBean folder);
    }

    public void setOnDirSelectedLisetner(onDirSelectedListener mLisetner) {
        this.mLisetner = mLisetner;
    }
}
