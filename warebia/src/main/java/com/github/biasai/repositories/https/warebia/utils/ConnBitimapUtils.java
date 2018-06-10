package com.github.biasai.repositories.https.warebia.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.github.biasai.repositories.https.warebia.base.BaseListenner;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by 彭治铭 on 2017/5/22.
 */

public class ConnBitimapUtils {
    private static ConnBitimapUtils connBitimap;
    private ThreadPoolExecutor threadPoolExecutor;
    private Map<String, Bitmap> maps;//缓存位图
    private Map<String, Boolean> saveMaps;//确认位图是否保存完成，防止保存和销毁的并发操作。true保存完成，false未保存完成

    private ConnBitimapUtils() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() + 2;
        int maxinumPoolSize = corePoolSize * 2 + 1;
        long keepAliveTime = 10;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingDeque<>();
        threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxinumPoolSize, keepAliveTime, unit, workQueue);
        //Log.e("test", "线程池核心数量:\t" + corePoolSize);
        maps = new HashMap<>();
        saveMaps = new HashMap<>();
    }

    public static ConnBitimapUtils getInstance() {
        if (connBitimap == null) {
            connBitimap = new ConnBitimapUtils();
        }
        return connBitimap;
    }

    /**
     * 获取网络位图。
     *
     * @param activity      可以为null,不为空，会自动跳转到主线程中回调。
     * @param uri           网络连接，使用Get请求。
     * @param params        uri里面的参数，可以为null
     * @param view          控件
     * @param isScale       是否将位图压缩到和控件同等大小。
     * @param isUI          是否跳回UI主线程。true 回调函数在UI线程，false回调函数在子线程[线程池里]。
     * @param baseListenner 回调函数，返回网络位图
     */
    public void getConnBitmap(final Activity activity, final String uri, final Map<String, String> params, final View view, final boolean isScale, final boolean isUI, final BaseListenner<Bitmap> baseListenner) {
        if (uri == null || uri.trim().equals("")) {
            return;
        }
        //网络请求，不能放在UI主线程中，必须在子线程中。
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bp = getConnBitmap(uri, params);
                if (isScale) {
                    bp = ViewBitmap(activity, view, bp);
                }
                if (baseListenner != null) {
                    final Bitmap bitmap = bp;
                    if (activity != null && !activity.isFinishing() && isUI) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                baseListenner.onResult(bitmap);
                            }
                        });
                    } else {
                        baseListenner.onResult(bitmap);
                    }
                }
            }
        });
    }

    //将bitmap压缩到和View同等大小，并且加载到View上。
    public Bitmap ViewBitmap(Activity activity, final View view, Bitmap src) {
        Bitmap bitmap = src;
        if (view != null && src != null) {
            int width = view.getLayoutParams().width;
            if (width < view.getWidth()) {
                width = view.getWidth();
            }
            int height = view.getLayoutParams().height;
            if (height < view.getHeight()) {
                height = view.getHeight();
            }
            if (width > 0 && height > 0) {
                float sp = (float) height / (float) width;
                bitmap = ProportionUtils.getInstance().GeometricCompressionBitmap(bitmap, width, sp);//对Bitmap根据控件大小，进行压缩。这个方法可以防止图片变形。
                final Bitmap bm = bitmap;
                if (activity != null && !activity.isFinishing()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //跳转UI主线程更新View
                            //view.setBackground(new BitmapDrawable(bm));
                            view.setBackgroundDrawable(new BitmapDrawable(bm));
                        }
                    });
                }
            }
        }
        return bitmap;
    }

    //获取网络图片【必须在子线程里，UI主线程不行】，以下方法，只有在非UI主线程才有效。注意是非
    //uri 可以是网络图片的直接地址，也可以不是。只要最后返回结果是图片即可。
    //params uri里面的参数，可以为null
    //uri传uri参数的时候，&会自动切断。所以&需要转换成 %26 。即可
    public Bitmap getConnBitmap(String uri, final Map<String, String> params) {
        if (uri == null || uri.trim().equals("")) {
            return null;
        }
        Bitmap bitmap = null;
        if (params != null) {
            StringBuffer sb = new StringBuffer();
            for (Map.Entry<String, String> e : params.entrySet()) {
                sb.append(e.getKey());
                sb.append("=");
                sb.append(e.getValue());
                sb.append("&");
            }
            uri = uri + "?" + sb.substring(0, sb.length() - 1);
        }
        //判断本地位图
        bitmap = CacheUtils.getInstance().getAsBitmap(getKey(uri));//此次对UtilCache进行优化，内部使用了UtilAssets。优化了位图。
        if (bitmap != null && !bitmap.isRecycled()) {
//            Log.e("test", "本地获取网络图片");
            return bitmap;
        }
        try {
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);//连接超时设置，绝对有效。一般50毫秒即可连接成功。
            conn.setRequestMethod("GET");
            // 设置通用的请求属性【必不可少，不然无法获取成功】
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            if (conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                byte[] b = InputStreamTOByte(inputStream);
                inputStream.close();
                inputStream = null;
                bitmap = BitmapFactory.decodeByteArray(b, 0, b.length, AssetsUtils.getInstance().getOptionsRGB_565());//===============================================================================最省内存法
                //本地图片缓存
                CacheUtils.getInstance().put(getKey(uri), bitmap);
            }
        } catch (Exception e) {
            Log.e("test", "网络图片获取失败:\t" + e.getMessage() + "\turl:\t" + uri);
        }
        return bitmap;
    }

    //设置网络位图【线程池中进行】
    //path为网络图片的直接地址
    public void setConnBitmap(final Activity activity, final String path, final View view) {
//        if (true) {
//            return;//占用内存2M
//        }
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    saveMaps.put(getSaveKey(view), false);
                    //判断缓存位图
                    if (maps != null && maps.containsKey(getKey(path))) {
                        Bitmap bitmap = maps.get(getKey(path));
                        if (bitmap != null && !bitmap.isRecycled()) {
                            updateView(activity, view, bitmap);
                            saveMaps.put(getSaveKey(view), true);
//                            Log.e("test", "临时缓存");
                            return;
                        } else {
                            maps.remove(getKey(path));
//                            Log.e("test", "已经释放");
                        }
                    }
//                    Log.e("test", "么有使用缓存");
//                    Log.e("test", "key键:\t" + getKey(path));
                    //判断本地位图
                    Bitmap bm = CacheUtils.getInstance().getAsBitmap(getKey(path));//此次对UtilCache进行优化，内部使用了UtilAssets。优化了位图。
                    if (bm != null && !bm.isRecycled()) {
                        updateView(activity, view, bm);
//                        Log.e("test", "本地\tkey:\t" + getKey(path) + "\t大小:\t" + bm.getByteCount() / 1024 + "KB");
                        if (maps != null) {
                            maps.put(getKey(path), bm);
                        }
                        saveMaps.put(getSaveKey(view), true);
                        return;
                    }
                    URL url = new URL(path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);//连接超时设置，绝对有效。一般50毫秒即可连接成功。
                    conn.setRequestMethod("GET");
//                    Log.e("test", "启动：\t" + conn.getResponseCode() + "\t");
                    if (conn.getResponseCode() == 200) {
                        InputStream inputStream = conn.getInputStream();
//                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, new Rect(0, 0, 0, 0), UtilAssets.getInstance(view.getContext()).getOptions());
                        byte[] b = InputStreamTOByte(inputStream);
                        inputStream.close();
                        inputStream = null;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length, AssetsUtils.getInstance().getOptionsRGB_565());//============================================================最省内存法
                        b = null;
                        //尽量使用 view.getLayoutParams().width，不要用view.getWidth()【靠不住，有时还会返回0】,但是view.getLayoutParams().width有时也会返回0.如约束布局不能设置match,只能设置0时。就会返回0
                        //Log.e("test", "宽度:\t" + view.getWidth() + "\t宽度：\t" + view.getLayoutParams().width);
                        int width = view.getLayoutParams().width;
                        if (width < view.getWidth()) {
                            width = view.getWidth();
                        }
                        int height = view.getLayoutParams().height;
                        if (height < view.getHeight()) {
                            height = view.getHeight();
                        }
                        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);//对Bitmap根据控件大小，进行压缩。
                        updateView(activity, view, bitmap);
                        //临时缓存
                        if (maps != null) {
                            maps.put(getKey(path), bitmap);
//                            Log.e("test", "已经缓存");
                        }
                        //缓存本地位图【再次使用的位图的复制，不是原位图。不要使用原位图，避开出错(同时操控一个位图会奔溃)。放心，复制的都是临时变量，存储完毕，内存会自动消下来。】
                        //UtilCache.getInstance(view.getContext()).put(getKey(path, view), bitmap.copy(Bitmap.Config.RGB_565, true),true);
                        CacheUtils.getInstance().put(getKey(path), bitmap);
                        //内存释放,不能释放，释放图片就显示不了了。
//                        if (!bitmap.isRecycled()) {
//                            bitmap.recycle();
//                            bitmap=null;
//                        }
                        saveMaps.put(getSaveKey(view), true);
//                        Log.e("test", "结束:\t" + getKey(path) + "\t大小：\t" + bitmap.getByteCount() / 1024 + "KB");
                        System.gc();
                    }
                } catch (Exception e) {
                    saveMaps.remove(getSaveKey(view));
                    Log.e("test", "网络图片请求异常:\t" + e.getMessage() + "\turl:\t" + path);
                }
            }
        });
    }

    final static int BUFFER_SIZE = 4096;

    //InputStream转byte字节，使用字节比使用流更省内存。当然测试发现只对网络输入流有效，一般的本地流就不用转了。转一下还浪费效率。
    public static byte[] InputStreamTOByte(InputStream in) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        try {
            while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
                outStream.write(data, 0, count);
        } catch (Exception e) {
            Log.e("test", "流转换字节出错:\t" + e.getMessage());
        }
        data = null;
        return outStream.toByteArray();
    }


    //更新UI
    public void updateView(Activity activity, final View view, final Bitmap bitmap) {
        //防止view.post不执行。【没有附加到当前窗口】。一般都是由actiivty更新UI
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    Log.e("test", "activity更新位图");
                    invalidate(view, bitmap);
                }
            });
        } else {
            //一般都会执行，有时可能不会执行。
            view.post(new Runnable() {
                @Override
                public void run() {
//                    Log.e("test", "view更新位图2:\t" + bitmap.getByteCount() / 1024 + "KB");
                    invalidate(view, bitmap);
                }
            });
        }
    }

    //更新View【更新位图之前不要主动释放原有的位图，防止出错。有新的位图时，旧的位图会自己消耗的，不用管。】
    private void invalidate(final View view, final Bitmap bitmap) {
        //view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        //Log.e("test", "执行了");
        if (view instanceof ImageView) {
//            recycleImageBitmap((ImageView) view);
            ((ImageView) view).setImageBitmap(bitmap);
        } else {
//            recycleBackground(view);
            //view.setBackground(new BitmapDrawable(bitmap));
            view.setBackgroundDrawable(new BitmapDrawable(bitmap));
        }
        System.gc();
    }

    //背景内存释放
    public void recycleBackground(final View view) {
        //Log.e("test","内存释放开始");
        Drawable drawable = view.getBackground();
        if (drawable != null && drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                //Log.e("test", "内存释放:\t" + bitmap.getByteCount() / 1024 / 1024);
                //view.setBackground(null);//置空，很重要
                view.setBackgroundDrawable(null);
                if (saveMaps != null && saveMaps.containsKey(getSaveKey(view)) && !saveMaps.get(getSaveKey(view))) {
                    final Bitmap bm = bitmap;
                    threadPoolExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                byte b = 0;//以防万一。无限循环。
                                while (saveMaps.containsKey(getSaveKey(view)) && !saveMaps.get(getSaveKey(view)) && b < 50) {
                                    b++;
                                    //没有保存完成
                                    Thread.sleep(500);
//                               Log.e("test", "销毁前循环判断");
                                }
//                                Log.e("test", "确认销毁2");
                                //保存完成之后再销毁
                                saveMaps.remove(getSaveKey(view));
                                bm.recycle();
                            } catch (Exception e) {
                                Log.e("test", "休眠出错:\t" + e.getMessage());
                            }
                            System.gc();
                        }
                    });
                } else {
//                    Log.e("test", "确认销毁");
                    //保存完成之后再销毁
                    saveMaps.remove(getSaveKey(view));
                    bitmap.recycle();
                    bitmap = null;
                }
            }
        }
        drawable = null;
        System.gc();
    }

    //回收ImageView里面的Src图片,最好调用一下。保持好习惯。
    public void recycleImageBitmap(final ImageView view) {
        Drawable drawable = view.getDrawable();
        if (drawable != null && drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null) {
                //Log.e("test", "内存释放2:\t" + bitmap.getByteCount() / 1024 / 1024);
                if (saveMaps != null && saveMaps.containsKey(getSaveKey(view)) && !saveMaps.get(getSaveKey(view))) {
                    final Bitmap bm = bitmap;
                    threadPoolExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                byte b = 0;//以防万一。无限循环。
                                while (saveMaps.containsKey(getSaveKey(view)) && !saveMaps.get(getSaveKey(view)) && b < 50) {
                                    b++;
                                    //没有保存完成
                                    Thread.sleep(500);
//                               Log.e("test", "销毁前循环判断");
                                }
                                //                            Log.e("test", "确认销毁2");
                                //保存完成之后再销毁
                                saveMaps.remove(getSaveKey(view));
                                bm.recycle();
                            } catch (Exception e) {
                                Log.e("test", "休眠出错:\t" + e.getMessage());
                            }
                            System.gc();
                        }
                    });
                } else {
//                    Log.e("test", "确认销毁");
                    //保存完成之后再销毁
                    saveMaps.remove(getSaveKey(view));
                    bitmap.recycle();
                    bitmap = null;
                }
                view.setImageBitmap(null);
            }
        }
        drawable = null;
        System.gc();
    }

    //缓存Bitmap的Key值,确保了绝对唯一。
    public String getKey(String path) {
        //view在加载图片和没加载图片时大小有可能不一样，不能确保唯一。
        //有时adapterView()之后大小也会发生改变。所以舍弃
//        int width = view.getLayoutParams().width;
//        if (width < view.getWidth()) {
//            width = view.getWidth();
//        }
//        int height = view.getLayoutParams().height;
//        if (height < view.getHeight()) {
//            height = view.getHeight();
//        }
//        return path + width + "" + height;
        return path;//就使用path【网络url】作为键值唯一标示即可。
    }

    //获取保存的map键值
    public String getSaveKey(View view) {
        return view.getId() + "" + view.hashCode();
    }
}
