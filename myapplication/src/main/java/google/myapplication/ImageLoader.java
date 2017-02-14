package google.myapplication;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

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
 * time：2016/8/5 12:40
 * des ：${TODO}
 * gitVersion：$Rev$
 * updateAuthor：$Author$
 * updateDate：$Date$
 * updateDes：${TODO}
 * ============================================================
 **/
public class ImageLoader {

    public void loadBitmap(ImageView imageView, int resid){
        Resources res = imageView.getResources();
        if (cancelPotentialWork(resid,imageView)){
            BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            AsyncDrawable asyncDrawable = new AsyncDrawable(res,bitmap,task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(resid);
        }

    }

    public static boolean cancelPotentialWork(int data, ImageView imageView){
        BitmapWorkerTask task = getBitmapWorkerTask(imageView);
        if (task != null){
            int bitmapData = task.data;
            if (bitmapData == 0 || bitmapData != data){
                task.cancel(true);
            }else {
                return false;
            }
        }
        return true;
    }

    public static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView){
        if (imageView != null){
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable){
                AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }
}
