package google.myapplication;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.lang.ref.WeakReference;

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
 * time：2016/8/5 12:41
 * des ：${TODO}
 * gitVersion：$Rev$
 * updateAuthor：$Author$
 * updateDate：$Date$
 * updateDes：${TODO}
 * ============================================================
 **/
public class AsyncDrawable extends BitmapDrawable {
    private final WeakReference bitmapWorkerTaskReference;

    public AsyncDrawable(Resources res, Bitmap bm,BitmapWorkerTask task){
        super(res,bm);
        bitmapWorkerTaskReference = new WeakReference(task);
    }

    public BitmapWorkerTask getBitmapWorkerTask(){
        return (BitmapWorkerTask) bitmapWorkerTaskReference.get();
    }

}
