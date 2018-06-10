package cn.android.support.v7.lib.sin.crown.kotlin.https

import android.app.Activity
import kotlinx.coroutines.experimental.async
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

//获取得到的数据，Log打印不出中文，但是JSON解析是可以解析中文的。
object Http {
    //存储网络请求（防止网络重复请求）
    var map: MutableMap<String, String> = mutableMapOf()

    fun Get(url: String?, activity: Activity? = null, requestParams: Https?, requestCallBack: GenericsCallback<*>? = null, timeOut: Int = 3000) {
        url?.let {
            requestParams?.let {
                if (!it.repeat) {
                    //不允许网络重复请求
                    if (map.containsKey(it.getUrlUnique())) {
                        //Log.e("test","重复了get")
                        return
                    }
                }
                map.put(it?.getUrlUnique(), "网络请求标志开始")//去除标志，在onFinish()方法里
            }

            //开启协程协议
            async {
                //fixme 开始链接
                activity?.runOnUiThread {
                    requestCallBack?.onStart()
                } ?: requestCallBack?.onStart()

                var result: String? = ""//返回数据
                var errStr: String? = null//异常信息
                var input: BufferedReader? = null
                var urlNameString = url
                //fixme 参数 params
                if (requestParams?.params?.size ?: 0 > 0) {
                    val sb = StringBuffer()
                    for ((key, value) in requestParams?.params?.entries!!) {
                        sb.append(key)
                        sb.append("=")
                        sb.append(value)
                        sb.append("&")
                    }
                    urlNameString = urlNameString + "?" + sb.substring(0, sb.length - 1)//Get传值，其实也是params，都在body里面
                }
                val realUrl = URL(urlNameString)
                // 打开和URL之间的连接
                val connection = realUrl.openConnection()
                //fixme 参数 header
                if (requestParams?.headers?.size ?: 0 > 0) {
                    for ((key, value) in requestParams?.headers?.entries!!) {
                        connection.setRequestProperty(key, value)
                        //Log.e("test", "键：\t" + e.getKey() + "\t值：\t" + e.getValue());
                    }
                }
                try {
                    //超时设置（放在异常捕捉里，防止不生效。）
                    connection.connectTimeout = timeOut//设置连接主机超时（单位：毫秒）。时间设置绝对有效。前提：手机开机之后至少必须连接一次网络，其后再断网。都有效。如果手机开机就没有网络,则设置无效。
                    connection.readTimeout = 0//设置从主机读取数据超时（单位：毫秒）。默认就是0。实际的连接超时时间的 ConnectTimeout+ReadTimeout
                    // 设置通用的请求属性
                    connection.setRequestProperty("accept", "*/*")
                    connection.setRequestProperty("connection", "Keep-Alive")
                    connection.setRequestProperty("user-agent",
                            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)")
                    // Get设置如下。
                    // POST两个都必须是true。Get不能。Get必须设置如下。默认就是如下。
                    connection.doOutput = false//不允许写
                    connection.doInput = true//只允许读
                    // 建立实际的连接
                    connection.connect()
                    try {
                        // 定义 BufferedReader输入流来读取URL的响应
                        input = BufferedReader(InputStreamReader(
                                connection.getInputStream()))
                        var line = input.readLine()
                        while (line != null) {
                            result += line
                            line = input.readLine()
                        }
                    } catch (e: Exception) {
                        //fixme 异常一 流异常
                        result = null
                        errStr = e.message
                    } finally {
                        // 使用finally块来关闭输入流
                        input?.close()
                    }
                } catch (e: Exception) {
                    //fixme 异常二 网络链接异常
                    result = null
                    errStr = e.message
                }
                if (result != null && errStr == null) {
                    //fixme 成功
                    activity?.runOnUiThread {
                        requestCallBack?.onSuccess(result)
                    } ?: requestCallBack?.onSuccess(result)

                } else {
                    //fixme 失败
                    activity?.runOnUiThread {
                        requestCallBack?.onFailure(errStr)
                    } ?: requestCallBack?.onFailure(errStr)
                }
            }
        }
    }

    fun Post(url: String?, activity: Activity? = null, requestParams: Https?, requestCallBack: GenericsCallback<*>? = null, timeOut: Int = 3000) {
        url?.let {
            requestParams?.let {
                if (!it.repeat) {
                    //不允许网络重复请求
                    if (map.containsKey(it.getUrlUnique())) {
                        //Log.e("test","重复了post")
                        return
                    }
                }
                map.put(it?.getUrlUnique(), "网络请求标志开始")//去除标志，在onFinish()方法里
            }
            //开启协程协议
            async {
                //fixme 开始链接
                activity?.runOnUiThread {
                    requestCallBack?.onStart()
                } ?: requestCallBack?.onStart()

                var errStr: String? = null//异常信息
                var result: String? = ""//返回信息
                try {
                    val BOUNDARY = UUID.randomUUID().toString()
                    val PREFIX = "--"
                    val LINEND = "\r\n"
                    val MULTIPART_FROM_DATA = "multipart/form-data" //图片上传格式
                    val CHARSET = "UTF-8"
                    val uri = URL(url)
                    //android 6.0(23)淘汰的是 HttpClient。HttpURLConnection是纯java的。是可以使用的。不需要任何第三方包。
                    val conn = uri.openConnection() as HttpURLConnection//fixme 打开链接
                    //conn.setReadTimeout(10 * 1000); // 缓存的最长时间
                    conn.connectTimeout = timeOut//超时连接，超过这个时间还没连接上，就会连接失败
                    conn.readTimeout = 0
                    conn.doInput = true// 允许输入
                    conn.doOutput = true// 允许输出
                    conn.useCaches = false // 不允许使用缓存
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("connection", "keep-alive")
                    conn.setRequestProperty("Charsert", CHARSET)
                    conn.setRequestProperty("Content-Type", "$MULTIPART_FROM_DATA;boundary=$BOUNDARY")

                    //fixme 参数 header
                    if (requestParams?.headers?.size ?: 0 > 0) {
                        for ((key, value) in requestParams?.headers?.entries!!) {
                            conn.setRequestProperty(key, value)
                            //Log.e("test", "键：\t" + e.getKey() + "\t值：\t" + e.getValue());
                        }
                    }
                    var outStream = DataOutputStream(conn.outputStream)

                    // 首先组拼文本类型的参数
                    var sb = StringBuilder()
                    //fixme 参数 params
                    if (requestParams?.params?.size ?: 0 > 0) {
                        for (entry in requestParams?.params?.entries!!) {
                            sb.append(PREFIX)
                            sb.append(BOUNDARY)
                            sb.append(LINEND)
                            sb.append("Content-Disposition: form-data; name=\"" + entry.key + "\"" + LINEND)//键
                            sb.append("Content-Type: text/plain; charset=$CHARSET$LINEND")
                            sb.append("Content-Transfer-Encoding: 8bit$LINEND")
                            sb.append(LINEND)
                            sb.append(entry.value)//值
                            sb.append(LINEND)
                        }
                        outStream.write(sb.toString().toByteArray())
                    }

                    // fixme 参数files 发送文件数据
                    if (requestParams?.files?.size ?: 0 > 0) {
                        for (file in requestParams?.files?.entries!!) {
                            if (file.value == null || file.value.toString().trim().equals("") || file.value.toString().trim().equals("null")) {
                                continue
                            }
                            if (file.value.isFile && file.value.exists()) {
                                val sb1 = StringBuilder()
                                sb1.append(PREFIX)
                                sb1.append(BOUNDARY)
                                sb1.append(LINEND)
                                sb1.append("Content-Disposition: form-data; name=" + file.key.trim({ it <= ' ' }) + "; filename=\""
                                        + file.value.getName() + "\"" + LINEND)
                                sb1.append("Content-Type: application/octet-stream; charset=$CHARSET$LINEND")
                                sb1.append(LINEND)
                                outStream.write(sb1.toString().toByteArray())
                                val input = FileInputStream(file.value)
                                val buffer = ByteArray(1024)
                                var len = input.read(buffer)
                                while (len != -1) {
                                    outStream.write(buffer, 0, len)
                                    len = input.read(buffer)
                                }
                                input.close()
                                outStream.write(LINEND.toByteArray())
                            }
                        }
                    }
                    //fixme 参数 body (body,param,file独立存在，可以同时使用，亲测可行)
                    requestParams?.body?.let {
                        if (requestParams?.body?.length ?: 0 > 0) {
                            outStream.writeUTF(requestParams?.body)//out整体就是一个body,
                        }
                    }
                    // 请求结束标志
                    val end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).toByteArray()
                    outStream.write(end_data)
                    outStream.flush()
                    // 得到响应码
                    val res = conn.responseCode

                    // 定义BufferedReader输入流来读取URL的响应
                    var input = BufferedReader(
                            InputStreamReader(conn.inputStream, CHARSET))//解决中文乱码。
                    if (res == 200) {
                        var line = input.readLine()
                        while (line != null) {
                            result += line
                            line = input.readLine()
                        }
                    }
                    outStream.close()
                    conn.disconnect()//fixme 断开链接
                    //String result = sb2.toString();
                    //result = new String(result.getBytes("iso-8859-1"), CHARSET);//这个可以解决中文乱码。以上方法已经解决了乱码问题。这个不用了。
                } catch (e: Exception) {
                    result = null;
                    errStr = e.message//fixme 异常一
                }
                if (result != null && errStr == null) {
                    //fixme 成功
                    activity?.runOnUiThread {
                        requestCallBack?.onSuccess(result)
                    } ?: requestCallBack?.onSuccess(result)

                } else {
                    //fixme 失败
                    activity?.runOnUiThread {
                        requestCallBack?.onFailure(errStr)
                    } ?: requestCallBack?.onFailure(errStr)
                }
            }
        }
    }

}