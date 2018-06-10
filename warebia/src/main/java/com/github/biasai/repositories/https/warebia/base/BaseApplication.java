package com.github.biasai.repositories.https.warebia.base;

import android.app.Application;
import android.content.Context;
import java.lang.reflect.Method;


/**
 * Created by  彭治铭 on 2017/9/10.
 */
//必须在AndroidManifest.xml中application指明
//<application android:name=".base.BaseApplication">
//配置文件声明之后，才会调用onCreate()等什么周期。
public class BaseApplication extends Application {

    private static BaseApplication sInstance;

    //通过反射获取ActivityThread【隐藏类】
    private static Object getActivityThread() {
        try {
            final Class<?> clz = Class.forName("android.app.ActivityThread");
            final Method method = clz.getDeclaredMethod("currentActivityThread");
            method.setAccessible(true);
            final Object activityThread = method.invoke(null);
            return activityThread;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //初始化
    public static BaseApplication getInstance() {
        if (sInstance == null) {
            //如果配置文件没有声明，也没有手动初始化。则通过反射自动初始化。【反射是最后的手段，效率不高】
            //通过反射，手动获取上下文。
            final Object activityThread = getActivityThread();
            if (null != activityThread) {
                try {
                    final Method getApplication = activityThread.getClass().getDeclaredMethod("getApplication");
                    getApplication.setAccessible(true);
                    Context applicationContext = (Context) getApplication.invoke(activityThread);
                    setsInstance(applicationContext);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return sInstance;
    }

    //如果没有在配置文件中配置，则需要手动调用以下方法，手动初始化BaseApplication
    //不会调用onCreate()等什么周期
    //BaseApplication.setsInstance(getApplication());
    public static void setsInstance(Context application) {
        if (sInstance == null) {
            sInstance = new BaseApplication();
            //统一上下文
            sInstance.attachBaseContext(application);
        }
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        sInstance = this;
    }

}
