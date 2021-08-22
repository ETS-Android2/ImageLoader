package com.hss01248.bigimageviewpager;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.disklrucache.DiskLruCache;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.SafeKeyGenerator;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.EmptySignature;
import com.bumptech.glide.util.LruCache;
import com.bumptech.glide.util.Util;
import com.shizhefei.view.largeimage.factory.InputStreamBitmapDecoderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.jessyan.progressmanager.ProgressListener;
import me.jessyan.progressmanager.ProgressManager;
import me.jessyan.progressmanager.body.ProgressInfo;

public class UrlLoader {

    public interface LoadListener{
        void onLoad(String path);

        void onProgress(int progress);

        void onFail(Throwable throwable);
    }

    static Handler handler = new Handler(Looper.getMainLooper());


    public static void download(Context context, ImageView ivHelper,String url,LoadListener listener){


        ProgressManager.getInstance().addResponseListener(url, new ProgressListener() {
            @Override
            public void onProgress(ProgressInfo progressInfo) {
                try {
                    listener.onProgress(progressInfo.getPercent());
                    //tvProgress.setText(progressInfo.getPercent()+"% , speed: "+(progressInfo.getSpeed()/1024/8)+"KB/s");
                }catch (Throwable throwable){
                    throwable.printStackTrace();
                }
            }

            @Override
            public void onError(long id, Exception e) {
                if(e != null){
                    e.printStackTrace();
                }
            }
        });
        loadGlideByView(context,url,listener,ivHelper);














       /* service.execute(new Runnable() {
            @Override
            public void run() {
                boolean cached = isCached(context, url);
                if(cached){
                    getFromCache(context,url,null,listener);
                }else {
                    ProgressManager.getInstance().addResponseListener(url, new ProgressListener() {
                        @Override
                        public void onProgress(ProgressInfo progressInfo) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        listener.onProgress(progressInfo.getPercent());
                                        //tvProgress.setText(progressInfo.getPercent()+"% , speed: "+(progressInfo.getSpeed()/1024/8)+"KB/s");
                                    }catch (Throwable throwable){
                                        throwable.printStackTrace();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(long id, Exception e) {
                            if(e != null){
                                e.printStackTrace();
                            }
                        }
                    });
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            loadGlideByView(context,url,listener,ivHelper);
                        }
                    });
                }
            }
        });*/
    }

    /**
     * 使用view来加载,提高优先级.否则会超级慢
     * @param context
     * @param url
     * @param listener
     * @param ivHelper
     */
    private static void loadGlideByView(Context context, String url, LoadListener listener, ImageView ivHelper) {

        getFromCache(context,url,null,listener);
        /*Glide.with(context)
                .load(url)
                .priority(Priority.IMMEDIATE)
                .listener(new RequestListener< Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        android.util.Log.d("GLIDE", String.format(Locale.ROOT,
                                "onException(%s, %s, %s, %s)", e, model, target, isFirstResource), e);
                        getFromCache(context,url,e,listener);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        android.util.Log.v("GLIDE", String.format(Locale.ROOT,
                                "onResourceReady(%s, %s, %s, %s)", resource, model, target, isFirstResource));
                        getFromCache(context,url,null,listener);
                        return false;
                    }
                })
                .into(ivHelper);*/
    }

    private static void getFromCache(Context context, String url,Throwable throwable, LoadListener listener) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Glide.with(context)
                        .download(url)
                        .priority(Priority.HIGH)
                        .addListener(new RequestListener<File>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                                if (e != null) {
                                    listener.onFail(e);
                                } else {
                                    listener.onFail(new Throwable("get cache file from glide failed"));
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                                Log.w("isCached", "cache by glide:" + url + "\nfile:" + resource);
                                listener.onLoad(resource.getAbsolutePath());
                                isCached(context, url);
                                return false;
                            }
                        }).into(new SimpleTarget<File>() {
                    @Override
                    public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {

                    }
                });
            }
        });

    }

    public static boolean isCached(Context context,String url) {
        OriginalKey originalKey = new OriginalKey(url, EmptySignature.obtain());
        SafeKeyGenerator safeKeyGenerator = new SafeKeyGenerator();
        String safeKey = safeKeyGenerator.getSafeKey(originalKey)+".0";
        try {

            DiskCache diskCache = DiskLruCacheWrapper.get(Glide.getPhotoCacheDir(context), DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE);
            //Glide.get(context).
           /* DiskLruCache diskLruCache = DiskLruCache.open(new File(context.getCacheDir(),
                    DiskCache.Factory.DEFAULT_DISK_CACHE_DIR), 1, 1, DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE);*/
           // DiskLruCache.Value value = diskCache.get(originalKey);
            Log.w("isCached","key:"+safeKey+",DiskLruCache.Value:"+diskCache.get(originalKey));
            Log.w("isCached","file :"+new File(context.getCacheDir(),"image_manager_disk_cache/"+safeKey).exists());
            //if (value != null && value.getFile(0).exists() && value.getFile(0).length() > 30) {
                return true;
           // }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;

    }

    private static class OriginalKey implements Key {

        private final String id;
        private final Key signature;

        public OriginalKey(String id, Key signature) {
            this.id = id;
            this.signature = signature;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            OriginalKey that = (OriginalKey) o;

            if (!id.equals(that.id)) {
                return false;
            }
            if (!signature.equals(that.signature)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + signature.hashCode();
            return result;
        }

        @Override
        public void updateDiskCacheKey(MessageDigest messageDigest) {
            try {
                messageDigest.update(id.getBytes(STRING_CHARSET_NAME));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            signature.updateDiskCacheKey(messageDigest);
        }
    }

    private static class SafeKeyGenerator {
        private final LruCache<Key, String> loadIdToSafeHash = new LruCache<Key, String>(1000);

        public String getSafeKey(Key key) {
            String safeKey;
            synchronized (loadIdToSafeHash) {
                safeKey = loadIdToSafeHash.get(key);
            }
            if (safeKey == null) {
                try {
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                    key.updateDiskCacheKey(messageDigest);
                    safeKey = Util.sha256BytesToHex(messageDigest.digest());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                synchronized (loadIdToSafeHash) {
                    loadIdToSafeHash.put(key, safeKey);
                }
            }
            return safeKey;
        }
    }
}
