package com.example.xiachen.myimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by xiachen on 15/11/13.
 */
public class ImageLoader {

    private static ImageLoader mInstance;
    private LruCache<String, Bitmap> mLruCache;

    private ExecutorService mThreadPool;

    private LinkedList<Runnable> mTaskQueue;
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    private Handler mUIHandler;
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    private Semaphore mSemaphoreThreadPool;
    private boolean isDiskCacheEnable = true;

    private static final int DEFAULT_THREAD_COUNT = 1;
    private static final String TAG = "ImageLoader";

    private Type mType = Type.LIFO;

    public enum Type {
        FIFO, LIFO
    }

    public static ImageLoader getInstance() {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(DEFAULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return mInstance;
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

    public ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    private void init(int threadCount, Type type) {
        initBackThread();
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<>();
        mType = type;
        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    private void initBackThread() {
        mPoolThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {

                        }
                    }
                };
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();
    }

    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    public void loadImage(final String path, final ImageView imageView, final boolean isFromNet) {
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView image = holder.imageView;
                    String path = holder.path;

                    if (imageView.getTag().equals(path)) {
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }
        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            refreashBitmap(path, imageView, bm);
        } else {
            addTask(buildTask(path, imageView, isFromNet));
        }
    }

    private Bitmap getBitmapFromLruCache(String path) {
        return mLruCache.get(path);
    }

    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mPoolThreadHandler == null) {
                mSemaphorePoolThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);

    }

    private Runnable buildTask(final String path, final ImageView imageView, final boolean isFromNet) {
        return new Runnable() {
            @Override
            public void run() {
                Bitmap bm = null;
                if (isFromNet) {
                    File file = getDiskCacheDir(imageView.getContext(), md5(path));
                    if (file.exists()) {
                        bm = loadImageFromLocal(file.getAbsolutePath(), imageView);
                    } else {
                        if (isDiskCacheEnable) {
                            boolean downloadState = DownloadImgUtils.downloadImgByUrl(path, file);
                            if (downloadState) {
                                bm = loadImageFromLocal(path, imageView);
                            }
                        } else {
                            bm = DownloadImgUtils.downloadImgByUrl(path, imageView);
                        }
                    }

                } else {
                    bm = loadImageFromLocal(path, imageView);
                }
                addBitmapToLruCache(path, bm);
                refreashBitmap(path, imageView, bm);
                mSemaphoreThreadPool.release();


            }
        };
    }

    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (mLruCache.get(path) != null) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    private File getDiskCacheDir(Context context, String s) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + s);
    }

    private String md5(String path) {
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] digest = md.digest(path.getBytes());
            return bytes2hex02(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String bytes2hex02(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            String tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1) {
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        return sb.toString();
    }

    private Bitmap loadImageFromLocal(String path, ImageView imageView) {
        ImageSizeUtil.ImageSize imageSize = ImageSizeUtil.getImageViewSize(imageView);
        Bitmap bm = decodeSampleBitmapFromPath(path, imageSize.width, imageSize.height);
        return bm;
    }

    private Bitmap decodeSampleBitmapFromPath(String path, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = ImageSizeUtil.caculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    private void refreashBitmap(final String path, final ImageView imageView, final Bitmap bm) {
        Message msg = Message.obtain();
        ImgBeanHolder holder = new ImgBeanHolder();
        holder.path = path;
        holder.imageView = imageView;
        holder.bitmap = bm;
        msg.obj = holder;
        mUIHandler.sendMessage(msg);
    }

    private class ImgBeanHolder {
        private String path;
        private ImageView imageView;
        private Bitmap bitmap;

    }
}
