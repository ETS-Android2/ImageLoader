package com.hss01248.webviewspider.basewebview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;

import com.blankj.utilcode.util.ToastUtils;
import com.hss01248.pagestate.PageStateConfig;
import com.hss01248.pagestate.PageStateManager;
import com.hss01248.webviewspider.R;
import com.hss01248.webviewspider.databinding.TitlebarForWebviewBinding;
import com.just.agentweb.AgentWeb;
import com.just.agentweb.AgentWebUIControllerImplBase;
import com.just.agentweb.WebViewClient;

import org.apache.commons.lang3.StringEscapeUtils;



public class BaseQuickWebview extends LinearLayout implements DefaultLifecycleObserver {



    String currentUrl = "";

    public AgentWeb getAgentWeb() {
        return mAgentWeb;
    }

    AgentWeb mAgentWeb;
    long delayAfterOnFinish;
    AgentWeb.PreAgentWeb preAgentWeb;

    public WebView getWebView() {
        return webView;
    }

    WebView webView;

    public String getCurrentTitle() {
        return currentTitle;
    }

    String currentTitle;

    public String getCurrentUrl() {
        return currentUrl;
    }

    WebDebugger debugger;
    String source;

    public WebPageInfo getInfo() {
        return info;
    }

    WebPageInfo info;

    public BaseQuickWebview(Context context) {
        super(context);
        init(context);
    }

    public BaseQuickWebview(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BaseQuickWebview(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BaseQuickWebview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        info = new WebPageInfo();
        initTitlebar(context);

        Activity activity = WebDebugger.getActivityFromContext(context);
        if(activity instanceof LifecycleOwner){
            LifecycleOwner owner = (LifecycleOwner) activity;
            addLifecycle(owner);
        }
        initWebView();
    }

    TitlebarForWebviewBinding titleBar;
    private void initTitlebar(Context context) {

        /*<com.hjq.bar.TitleBar
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="20dp"
                app:barStyle="night"
                app:leftTitle="返回"
                app:rightTitle="设置"
                app:title="夜间模式的标题栏" />*/
        Activity activity = WebDebugger.getActivityFromContext(context);
        titleBar = TitlebarForWebviewBinding.inflate(activity.getLayoutInflater(),this,false);

        addView(titleBar.getRoot());
        titleBar.ivBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean b = onBackPressed();
                if(!b){
                    ActivityUtils.getTopActivity().finish();
                }
            }
        });
        titleBar.ivClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityUtils.getTopActivity().finish();
            }
        });
        titleBar.ivMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });

    }

    protected void showMenu() {
        ToastUtils.showLong("show menu");

    }

    public void getSource(ValueCallback<String> valueCallback){
        if(!TextUtils.isEmpty(source)){
            valueCallback.onReceiveValue(source);
            return;
        }
        loadSource(valueCallback);
    }

    public static BaseQuickWebview loadHtml(Context context,String url,long delayAfterOnFinish,ValueCallback<WebPageInfo> sourceLoadListener){
        BaseQuickWebview quickWebview = new BaseQuickWebview(context);
        quickWebview.needBlockImageLoad = true;
        quickWebview.delayAfterOnFinish = delayAfterOnFinish;
/*
        Dialog dialog = new Dialog(context);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(quickWebview);
        dialog.show();
        WindowManager.LayoutParams attributes = dialog.getWindow().getAttributes();
        attributes.height = ScreenUtils.getAppScreenHeight()/2;
        attributes.gravity = Gravity.BOTTOM;
        dialog.getWindow().setAttributes(attributes);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });*/



        quickWebview.setSourceLoadListener(new ValueCallback<WebPageInfo>() {
            @Override
            public void onReceiveValue(WebPageInfo value) {
                sourceLoadListener.onReceiveValue(value);
                if(quickWebview.getAgentWeb() != null){
                    quickWebview.getAgentWeb().destroy();
                }
            }
        });
        quickWebview.loadUrl(url);
        return quickWebview;

    }
    ValueCallback<WebPageInfo> sourceLoadListener;


    public void setNeedBlockImageLoad(boolean needBlockImageLoad) {
        this.needBlockImageLoad = needBlockImageLoad;
    }

    boolean needBlockImageLoad;
    public void setSourceLoadListener(ValueCallback<WebPageInfo> sourceLoadListener){
       this.sourceLoadListener = sourceLoadListener;
    }


    public void loadSource(ValueCallback<String> valueCallback){
        if(webView == null){
            Log.w("loadSource","webview is null");
            return;
        }
//        if(TextUtils.isEmpty(source)){
//            valueCallback.onReceiveValue(source);
//            return;
//        }
        //String script = "javascript:document.getElementsByTagName('html')[0].innerHTML";
        String script = "javascript:document.getElementsByTagName('body')[0].innerHTML";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //在主线程执行,耗时好几s
            webView.evaluateJavascript(script, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    source = StringEscapeUtils.unescapeJava(value);
                    if(source.startsWith("\"")){
                        source = source.substring(1);
                    }
                    if(source.endsWith("\"")){
                        source = source.substring(0,source.length()-1);
                    }
                    //source = "<html>"+source +"</html>";
                    source = "<body>"+source +"</body>";
                    LogUtils.v(source);
                    info.htmlSource = source;
                    valueCallback.onReceiveValue(source);
                }
            });
        }
    }

    boolean hasAdd;
    private void addLifecycle(LifecycleOwner lifecycleOwner){
        if(hasAdd){
            return;
        }
        lifecycleOwner.getLifecycle().addObserver(this);
        hasAdd = true;
    }

    public void loadUrl(String url){
        go(url);
    }



    PageStateManager stateManager;

    JsCreateNewWinImpl jsCreateNewWin = new JsCreateNewWinImpl();

    private void initWebView() {
        preAgentWeb = AgentWeb.with((Activity) getContext())//传入Activity or Fragment
                .setAgentWebParent(this,
                        new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                //传入AgentWeb 的父控件 ，如果父控件为 RelativeLayout ， 那么第二参数需要传入 RelativeLayout.LayoutParams ,第一个参数和第二个参数应该对应。
                .useDefaultIndicator()// 使用默认进度条
                .setAgentWebUIController(new AgentWebUIControllerImplBase(){
                    @Override
                    public void onMainFrameError(WebView view, int errorCode, String description, String failingUrl) {
                        super.onMainFrameError(view, errorCode, description, failingUrl);
                        if(stateManager != null){
                            stateManager.showError(errorCode+"\n"+description+"\n on url:"+failingUrl);
                        }
                    }

                    @Override
                    public void onShowMainFrame() {
                        super.onShowMainFrame();
                        if(stateManager != null){
                            stateManager.showContent();
                        }

                    }
                })
                .setWebViewClient(new WebViewClient(){
                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                        source = "";
                        currentUrl = url;
                        currentTitle = "";
                        info.htmlSource = "";
                        info.url = url;
                        info.title = "";
                        if(needBlockImageLoad){
                            view.getSettings().setBlockNetworkImage(needBlockImageLoad);
                        }
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loadSource(new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        if(sourceLoadListener != null){
                                            sourceLoadListener.onReceiveValue(info);
                                        }
                                    }
                                });
                            }
                        },delayAfterOnFinish);

                    }
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                        if(stateManager != null){
                            stateManager.showError(errorCode+"\n"+description+"\n on url:"+failingUrl);
                        }

                    }

                    @Override
                    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                        super.onReceivedHttpError(view, request, errorResponse);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if(request.isForMainFrame()){
                                if(stateManager != null){
                                    stateManager.showError(errorResponse.getStatusCode()+"\n"+errorResponse.getReasonPhrase()+"\n on url:"+request.getUrl());
                                }

                            }
                        }
                    }

                    @Override
                    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                        super.onReceivedSslError(view, handler, error);
                        if(stateManager != null){
                            stateManager.showError("SslError:\n"+error.toString());
                        }

                    }
                })
                .setWebChromeClient(new com.just.agentweb.WebChromeClient(){

                    @Override
                    public void onReceivedTitle(WebView view, String title) {
                        super.onReceivedTitle(view, title);
                        titleBar.tvTitle.setText(title);
                        currentTitle = title;
                        info.title = title;
                    }

                   /* @Override
                    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                        return jsCreateNewWin.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
                    }

                    @Override
                    public void onCloseWindow(WebView window) {
                        jsCreateNewWin.onCloseWindow(window);
                    }*/
                })
              // .setMainFrameErrorView(R.layout.pager_error,R.id.error_btn_retry)
                //.setMainFrameErrorView(errorLayout)
                .createAgentWeb()//
                .ready();

        mAgentWeb = preAgentWeb.get();

        webView = mAgentWeb.getWebCreator().getWebView();
        stateManager = PageStateManager.initWhenUse(mAgentWeb.getWebCreator().getWebParentLayout(), new PageStateConfig() {

            @Override
            public boolean isFirstStateLoading() {
                return false;
            }

            @Override
            public void onRetry(View retryView) {
                stateManager.showContent();
                webView.reload();

            }
        });
        WebConfigger.config(webView);
        debugger =  new WebDebugger();
        debugger.setWebviewDebug(webView);

    }



    private void go(String url){
        info.url = url;
        WebConfigger.syncCookie(webView,url);
        if(mAgentWeb == null){
            LogUtils.w("mAgentWeb == null");
            //mAgentWeb = preAgentWeb.go(url);
        }else {
            mAgentWeb.getUrlLoader().loadUrl(url);
        }


    }






    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if(mAgentWeb != null){
            mAgentWeb.getWebLifeCycle().onResume();
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if(mAgentWeb != null){
            mAgentWeb.getWebLifeCycle().onPause();
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if(mAgentWeb != null){
            mAgentWeb.getWebLifeCycle().onDestroy();
        }
    }

    public boolean onBackPressed(){
        if(mAgentWeb == null){
            return false;
        }
        return mAgentWeb.back();
    }


}
