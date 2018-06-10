package com.github.biasai.repositories.https.warebia.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.github.biasai.repositories.https.warebia.R;
import com.github.biasai.repositories.https.warebia.utils.ProportionUtils;

import java.lang.reflect.Field;


/**
 * 自定义弹窗【支持软键盘,可以对文本框进行输入】
 * 子类构造方法中一定要调用init()方法即可。其他扩展内容随意添加。
 * 子类初始，可以调用isFinishing(BaseDialog baseDialog),判断是否需要初始化
 *
 * @author 彭治铭
 */
public abstract class BaseDialog {
    public Activity activity;
    protected Dialog dialog;
    protected Window window;

    public Activity getActivity() {
        return activity;
    }

    /**
     * 子类在构造函数中，实现这个方法即可。
     * 弹窗可以多重显示，后面的弹窗会遮挡前面的弹窗。
     *
     * @param activity 必不可少。必须是当前Activity的上下文,BaseApplication.getInstance()会报错。
     * @param LayoutId 布局文件
     * @param
     */
    @SuppressLint("NewApi")
    protected void init(final Activity activity, final int LayoutId) {
        this.activity = activity;
        addressActivy = activity.toString();
        //==========================================================================================开始

        //if (Build.VERSION.SDK_INT < 19) {//4.4以下全部全屏。因为控制不了状态栏的颜色。所以就不要状态栏。直接全屏}
        //如果Activity是全屏,弹窗也会是全屏。主题文件也没办法。
        int styleTheme;//布局居中

        //与Activity是否有状态栏无关。Dialog可以自己控制自己是否显示状态栏。即使Acitivy全屏，Dilog也可以有自己的状态栏。亲测
        if (isStatus() && (Build.VERSION.SDK_INT >= 19)) {//是否有状态栏
            if (isTransparent()) {//背景是否透明
                styleTheme = R.style.biasaiBaseDialogTransparent;
            } else {
                styleTheme = R.style.biasaiBaseDialog;
            }
        } else {
            if (isTransparent()) {
                styleTheme = R.style.biasaiBaseDialogFullTransparent;
            } else {
                styleTheme = R.style.biasaiBaseDialogFull;
            }
        }
        dialog = new Dialog(activity, styleTheme) {
            @Override
            public void onAttachedToWindow() {
                super.onAttachedToWindow();
                //按键屏蔽，每次都重新设置
                BaseDialog.this.setOnKeyListener(isLocked());
                //触摸最外层控件，是否消失
                if (isDismiss()) {
                    getParentView().setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                                dismiss();
                            }
                            return true;
                        }
                    });
                } else {
                    getParentView().setOnTouchListener(null);
                }
                //附加到窗口,每次显示的时候都会调用
                listener();
            }

            @Override
            public void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                //从当前窗口移除。每次dismiss的时候都会调用
                recycleView();
                if (isRecycel()) {
                    recycles();
                }
            }
        };

        if (Build.VERSION.SDK_INT < 19) {
            //低版本显示窗体，必须在window.setContentView之前调用一次。其后就可随便调show()了。
            //高版本可在window.setContentView()之后调用。
            if (dialog != null && activity != null && !activity.isFinishing()) {
                if (isAutoShow()) {
                    dialog.show();
                }
            }
        }
        //true(按其他区域会消失),按返回键还起作用。false(按对话框以外的地方不起作用,即不会消失)【现在的布局文件，基本都是全屏的。这个属性设置已经没有意义了。触屏消失，需要自己手动去实现。】
        //都以左上角为标准对齐。没有外区，全部都是Dialog区域。已经确保百分百全屏。所以这个方法已经没有意义
        //dialog.setCanceledOnTouchOutside(true);// 调用这个方法时，按对话框以外的地方不起作用。按返回键还起作用
        window = dialog.getWindow();
        if (Build.VERSION.SDK_INT <= 17) {
            //解决乐视黑屏问题
            window.setFormat(PixelFormat.RGBA_8888);
            window.setBackgroundDrawable(null);
        }
        //window.setContentView之前设置
        //window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏,无效。全屏必须到style主题文件里设置才有效
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//状态栏透明,4.4及以上有效
        window.getDecorView().setFitsSystemWindows(false);
        window.setContentView(LayoutId);// 弹出框的布局
        if (Build.VERSION.SDK_INT >= 19) {
            if (dialog != null && activity != null && !activity.isFinishing()) {
                if (isAutoShow()) {
                    dialog.show();//防止状态栏的设置无效。一定在设置之后再显示出来。
                }
            }
        }
        //为Window设置动画【Dialog和PopuWindow动画都是Style文件】
        //window.setWindowAnimations(R.style.CustomDialog);
        //popupWindow.setAnimationStyle(R.style.CustomDialog)

        //如果是windowIsFloating为false。则以左上角为标准。居中无效。并且触摸外区也不会消失。因为没有外区。整个屏幕都是Dialog区域。
        window.setGravity(Gravity.CENTER);//居中。

        //设置状态栏背景透明【亲测有效】
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);   //去除半透明状态栏
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);  //一般配合fitsSystemWindows()使用, 或者在根部局加上属性android:fitsSystemWindows="true", 使根部局全屏显示
            if (Build.VERSION.SDK_INT >= 21) {
                window.setStatusBarColor(Color.TRANSPARENT);
            }
            ViewPager.DecorView decordView = (ViewPager.DecorView) window.getDecorView();     //获取DecorView实例
            Field field = ViewPager.DecorView.class.getDeclaredField("mSemiTransparentStatusBarColor");  //获取特定的成员变量
            field.setAccessible(true);   //设置对此属性的可访问性
            field.setInt(decordView, Color.TRANSPARENT);  //修改属性值
        } catch (Exception e) {
            //Log.e("test", "状态栏异常:\t" + e.getMessage());
        }

        initUI();
        adapterUI();
        if (isAutoAdapterUI()) {
            try {
                //自动适配所有控件大小。
                ProportionUtils.getInstance().adapterAllView(activity, getParentView(), true, true);
            } catch (Exception e) {
                Log.e("test", "自动适配所有弹窗控件异常:\t" + e.getMessage());
            }
        }
        setWindowAnimations();
        //设置屏幕的亮度【统一整个App应用的的亮度】
        //BaseActivity.setScreenBrightness(activity, BaseActivity.getScreenBrightness(activity));//默认亮度就不用设置了。
        //==========================================================================================结束
    }

    //获取xml文件最外层控件。
    public ViewGroup getParentView() {
        View decorView = window.getDecorView();//布局里面的最顶层控件，本质上是FrameLayout(帧布局)，FrameLayout.LayoutParams
        ViewGroup contentView = (ViewGroup) decorView.findViewById(android.R.id.content);//我们的布局文件。就放在contentView里面。contentView本质上也是FrameLayout(帧布局)，FrameLayout.LayoutParams
        View parent = contentView.getChildAt(0);//这就是我们xml布局文件最外层的那个父容器控件。
        return (ViewGroup) parent;
    }

    //初始化UI【初始化时会自动调用，子类不需要调用，只需要实现该方法即可】
    protected abstract void initUI();

    //事件，弹窗每次显示时，都会调用
    protected abstract void listener();

    //事件，弹窗每次消失时，都会调用
    protected abstract void recycleView();

    //是否清除所有属性，true会执行recycles()方法【单列模式也就没有任何意思】，false则不会。
    protected boolean isRecycel() {
        return true;//默认会清除,防止内存泄漏，建议true
    }

    //完全清空，所有属性都清空。防止内存泄漏
    //子类需要实现所有属性的set()方法才有效。
    protected void recycles() {
        activity = null;
        window = null;
        dialog = null;
    }

    //给Dialog添加默认动画。子类可以重写。
    protected void setWindowAnimations() {
        window.setWindowAnimations(R.style.biasai_window_right);//从右边进，从右边出
    }

    //屏幕适配【如果自动适配了，这个方法也就没有多大意义了,不过adapterUI()和isAutoAdapterUI()两个方法都会执行。】
    protected void adapterUI() {
    }

    //是否自动适配所有控件大小，默认是为fasle
    protected boolean isAutoAdapterUI() {
        return false;
    }

    protected boolean status = true;

    //是否显示状态栏【true会显示状态栏，false不会显示状态栏】
    //与Activity是否有状态栏无关。Dialog可以自己控制自己是否显示状态栏。即使Acitivy全屏，Dilog也可以有自己的状态栏。亲测
    protected boolean isStatus() {
        return status;
    }


    //背景是否透明,true透明，false背景会有遮罩层半透明的效果。
    protected boolean isTransparent() {
        return false;
    }

    //触摸最外层控件，弹窗是否消失。默认不消失。兼容之前不消失的。
    protected boolean isDismiss() {
        return false;
    }

    //初始化getInstance()的時候，是否自动显示出來。默认显示。
    protected boolean isAutoShow() {
        return true;
    }

    //true屏蔽返回键，false不屏蔽返回键。默认生成的方法，返回的都是false。即默认就是false，不屏蔽
    protected boolean isLocked() {
        return false;
    }

    //获取控件
    public View findViewById(int id) {
        return window.findViewById(id);
    }

    //关闭弹窗【在Activity关闭之前，一定要先关闭弹窗。不然对activity的引用，activity会内存泄漏/最好关闭activity之前，手动调用一次关闭弹窗。】
    public void dismiss() {
        if (dialog != null && activity != null && !activity.isFinishing()) {
            dialog.dismiss();
        }
        System.gc();//垃圾内存回收
    }

    //显示窗体,正确显示窗体。返回true
    public Boolean show() {
        if (activity != null && !activity.isFinishing()) {
            //显示窗体，必须在window.setContentView之前调用一次。其后就可随便调show()了。
            dialog.show();
        }
        return true;
    }

    private String addressActivy;//记录当前Activity的地址

    public static boolean isfinish(Activity activity, BaseDialog baseDialog) {
        if (isFinishing(activity, baseDialog)) {
            return true;
        }
        baseDialog.addressActivy = activity.toString();
        return false;
    }

    //判断当前弹窗是否已经消亡【多个Activity就使用这个进行判断】
    private static Boolean isFinishing(Activity activity, final BaseDialog baseDialog) {
        if (baseDialog != null && baseDialog.dialog != null) {
            baseDialog.activity = activity;
            if (baseDialog.addressActivy == null || !activity.toString().equals(baseDialog.addressActivy)) {//如果Activity已经改变，弹窗也必须重新实例化才行。不然可能无显示。
                return true;//该弹窗已经销毁，需要重新实例化。
            } else {
                return isFinishing(baseDialog);
            }
        } else {
            return true;
        }
    }

    /**
     * 判断当前弹窗是否已经存在。如果存在，直接显示出来。子类不需要再调用show()。
     * 判断 当前窗口是否销毁【子类重新初始前，可以判断当前窗口是否已经销毁，如果销毁，则重新创建】
     *
     * @param baseDialog 当前窗口
     * @return true【已经销毁，需要重新初始】,false【没有销毁，可以继续显示。会自动显示出来】
     */
    private static Boolean isFinishing(final BaseDialog baseDialog) {
        if (!baseDialog.activity.isFinishing()) {
            if (baseDialog.isAutoShow()) {
                baseDialog.show();//显示出来的。
            }
            return false;
        }
        return true;
    }

    //捕捉屏蔽返回键
    private void setOnKeyListener(boolean locked) {
        if (locked) {
            //屏蔽
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK
                            && event.getAction() == KeyEvent.ACTION_DOWN) {
                        return true;//记得返回true。表示已经处理。
                    }
                    return false;//返回键以外交给系统自行处理。不可以屏蔽，不然输入法键盘的按键可能无效。如删除键
                }
            });
        } else {
            //不屏蔽
            dialog.setOnKeyListener(null);
        }
    }
}
