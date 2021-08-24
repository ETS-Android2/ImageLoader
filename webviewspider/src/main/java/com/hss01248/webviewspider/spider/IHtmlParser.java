package com.hss01248.webviewspider.spider;

import android.content.Context;
import android.webkit.ValueCallback;

import com.hss01248.webviewspider.basewebview.WebPageInfo;

import java.util.ArrayList;
import java.util.List;

public interface IHtmlParser {


   default String resetDetailTitle(String title){
       return title;
   }

    String entranceUrl();


    List<String> parseTargetImagesInHtml(String html);

    String folderName();

   default String subfolderName(String title,String url){
        return "";
    }

   default boolean hiddenFolder(){
        return false;
    }



    default void parseListAndDetail(Context context, WebPageInfo listWebPageInfo, ValueCallback<ListToDetailImgsInfo> infoCallback, ValueCallback<String> progressCallback){
        infoCallback.onReceiveValue(new ListToDetailImgsInfo());
    }


    boolean interceptImage(String url);
}
