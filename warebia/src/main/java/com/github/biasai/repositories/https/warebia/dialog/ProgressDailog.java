package com.github.biasai.repositories.https.warebia.dialog;

import android.app.Activity;
import android.view.View;

import com.github.biasai.repositories.https.warebia.R;
import com.github.biasai.repositories.https.warebia.base.BaseDialog;
import com.github.biasai.repositories.https.warebia.utils.ProportionUtils;
import com.github.biasai.repositories.https.warebia.view.ProgressView;


/**
 * 圆形进度条【需要手动关闭，返回键无法关闭】
 * Created by 彭治铭 on 2017/5/21.
 */

public class ProgressDailog extends BaseDialog {

    //在子线程中，显示出来
    public static void buider(final Activity activity) {
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getInstance(activity);
                }
            });
        }
    }

    //在子线程中关闭窗口
    public static void tear(final Activity activity) {
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isfinish(activity, progressDailog)) {
                        progressDailog.dismiss();
                    }
                }
            });
        }
    }

    public static boolean isAttach = true;
    public static ProgressDailog progressDailog;

    public static ProgressDailog getInstance(Activity activity) {
        //在主线程中实例化。防止子线程中调用错误。
        if (isfinish(activity, progressDailog)) {
            progressDailog = new ProgressDailog(activity);
        }
        return progressDailog;
    }

    public ProgressView progressView;

    public ProgressDailog(Activity activity) {
        if (activity != null) {
            init(activity, R.layout.biasai_dialog_progress);
        }
    }

    long start;

    @Override
    protected void initUI() {
        progressView = (ProgressView) findViewById(R.id.crown_progressView);
    }

    @Override
    protected void adapterUI() {
        ProportionUtils.getInstance().adapterScreen(activity, findViewById(R.id.crown_progress_dialog));
    }

    @Override
    protected void setWindowAnimations() {
        //super.setWindowAnimations();
        window.setWindowAnimations(R.style.biasai_window_alpha);
    }

    @Override
    protected boolean isAutoAdapterUI() {
        return false;
    }

    @Override
    protected boolean isStatus() {
        return true;
    }

    @Override
    protected boolean isTransparent() {
        return false;
    }

    @Override
    protected boolean isLocked() {
        return true;
    }

    @Override
    protected void listener() {
        if (activity != null) {
            isAttach = true;
            progressView.degress = 0;
            progressView.setVisibility(View.VISIBLE);
            start = System.currentTimeMillis();
        }
    }

    @Override
    protected void recycleView() {
        if (activity != null) {
            isAttach = false;
            progressView.setVisibility(View.INVISIBLE);
            //handler.removeMessages(0);
        }
    }

    public static void setIsAttach(boolean isAttach) {
        ProgressDailog.isAttach = isAttach;
    }

    public static void setProgressDailog(ProgressDailog progressDailog) {
        ProgressDailog.progressDailog = progressDailog;
    }

    public void setProgressView(ProgressView progressView) {
        this.progressView = progressView;
    }

    public void setStart(long start) {
        this.start = start;
    }
}
