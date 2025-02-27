package com.hss01248.imagelist;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.bumptech.glide.load.engine.GlideException;
import com.hss.downloader.IDownload;
import com.hss.downloader.IDownloadCallback;
import com.hss01248.image.ImageLoader;
import com.hss01248.image.interfaces.FileGetter;

import java.io.File;

import me.jessyan.progressmanager.ProgressListener;
import me.jessyan.progressmanager.ProgressManager;
import me.jessyan.progressmanager.body.ProgressInfo;

public class GlideDownloadImpl implements IDownload {
    @Override
    public void download(String url, String filePath, IDownloadCallback callback) {
        ImageLoader.getActualLoader().download(url, new FileGetter() {
            @Override
            public void onSuccess(File file, int width, int height) {
                ThreadUtils.executeByIo(new ThreadUtils.Task<File>() {
                    @Override
                    public File doInBackground() throws Throwable {
                        File file1 = new File(filePath);
                        FileUtils.copy(file, file1, new FileUtils.OnReplaceListener() {
                            @Override
                            public boolean onReplace(File srcFile, File destFile) {
                                return true;
                            }
                        });
                        return file1;
                    }

                    @Override
                    public void onSuccess(File result) {
                        callback.onSuccess(url);
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onFail(Throwable t) {

                    }
                });

            }

            @Override
            public void onFail(Throwable e) {
                if(e instanceof GlideException){
                    GlideException exception = (GlideException) e;
                    if(exception.getRootCauses() != null && exception.getRootCauses().size()>0){
                        e = exception.getRootCauses().get(0);
                    }
                }
                callback.onFail(url,e.getMessage(),e);

            }

            @Override
            public void onStart() {
                callback.onStart(url);
            }

            @Override
            public void onProgress(long currentOffset, long totalLength) {
               // callback.progress(url,currentOffset,totalLength);
            }
        });
        ProgressManager.getInstance().addResponseListener(url, new ProgressListener() {
            @Override
            public void onProgress(ProgressInfo progressInfo) {
                try {
                    callback.progress(url,progressInfo.getCurrentbytes(),progressInfo.getContentLength());
                    //tvProgress.setText(progressInfo.getPercent()+"% , speed: "+(progressInfo.getSpeed()/1024/8)+"KB/s");
                }catch (Throwable throwable){
                    throwable.printStackTrace();
                }
            }

            @Override
            public void onError(long id, Exception e) {
                if(e != null){
                    //e.printStackTrace();
                }
            }
        });
    }
}
