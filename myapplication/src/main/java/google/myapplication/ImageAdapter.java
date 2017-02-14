package google.myapplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * ============================================================
 * Copyright：${TODO}有限公司版权所有 (c) 2016
 * Author：   陈冠杰
 * Email：    815712739@qq.com
 * GitHub：   https://github.com/JackChen1999
 * <p/>
 * Project_Name：PhotoSelector
 * Package_Name：google.myapplication
 * Version：1.0
 * time：2016/8/5 18:41
 * des ：${TODO}
 * gitVersion：$Rev$
 * updateAuthor：$Author$
 * updateDate：$Date$
 * updateDes：${TODO}
 * ============================================================
 **/
public class ImageAdapter extends ArrayAdapter<String> {

    private LruCache<String,BitmapDrawable> mLruCache;
    private Bitmap loading;
    private ListView mListView;
    private final Resources mResources;

    public ImageAdapter(Context context, int resource) {
        super(context, resource);
        mResources = context.getResources();
        loading = BitmapFactory.decodeResource(mResources,R.id.iv);

        int cachememory = (int) (Runtime.getRuntime().maxMemory()/8);
        mLruCache = new LruCache<String, BitmapDrawable>(cachememory){
            @Override
            protected int sizeOf(String key, BitmapDrawable value) {
                return value.getBitmap().getByteCount();
            }
        };

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 4;
        options.inPreferredConfig = Bitmap.Config.RGB_8888;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mListView == null){
            mListView = (ListView) parent;
        }

        View view;
        if (convertView == null){
            view = LayoutInflater.from(getContext()).inflate(R.layout.activity_main,parent,false);
        }else {
            view = convertView;
        }

        String url = getItem(position);
        ImageView iv = (ImageView) view.findViewById(R.id.iv);
        BitmapDrawable drawable = getBitmapFromMemoryCache(url);
        if (drawable != null){
            iv.setImageDrawable(drawable);
        }else if (cancelBitmapTask(url,iv)){
            BitmapTask task = new BitmapTask(iv);
            AsyncDrawable asyncDrawable = new AsyncDrawable(mResources,loading,task);
            iv.setImageDrawable(asyncDrawable);
            task.execute(url);
        }
        return super.getView(position, convertView, parent);
    }

    public boolean cancelBitmapTask(String url, ImageView iv){
        BitmapTask task = getBitmapTask(iv);
        if (task != null){
            String imageurl = task.imageurl;
            if (imageurl == null || !imageurl.equals(url)){
                task.cancel(true);
            }else {
                return false;
            }
        }
        return true;
    }

    public void addBitmapToMemoryCache(String url, BitmapDrawable drawable){
        if (getBitmapFromMemoryCache(url) == null){
            if (drawable != null){
                mLruCache.put(url,drawable);
            }
        }
    }

    public BitmapDrawable getBitmapFromMemoryCache(String url){
        return mLruCache.get(url);
    }

    class AsyncDrawable extends BitmapDrawable{
        private WeakReference<BitmapTask> mReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,BitmapTask task) {
            super(res, bitmap);
            mReference = new WeakReference<BitmapTask>(task);
        }

        public BitmapTask getBitmapTask(){
            return mReference.get();
        }
    }

    public BitmapTask getBitmapTask(ImageView iv){
        if (iv != null){
            Drawable drawable = iv.getDrawable();
            if (drawable instanceof AsyncDrawable){
                AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapTask();
            }
        }
        return null;
    }

    class BitmapTask extends AsyncTask<String,Void,BitmapDrawable>{

        private String imageurl;
        private WeakReference<ImageView> mReference;

        public BitmapTask(ImageView iv){
            mReference = new WeakReference<ImageView>(iv);
        }

        @Override
        protected BitmapDrawable doInBackground(String... params) {
            imageurl = params[0];
            BitmapDrawable drawable = new BitmapDrawable(mResources,downloadBitmap(imageurl));
            addBitmapToMemoryCache(imageurl,drawable);
            return drawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable drawable) {
            super.onPostExecute(drawable);
            ImageView iv = getAttacheImageView();
            if (iv != null && drawable != null){
                iv.setImageDrawable(drawable);
            }
        }

        public ImageView getAttacheImageView(){
            ImageView iv = mReference.get();
            BitmapTask task = getBitmapTask(iv);
            if (task == this){
                return iv;
            }
            return null;
        }

        public Bitmap downloadBitmap(String imageurl){
            HttpURLConnection conn = null;
            Bitmap bitmap = null;
            try {
                URL url = new URL(imageurl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5*1000);
                conn.setReadTimeout(10*1000);
                bitmap = BitmapFactory.decodeStream(conn.getInputStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }
    }
}
