package google.myapplication.photowall;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import google.myapplication.R;

/**
 * ============================================================
 * Copyright：${TODO}有限公司版权所有 (c) 2016
 * Author：   陈冠杰
 * Email：    815712739@qq.com
 * GitHub：   https://github.com/JackChen1999
 * <p/>
 * Project_Name：PhotoSelector
 * Package_Name：google.myapplication.photowall
 * Version：1.0
 * time：2016/8/5 14:40
 * des ：${TODO}
 * gitVersion：$Rev$
 * updateAuthor：$Author$
 * updateDate：$Date$
 * updateDes：${TODO}
 * ============================================================
 **/
public class PhotoWallAdapter extends ArrayAdapter<String> {

    private GridView mGridView;
    private LayoutInflater mInflater;
    private Context mContext;
    private LruCache<String,Bitmap> mLruCache;
    private DiskLruCache mDiskCache;
    private int mItemHeight = 0;
    private Set<BitmapWorkerTask> mTasks;

    public PhotoWallAdapter(Context context, int resource, String[] objects, GridView gridView) {
        super(context, resource, objects);
        init(context,gridView);
    }

    private void init(Context context, GridView gridView) {
        mContext = context;
        mGridView = gridView;
        mInflater = LayoutInflater.from(context);

        int cacheMemory = (int) (Runtime.getRuntime().maxMemory()/8);
        mLruCache = new LruCache<String, Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };

        File cacheDir = getCacheDir("thumb");
        if (!cacheDir.exists()){
            cacheDir.mkdirs();
        }
        try {
            mDiskCache = DiskLruCache.open(cacheDir,getAppVersion(),1,10*1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String url = getItem(position);
        View view;
        if (convertView == null){
            view = mInflater.inflate(R.layout.activity_main,parent,false);
        }else {
            view = convertView;
        }
        ImageView iv = (ImageView) view.findViewById(R.id.iv);
        if (iv.getLayoutParams().height != mItemHeight){
            iv.getLayoutParams().height = mItemHeight;
        }
        iv.setTag(url);
        loadBitmap(url,iv);
        return view;
    }

    private void loadBitmap(String url, ImageView iv) {
        Bitmap bitmap = getBitmapFromMemoryCache(url);
        if (bitmap == null){
            BitmapWorkerTask task = new BitmapWorkerTask();
            mTasks.add(task);
            task.execute(url);
        }else {
            if (iv != null && bitmap != null){
                iv.setImageBitmap(bitmap);
            }
        }

    }

    public Bitmap getBitmapFromMemoryCache(String url){
        return mLruCache.get(url);
    }

    public void addBitmapToMemoryCache(String url,Bitmap bitmap){
        if (getBitmapFromMemoryCache(url) == null){
            if (bitmap != null){
                mLruCache.put(url,bitmap);
            }
        }
    }

    public File getCacheDir(String uniquename){
        String cachepath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()){
            cachepath = mContext.getExternalCacheDir().getPath();
        }else {
            cachepath = mContext.getCacheDir().getPath();
        }
        return new File(cachepath + File.separator + uniquename);
    }

    public int getAppVersion(){
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext
                    .getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    private String hashKeyForDisk(String key){
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(key.getBytes());
            return bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return String.valueOf(key.hashCode());
        }
    }

    public String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if (hex.length() == 1){
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public void cancelAllTask(){
        for (BitmapWorkerTask task : mTasks) {
        	task.cancel(false);
        }
    }

    public void flush(){
        if (mDiskCache != null){
            try {
                mDiskCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class BitmapWorkerTask extends AsyncTask<String,Void,Bitmap>{
        private String imageurl;

        @Override
        protected Bitmap doInBackground(String... params) {
            imageurl = params[0];
            FileInputStream in = null;
            FileDescriptor descriptor = null;
            DiskLruCache.Snapshot snapshot = null;
            String key = hashKeyForDisk(imageurl);
            try {
                snapshot = mDiskCache.get(key);
                if (snapshot == null){
                    DiskLruCache.Editor editor = mDiskCache.edit(key);
                    if (editor != null){
                        OutputStream out = editor.newOutputStream(0);
                        if (downloadUrlToStream(imageurl,out)){
                            editor.commit();
                        }else {
                            editor.abort();
                        }
                    }
                    snapshot = mDiskCache.get(key);
                }

                if (snapshot != null){
                    in = (FileInputStream) snapshot.getInputStream(0);
                    descriptor = in.getFD();
                }
                Bitmap bitmap = null;
                if (descriptor != null){
                    bitmap = BitmapFactory.decodeFileDescriptor(descriptor);
                }
                if (bitmap != null){
                    addBitmapToMemoryCache(imageurl,bitmap);
                }
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (descriptor == null && in != null){
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ImageView iv = (ImageView) mGridView.findViewWithTag(imageurl);
            if (iv != null && bitmap != null){
                iv.setImageBitmap(bitmap);
            }
        }

        public boolean downloadUrlToStream(String imageurl,OutputStream outputStream){
            HttpURLConnection conn = null;
            BufferedInputStream in = null;
            BufferedOutputStream out = null;

            try {
                URL url = new URL(imageurl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5*1000);
                conn.setReadTimeout(10*1000);
                if (conn.getResponseCode() == 200){
                    in = new BufferedInputStream(conn.getInputStream(),8*1024);
                    out = new BufferedOutputStream(outputStream,8*1024);
                    int b = 0;
                    while ((b = in.read()) != -1){
                        out.write(b);
                    }
                }
                return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (conn != null){
                    conn.disconnect();
                }

                if (in != null){
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (out != null){
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }
    }
}
