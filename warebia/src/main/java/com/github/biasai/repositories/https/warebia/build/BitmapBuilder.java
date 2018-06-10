package com.github.biasai.repositories.https.warebia.build;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;

import com.github.biasai.repositories.https.warebia.base.BaseListenner;
import com.github.biasai.repositories.https.warebia.utils.ConnBitimapUtils;

import java.util.Map;


/**
 * Created by 彭治铭 on 2018/1/12.
 */

public class BitmapBuilder {
    private static BitmapBuilder bitmapBuilder;

    //url是基础，buider的时候，必须传人。
    //url和回调函数没有set方法。
    public static BitmapBuilder Builder(String url) {
        if (bitmapBuilder == null) {
            bitmapBuilder = new BitmapBuilder();
        }
        bitmapBuilder.url = url;
        bitmapBuilder.params = null;
        bitmapBuilder.isUi = true;
        bitmapBuilder.isScale = true;
        return bitmapBuilder;
    }

    private BitmapBuilder() {
    }


    /**
     * 开始执行
     * 调用方式： BitmapBuilder.Builder().setUrl(url).setView(view).execute(context,null,null);
     * @param activity 上下文,可以为null,不为空，会自动跳转到主线程中回调。
     * @param callBack 回调函数【返回网络位图】可以为空。一般有View时，回调函数基本意义不大，可以为空。
     * @param view 如果View不为空，则默认将Bitmap加载到View上面。isScale为true时，才会压缩并加载到View上。
     */
    public void execute(Activity activity,View view,BaseListenner<Bitmap> callBack) {
        ConnBitimapUtils.getInstance().getConnBitmap(activity,url, params, view, isScale, isUi, callBack);
    }

    private String url;//网络请求链接
    private Map<String, String> params;//params请求参数，其实在body里面，属性body子集
    private boolean isUi = true;//回调返回，是否在UI主线程。 true在主线程，false在子线程【线程池里】
    private boolean isScale = true;//是否将Bitmap压缩到和控件大小一样。默认都是true。前提View不能为空。

    public BitmapBuilder setParams(Map<String, String> params) {
        this.params = params;
        return this;
    }

    public BitmapBuilder isUi(boolean isUi) {
        this.isUi = isUi;
        return this;
    }

    public BitmapBuilder isScale(boolean isScale) {
        this.isScale = isScale;
        return this;
    }
}
