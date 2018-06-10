package com.github.biasai.repositories.https.warebia.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 文件下载工具类，支持断点下载。
 * 上传就不需要单独写工具类了【所谓的上传就是将文件(File file)作为参数通过Http协议Post提交即可】,如：params.addBodyParameter(key, file);
 * Created by 彭治铭 on 2017/5/24.
 */

public class FileLoadUtils {
    private Context context;
    private static FileLoadUtils fileDown;
    private ThreadPoolExecutor threadPoolExecutor;
    //文件下载目录
    public String cacheDir;
    //判断该uri是否正在下载
    private Map<String, Boolean> mapLoad;

    //构造函数
    private FileLoadUtils(Context context) {
        this.context = context;
        //this.cacheDir = context.getApplicationContext().getFilesDir().getAbsolutePath();//这个地址，文件无法分享。(内部位置无法分享出去),不需要权限
        this.cacheDir = context.getApplicationContext().getExternalCacheDir().getAbsolutePath();//这个位置，可以分享。（SD卡的东西可以分享出去）,不需要权限。推荐使用这个
        //this.cacheDir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();//需要SD卡权限
        mapLoad = new HashMap<>();
        int corePoolSize = Runtime.getRuntime().availableProcessors() + 2;
        int maxinumPoolSize = corePoolSize * 2 + 1;
        long keepAliveTime = 10;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingDeque<>();
        threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxinumPoolSize, keepAliveTime, unit, workQueue);
    }

    //初始化
    public static FileLoadUtils getInstance(Context context) {
        if (fileDown == null) {
            fileDown = new FileLoadUtils(context);
        }
        return fileDown;
    }

    //回调接口
    public interface RequestCallBack {
        public boolean isCall = false;//是否已經回調

        //开始下载，即网络链接成功。
        public void onStart();

        /**
         * 成功
         *
         * @param file 下载文件
         */
        public void onSuccess(File file);

        /**
         * 进度
         *
         * @param current 当前下载大小
         * @param max     总大小
         * @param bias    下载百分比（0-100）
         */
        public void onLoad(long current, long max, int bias);

        /**
         * 失败
         *
         * @param isLoad true 文件已经下载，false文件没有下载
         * @param result 失败原因
         * @param file   下载的文件
         */
        public void onFailure(Boolean isLoad, String result, File file);
    }

    /**
     * 下载
     *
     * @param uri             下载链接
     * @param srcFileName     文件名，包括后缀。如果为null或""空，会自动获取网络上的名称。
     * @param requestCallBack 回调函数
     */
    public void downLoad(final String uri, final String srcFileName, final RequestCallBack requestCallBack) {
        if (mapLoad.get(uri) != null && mapLoad.get(uri)) {
            //判断是否正在下载，防止重复下载。
            return;
        }
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mapLoad.put(uri, true);//标志正在下载
                    URL url = new URL(uri);
                    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);//连接超时设置，绝对有效。一般50毫秒即可连接成功。
                    conn.setRequestMethod("GET");
                    String fileName = srcFileName;
                    if (fileName == null || (fileName != null && fileName.trim().equals(""))) {
                        fileName = getConnFileName(conn);
                    }

                    fileName = cacheDir + "/" + fileName;//文件完整名称，包括路径和文件名后缀
                    final File file = new File(fileName);
                    if (file.exists()) {//文件已经存在
                        if (fileName.contains(".apk")) {//判断下载文件是否是apk包
                            if (getUninatllApkInfo(fileName)) {//判断本地apk包是否完整
                                if (requestCallBack != null) {
                                    requestCallBack.onFailure(true, "apk已經下載", file);
                                }
                                conn.disconnect();//断开链接
                                return;
                            }
                        }
                        // 设置 User-Agent,这个设不设置没有影响
                        conn.setRequestProperty("User-Agent", "NetFox");
                        // 设置断点续传的开始位置,这个是关键，末尾必须加"-"。会返回206
                        String RANGE = "bytes=" + (file.length()) + "-";
                        conn.setRequestProperty("RANGE", RANGE);
                    } else {
                        new File(cacheDir).mkdirs();//新建文件夹(可以多级创建,mkdir不能多级创建),没有才创建，不会覆盖原有文件夹
                        file.createNewFile();//新建文件,没有则创建，有，则不会创建，即不会覆盖原有文件
                    }
                    int ResponseCode = conn.getResponseCode();//一旦调用了get函数，就不能再设置参数了。
                    //file.length()和conn.getContentLength()是对等。都是一个文件的实际大小。
                    long max = conn.getContentLength();//文件总大小,一旦调用了这个方法，就不能再设置参数了【如：setRequestProperty】
                    //判断文件是否存在，以及大小是否相当。避免重复下载。
                    if ((ResponseCode == 200 || ResponseCode == 206) && file.length() != max) {
                        if (requestCallBack != null) {
                            requestCallBack.onStart();
                        }
                        if (conn.getResponseCode() == 206) {//200连接成功，206断点续传。
                            max = file.length() + max;
                        }
                        InputStream inputStream = conn.getInputStream();
                        //下载主要耗时就是花费在对流的读写操作上。
                        //持久化操作，将流转化为本地文件
                        OutputStream output = new FileOutputStream(file, true);//参数二 true,在文件末尾继续写，否则会覆盖原有文件
                        byte[] buffer = new byte[4 * 1024];
                        int iLen = 0;
                        long currentTimeMillis = 0;
                        while ((iLen = inputStream.read(buffer)) != -1) {
                            //耗时操作都在这里。
                            output.write(buffer, 0, iLen);
                            if (requestCallBack != null && (System.currentTimeMillis() - currentTimeMillis) > 1000) {
                                float current = file.length();
                                int bias = (int) (current / max * 100);
                                requestCallBack.onLoad((int) current, max, bias);
                                currentTimeMillis = System.currentTimeMillis();
                            }
                        }
                        output.flush();
                        output.close();
                        if (requestCallBack != null) {
                            requestCallBack.onLoad(max, max, 100);
                            requestCallBack.onSuccess(file);
                        }
                        inputStream.close();
                        inputStream = null;
                        System.gc();
                    } else {
                        //其他连接状态
                        if (ResponseCode == 416 && requestCallBack != null) {
                            requestCallBack.onFailure(true, "文件已經下載", file);
                        }
                    }
                } catch (Exception e) {
                    if (requestCallBack != null) {
                        requestCallBack.onFailure(false, e.getMessage(), null);
                    }
                    Log.e("test", "下载失败异常:\t" + e.getMessage());
                } finally {
                    //标志下载结束
                    mapLoad.put(uri, false);
                    mapLoad.remove(uri);
                    //Log.e("test", "结束");
                }
            }
        });
    }

    //获取网络文件名称
    public String getConnFileName(HttpURLConnection conn) {
        String fileName = conn.getURL().getFile();//conn.getURL()真实的URL
        fileName = fileName
                .substring(fileName.lastIndexOf("/") + 1);// 通过最真实的url获取文件的真实名称
        //去除不合法的字符，以免本地文件生成失败
        fileName = fileName.replace('/', '0');
        fileName = fileName.replace('\\', '0');
        fileName = fileName.replace('|', '0');
        fileName = fileName.replace('?', '0');
        fileName = fileName.replace('<', '0');
        fileName = fileName.replace('>', '0');
        fileName = fileName.replace('*', '0');
        fileName = fileName.replace(':', '0');
        fileName = fileName.replace('"', '0');
        return fileName;
    }

    //将输入流转换为文本【如，读取网络输入流文本】
    public String getConnAsString(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        String temp = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        while ((temp = br.readLine()) != null) {
            //Log.e("test", "读取：\t" + temp);
            sb.append(temp);
        }
        return sb.toString();//转化为文本
    }

    /**
     * 判断apk安装包是否完整
     *
     * @param filePath
     * @return true完整， false apk有损坏，不完整。
     */
    public boolean getUninatllApkInfo(String filePath) {
        boolean result = false;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(filePath,
                    PackageManager.GET_ACTIVITIES);
            if (info != null) {
                result = true;
            }
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    /**
     * 判断下载apk，本地是否已经存在。以免重复下载。
     *
     * @param uri         下载链接
     * @param srcFileName 文件名，包括后缀。如果为null或""空，会自动获取网络上的名称。
     * @return 返回apk完整路径，本地已存在。 null 不存在
     */
    public String judgeApk(final String uri, final String srcFileName) {
        try {
            String fileName = srcFileName;
            if (fileName == null || (fileName != null && fileName.trim().equals(""))) {
                URL url = new URL(uri);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                fileName = getConnFileName(conn);
                conn.disconnect();//断开链接
            }
            fileName = cacheDir + "/" + fileName;//文件完整名称，包括路径和文件名后缀
            final File file = new File(fileName);
            if (file.exists()) {//文件已经存在
                if (fileName.contains(".apk")) {//判断下载文件是否是apk包
                    if (getUninatllApkInfo(fileName)) {//判断本地apk包是否完整
                        return fileName;//本地已经存在
                    }
                }
            }
        } catch (Exception e) {
            Log.e("test", "本地apk判断异常:\t" + e.getMessage());
        }
        return null;
    }

}
