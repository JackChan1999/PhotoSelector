package com.google.photoselector.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * ============================================================
 * Copyright：Google有限公司版权所有 (c) 2016
 * Author：   陈冠杰
 * Email：    815712739@qq.com
 * GitHub：   https://github.com/JackChen1999
 * <p/>
 * Project_Name：PhotoSelector
 * Package_Name：com.google.photoselector.imageloader
 * Version：1.0
 * time：2016/8/3 17:12
 * des ：图片加载
 * gitVersion：
 * updateAuthor：
 * updateDate：
 * updateDes：
 * ============================================================
 **/
public class ImageLoader {
    private Handler mUIHandler;
    private Handler mPollThreadHandler;
    private Semaphore mSemaphoreThreadPool;
    private Semaphore mSemaphorePollThreadHandler = new Semaphore(0);
    //private Thread mPollThread;
    private HandlerThread mThread;// 轮询线程，在后台去执行下载图片任务
    private ExecutorService mThreadPool;
    private LinkedList<Runnable> mTaskQueue;
    private LruCache<String, Bitmap> mLruCache;

    private static int DEFAULT_THREAD_COUNT = 3;
    private String mMaxWidth = "mMaxWidth";
    private String mMaxHeight = "mMaxHeight";

    private Type mType = Type.LIFO;

    private enum Type {
        FIFO, LIFO
    }

    private static ImageLoader mInstance;

    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    public static ImageLoader getInstance() {
        return getInstance(DEFAULT_THREAD_COUNT, Type.LIFO);
    }

    public static ImageLoader getInstance(int threadCount, Type type) {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    private void init(int threadCount, Type type) {
        mType = type;
        mSemaphoreThreadPool = new Semaphore(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mThreadPool = Executors.newFixedThreadPool(threadCount);

        initBackThread();

        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    public void initBackThread(){

        mThread = new HandlerThread("PollThread");
        mThread.start();

        mPollThreadHandler = new Handler(mThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                mThreadPool.execute(getTask());
                try {
                    mSemaphoreThreadPool.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        mSemaphorePollThreadHandler.release();


        /*mPollThread = new Thread() {
            @Override
            public void run() {
                super.run();
                Looper.prepare();
                mPollThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                mSemaphorePollThreadHandler.release();
                Looper.loop();
            }
        };
        mPollThread.start();*/
    }

    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    public void loadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    ImageHolder holder = (ImageHolder) msg.obj;
                    Bitmap bm = holder.bm;
                    String path = holder.path;
                    ImageView imageView = holder.imageView;
                    if (path.equals(imageView.getTag().toString())) {
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }

        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            refreshImageView(path, bm, imageView);
        } else {
            addTask(new Runnable() {
                @Override
                public void run() {
                    ImageSize imageSize = getImageViewSize(imageView);
                    Bitmap bm = decodeBitmapFromPath(path, imageSize);
                    addBitmapToLruCache(path, bm);
                    refreshImageView(path, bm, imageView);
                    mSemaphoreThreadPool.release();
                }
            });
        }
    }

    private Bitmap decodeBitmapFromPath(String path, ImageSize imageSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, imageSize.width, imageSize.height);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }

           /* long totalPixels = width * height / inSampleSize;

            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels /= 2;
            }*/
        }

        return inSampleSize;
    }

    private ImageSize getImageViewSize(ImageView imageView) {
        DisplayMetrics metrics = imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();

        int width = imageView.getWidth();
        if (width <= 0) {
            width = lp.width;
        }
        if (width <= 0) {
            width = getImageViewFieldValue(imageView, mMaxWidth);
        }
        if (width <= 0) {
            width = metrics.widthPixels;
        }

        int height = imageView.getWidth();
        if (height <= 0) {
            height = lp.height;
        }
        if (height <= 0) {
            height = getImageViewFieldValue(imageView, mMaxHeight);
        }
        if (height <= 0) {
            height = metrics.heightPixels;
        }

        ImageSize imageSize = new ImageSize();
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    private int getImageViewFieldValue(Object obj, String fieldname) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldname);
            field.setAccessible(true);
            int fieldvalue = field.getInt(obj);
            if (fieldvalue > 0 && fieldvalue < Integer.MAX_VALUE) {
                value = fieldvalue;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }

    private synchronized void addTask(Runnable r) {
        mTaskQueue.add(r);
        if (mPollThreadHandler == null) {
            try {
                mSemaphorePollThreadHandler.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mPollThreadHandler.sendEmptyMessage(0);// 给轮询线程发送消息
    }

    private void refreshImageView(String path, Bitmap bm, ImageView imageView) {
        Message msg = Message.obtain();
        ImageHolder holder = new ImageHolder();
        holder.bm = bm;
        holder.path = path;
        holder.imageView = imageView;
        msg.obj = holder;
        mUIHandler.sendMessage(msg);
    }

    private Bitmap getBitmapFromLruCache(String path) {
        return mLruCache.get(path);
    }

    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == null) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    class ImageSize {
        protected int width;
        protected int height;
    }

    class ImageHolder {
        protected Bitmap bm;
        protected String path;
        protected ImageView imageView;
    }
}
