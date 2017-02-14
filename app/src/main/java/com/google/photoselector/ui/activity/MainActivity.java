package com.google.photoselector.ui.activity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.photoselector.R;
import com.google.photoselector.bean.FolderBean;
import com.google.photoselector.imageloader.ImageLoader;
import com.google.photoselector.ui.adapter.abslistview.CommonAdapter;
import com.google.photoselector.ui.adapter.abslistview.ViewHolder;
import com.google.photoselector.ui.widget.DirPopupWindow;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * ============================================================
 * Copyright：Google有限公司版权所有 (c) 2016
 * Author：   陈冠杰
 * Email：    815712739@qq.com
 * GitHub：   https://github.com/JackChen1999
 *
 * Project_Name：PhotoSelector
 * Package_Name：com.google.photoselector.ui.activity
 * Version：1.0
 * time：2016/8/4 22:02
 * des ：
 * gitVersion：
 * updateAuthor：
 * updateDate：
 * updateDes：
 * ============================================================
 **/
public class MainActivity extends AppCompatActivity{
    private final int DATA_LOADED = 1;
    private final String SEPERATOR = ",";

    private GridView mGridView;
    private List<String> lImgs;
    private ImageAdapter adapter;
    private ActionBar mActionBar;

    private TextView tvDirName;
    private TextView tvDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();
    private ProgressDialog mProgressDialog;
    private DirPopupWindow mDirPopupWindow;

    private FilenameFilter filter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")
                    || filename.endsWith(".png")) {
                return true;
            }
            return false;
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED) {
                FolderBean folder = (FolderBean) msg.obj;
                data2View(folder);
                mProgressDialog.dismiss();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        initListener();
        initDirPopupWindow();
    }

    private void initView() {
        //getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        //supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.imageup_main);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#424242")));
        //mActionBar.setBackgroundDrawable(new ColorDrawable(Color.argb(128, 0, 0, 0)));
        mActionBar.setTitle("选择图片");

        lImgs = new ArrayList<String>();
        adapter = new ImageAdapter(this, R.layout.imageup_item, lImgs);
        mGridView = (GridView) findViewById(R.id.imgupGridView);
        mGridView.setAdapter(adapter);

        tvDirName = (TextView) findViewById(R.id.imgupTvDirName);
        tvDirCount = (TextView) findViewById(R.id.imgupTvDirCount);
    }

    private void initData() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(this, "当前存储卡不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this, null, "正在加载……");

        Executors.newSingleThreadExecutor().execute(new Runnable() {

            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();

                String selection = MediaStore.Images.Media.MIME_TYPE
                        + "= ? or " + MediaStore.Images.Media.MIME_TYPE + "= ?";
                String[] selectionArgs = new String[] { "image/jpeg", "image/png" };
                String sortOrder = MediaStore.Images.Media.DATE_MODIFIED;
                Cursor cursor = cr.query(mImgUri, null, selection, selectionArgs, sortOrder);

                // 存储遍历过的parentFile，防止重复遍历
                Set<String> mDirPaths = new HashSet<String>();
                FolderBean folder = null;
                while (cursor.moveToNext()) {
                    // 拿到图片路径
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (null == parentFile) {
                        continue;
                    }

                    String dirPath = parentFile.getAbsolutePath();
                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    }

                    // 获取文件夹下所有图片的名字
                    String[] fileNames = parentFile.list(filter);
                    if (null == fileNames) {
                        continue;
                    }

                    mDirPaths.add(dirPath);
                    folder = new FolderBean();
                    folder.setDirPath(dirPath);
                    folder.setFirstImgPath(path);
                    folder.setImgCount(fileNames.length);
                    mFolderBeans.add(folder);
                }
                cursor.close();

                folder = new FolderBean();
                folder.setDirName("所有图片");
                folder.setFirstImgPath(mFolderBeans.get(0).getFirstImgPath());
                String imgPath = "";
                int imgCount = 0;
                for (FolderBean bean : mFolderBeans) {
                    imgPath += bean.getDirPath() + SEPERATOR;
                    imgCount += bean.getImgCount();
                }
                folder.setDirPath(imgPath.substring(0, imgPath.length() - 1));
                folder.setImgCount(imgCount);

                mFolderBeans.add(0, folder);
                gridViewDatas(folder);

                // 通知handler扫描图片完成
                Message msg = mHandler.obtainMessage();
                msg.what = DATA_LOADED;
                msg.obj = folder;
                mHandler.sendMessage(msg);
            }
        });
    }

    private void initListener() {
        tvDirName.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mDirPopupWindow.setAnimationStyle(R.style.popupwindow_anim);
                // 设置显示位置
                mDirPopupWindow.showAsDropDown(tvDirName, 0, 0);

                // 设置显示内容区域变暗
                lightSwitch(0.3f);
            }
        });

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this,ImageDetailActivity.class);
                intent.putExtra("position",position);
                intent.putStringArrayListExtra("imageUrls", (ArrayList<String>) lImgs);
                startActivity(intent);
            }
        });
    }

    private void initDirPopupWindow() {
        mDirPopupWindow = new DirPopupWindow(this, mFolderBeans);
        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                lightSwitch(1.0f);
            }
        });

        mDirPopupWindow.setOnDirSelectedLisetner(new DirPopupWindow.onDirSelectedListener() {
            @Override
            public void onSelected(FolderBean folder) {
                gridViewDatas(folder);
                data2View(folder);
                mDirPopupWindow.dismiss();
            }
        });
    }

    /**
     * 内容区域明暗度设置
     */
    private void lightSwitch(float alpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = alpha;
        getWindow().setAttributes(lp);
    }

    /**
     * 把扫描完成的数据显示在GridView中
     */
    private void data2View(FolderBean bean) {
        if (TextUtils.isEmpty(bean.getDirPath())) {
            Toast.makeText(this, "未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }

        adapter.notifyDataSetChanged();
        tvDirName.setText(bean.getDirName());
        tvDirCount.setText(bean.getImgCountStr());
    }

    private void gridViewDatas(FolderBean folder) {
        lImgs.clear();
        String[] dirPaths = folder.getDirPath().split(SEPERATOR);

        File selDir;
        String[] fileNames;
        for (String path : dirPaths) {
            selDir = new File(path);
            fileNames = selDir.list(filter);

            for (String stra : fileNames) {
                lImgs.add(path + File.separator + stra);
            }
        }
    }

    private class ImageAdapter extends CommonAdapter<String>{
        private final String COLOR = "#77000000";
        private ImageLoader loader;
        // 当切换文件夹的时候共享数据集
        public Set<String> sSelImg = new HashSet<String>();
        private int mScreenWidth;

        public ImageAdapter(Context context, int layoutId, List<String> datas) {
            super(context, layoutId, datas);
            loader = ImageLoader.getInstance();
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            mScreenWidth = metrics.widthPixels;
        }

        @Override
        protected void convert(ViewHolder viewHolder, final String path, int position) {
            final ImageView imgPic = viewHolder.getView(R.id.imgupItemImageView);
            final ImageView imgSel = viewHolder.getView(R.id.imgupItemSelect);

            imgPic.setMaxWidth(mScreenWidth/3);

            // 重置状态
            imgPic.setImageResource(R.drawable.empty_photo);
            imgPic.setColorFilter(null);
            loader.loadImage(path, imgPic);
            imgSel.setImageResource(R.mipmap.picture_unselected);

            if (sSelImg.contains(path)) {
                imgPic.setColorFilter(Color.parseColor(COLOR));
                imgSel.setImageResource(R.mipmap.pictures_selected);
            }

            imgSel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (sSelImg.contains(path)) {// 已经被选择
                        sSelImg.remove(path);
                        imgPic.setColorFilter(null);
                        imgSel.setImageResource(R.mipmap.picture_unselected);
                    } else {// 未被选择
                        sSelImg.add(path);
                        imgPic.setColorFilter(Color.parseColor(COLOR));
                        imgSel.setImageResource(R.mipmap.pictures_selected);
                    }
                }
            });
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            sSelImg.clear();// 清空之前所选择的数据
        }
    }
}
