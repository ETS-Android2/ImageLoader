package com.hss01248.glidev4.config;

import android.content.Context;

import androidx.annotation.NonNull;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.LibraryGlideModule;
import com.hss01248.image.config.GlobalConfig;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jp.co.link_u.library.glideavif.AvifDecoderFromByteBuffer;
import me.jessyan.progressmanager.ProgressManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by Administrator on 2017/5/2 0002.
 */
@GlideModule
public class GlideModelConfig extends LibraryGlideModule {



    /* @Override
     public void applyOptions(Context context, GlideBuilder builder) {
         builder.setLogLevel(Log.DEBUG);
         if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ) {
             builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
         }else {
             builder.setDecodeFormat(DecodeFormat.PREFER_RGB_565);
         }//解决rgb565部分手机上出现绿色问题

         final UncaughtThrowableStrategy myUncaughtThrowableStrategy = new ...
    builder.setDiskCacheExecutor(newDiskCacheExecutor(myUncaughtThrowableStrategy));
    builder.setResizeExecutor(newSourceExecutor(myUncaughtThrowableStrategy));
    }*/
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide,
                                   @NonNull Registry registry) {
        /**
         * 不带拦截功能，只是单纯替换通讯组件
         */
        LogUtils.w("in lib: registerComponents");
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
                //.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        if(GlobalConfig.debug){
            setIgnoreAll(builder);
        }

        builder
                //.addNetworkInterceptor(new ProgressInterceptor())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(ProgressManager.getInstance()
                .with(builder).build()));
        registry.prepend(ByteBuffer.class, Bitmap.class,new AvifDecoderFromByteBuffer());
        Log.i("glide", "registerComponents---");

    }


    private static void setIgnoreAll(OkHttpClient.Builder builder) {
        X509TrustManager xtm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] x509Certificates = new X509Certificate[]{};
                return x509Certificates;
                // return null;
            }
        };

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");

            sslContext.init(null, new TrustManager[]{xtm}, new SecureRandom());

            HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            builder.sslSocketFactory(sslContext.getSocketFactory())
                    .hostnameVerifier(DO_NOT_VERIFY);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }
}
