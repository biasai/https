package cn.android.support.v7.lib.sin.crown.kotlin.https

import android.app.Activity
import android.util.Log
import com.github.biasai.repositories.https.warebia.dialog.ProgressDailog
import java.io.File

open class Https(open var url: String?, open var activity: Activity? = null) {//fixme activity不为空时，回调到主线程

    var timeOut = 3000;//超时链接时间，单位毫秒,一般500毫秒足已。亲测100%有效。极少数设备可能脑抽无效。不用管它。
    fun timeOut(timeOut: Int = this.timeOut): Https {
        this.timeOut = timeOut
        return this
    }

    var load: Boolean = false//是否显示进度条，默认不显示，fixme (Activity不能为空，Dialog需要Activity的支持)
    fun showLoad(isLoad: Boolean = true): Https {
        this.load = isLoad
        return this
    }

    //进度条变量名，子类虽然可以重写，但是类型改不了。所以。进度条就不允许继承了。子类自己去定义自己的进度条。
    var progressbar: ProgressDailog? = null//进度条(Activity不能为空，Dialog需要Activity的支持)

    //fixme 显示进度条[子类要更改进度条，可以重写这个]
    //重写的时候，注意屏蔽父类的方法，屏蔽 super.showProgress()
    open fun showProgress() {
        if (load) {
            if (progressbar == null && activity != null) {
                progressbar = ProgressDailog(activity)
            }
            progressbar?.show()
        }
    }

    //fixme 关闭进度条[子类可以重写,重写的时候，记得对自己的进度条进行内存释放。]
    //重写的时候，注意屏蔽父类的方法，屏蔽 super.showProgress()
    open fun dismissProgress() {
        if (load) {
            progressbar?.let {
                progressbar?.dismiss()
                ProgressDailog.progressDailog = null
                progressbar?.progressView = null
                progressbar?.activity = null//最主要就是对Activity进行释放。
                progressbar = null
            }
        }
    }

    var cacle: Boolean = false//是否缓存，默认不缓存
    fun cacle(isCache: Boolean = true): Https {
        cacle = isCache
        return this
    }

    var repeat: Boolean = false//是否允许网络重复请求。默认不允许重复请求。
    fun repeat(isRepeat: Boolean = true): Https {
        this.repeat = isRepeat
        return this
    }

    var showParams: Boolean = false//是否显示打印参数，默认不打印
    fun showParams(isShowParam: Boolean = true): Https {
        this.showParams = isShowParam
        return this
    }

    //开始回调
    var start: (() -> Unit)? = null

    fun onStart(start: (() -> Unit)? = null): Https {
        this.start = start
        return this
    }

    //成功回调
    var success: ((result: String) -> Unit)? = null

    fun onSuccess(success: ((result: String) -> Unit)? = null): Https {
        this.success = success
        return this
    }

    //失败回调
    var failure: ((errStr: String?) -> Unit)? = null

    fun onFailure(failure: ((errStr: String?) -> Unit)? = null): Https {
        this.failure = failure
        return this
    }

    //结束回调，无论是成功还是失败都会调用(最后执行)
    var finish: (() -> Unit)? = null

    fun onFinish(finish: (() -> Unit)? = null): Https {
        this.finish = finish
        return this
    }

    //参数
    //header头部参数。Get，Post都行
    val headers: MutableMap<String, String> by lazy { mutableMapOf<String, String>() }
    //params属于 body子集。Get，Post都行
    val params: MutableMap<String, String> by lazy { mutableMapOf<String, String>() }
    //files也属于params，文件上传。Pst请求
    val files: MutableMap<String, File> by lazy { mutableMapOf<String, File>() }
    //params,files,body都可以同时使用。Post请求
    var body: String? = null

    //fixme 方法必须放在变量声明之后
    //设置默认参数，以及对参数做一些特殊处理（如加密）。子类可以重写
    //在请求之前会调用。
    open fun onPreParameter() {
        //header默认参数
        //headers.put("1", "1")
        //params默认参数
        //params.put("2", "2")
    }

    //fixme 对服务器返回数据最先处理，做一些特殊处理。
    //fixme 如数据解密等（先解密，然后才进行json解析）。子类可以重写
    open fun onPostResponse(response: String): String {
        return response
    }

    fun body(body: String? = null): Https {
        body?.let {
            this.body = body
        }
        return this
    }

    //添加头部参数
    fun addHeader(key: String, value: String): Https {
        headers.put(key, value)
        return this
    }

    //添加头部参数(融合两个Map)
    fun addHeader(header: MutableMap<String, String>? = null): Https {
        header?.let {
            for ((key, value) in header.entries) {
                headers.put(key, value)
            }
        }
        return this
    }

    fun addParam(key: String, value: String): Https {
        params.put(key, value)
        return this
    }

    fun addParam(param: MutableMap<String, String>? = null): Https {
        param?.let {
            for ((key, value) in param.entries) {
                params.put(key, value)
            }
        }
        return this
    }

    fun addFile(key: String, value: File): Https {
        files.put(key, value)
        return this
    }

    fun addFile(file: MutableMap<String, File>? = null): Https {
        file?.let {
            for ((key, value) in file.entries) {
                files.put(key, value)
            }
        }
        return this
    }


    //打印参数(不需要手动到调用。会在请求调用前，自动调用)
    fun LogParams() {
        if (showParams) {
            Log.e("test", "url:==================================================\t" + url)
            if (headers?.size > 0) {
                Log.e("test", "头部Header=========================================")
                for ((key, value) in headers.entries) {
                    Log.e("test", "key:\t" + key + "\tvalue:\t" + value)
                }
            }
            if (params?.size > 0) {
                Log.e("test", "Params=============================================")
                for ((key, value) in params.entries) {
                    Log.e("test", "key:\t" + key + "\tvalue:\t" + value)
                }
            }
            if (files?.size > 0) {
                Log.e("test", "文件===============================================")
                for ((key, value) in files.entries) {
                    Log.e("test", "key:\t" + key + "\tvalue:\t" + value?.absoluteFile)
                }
            }
            body?.let {
                Log.e("test", "Body===============================================")
                Log.e("test", "body:\t" + body)
            }
        }
    }

    //获取网络请求唯一标志(url+所有参数集合)
    fun getUrlUnique(): String {
        var stringBuffer = StringBuffer("网络请求唯一标志:\t")
        stringBuffer.append(url)
        if (headers?.size > 0) {
            for ((key, value) in headers.entries) {
                stringBuffer.append(key)
                stringBuffer.append(value)
            }
        }
        if (params?.size > 0) {
            for ((key, value) in params.entries) {
                stringBuffer.append(key)
                stringBuffer.append(value)
            }
        }
        if (files?.size > 0) {
            for ((key, value) in files.entries) {
                stringBuffer.append(key)
                stringBuffer.append(value)
            }
        }
        body?.let {
            stringBuffer.append(it)
        }
        //Log.e("test", "" + stringBuffer)
        return stringBuffer.toString()
    }

    //fixme Get请求,所有参数设置完成之后再调用
    fun Get(vararg field: String, callback: ((t: String) -> Unit)? = null) {
        onPreParameter()
        LogParams()
        Http.Get(url, activity, this, requestCallBack = object : GenericsCallback<String>(this, String::class.java, *field) {
            override fun onResponse(response: String?) {
                callback?.let {
                    response?.let {
                        callback(response)//fixme 默认返回原始数据String(包括缓存数据)，数据不为空的时候调用
                    }
                }
                super.onResponse(response)
            }

        }.isResponse(false).isList(false), timeOut = timeOut)
    }

    //fixme 必须传一个实例进去(因为无论是我还是fastjson都无法获取ArrayList里的类型) .Get(BaseBean()){}
    inline fun <reified T : Any> Get(t: T, vararg field: String, noinline callback: (t: T) -> Unit) {
        onPreParameter()
        LogParams()
        Http.Get(url, activity, this, requestCallBack = object : GenericsCallback<T>(this, T::class.java, *field) {
            override fun onResponse(t: T): GenericsCallback<T> {
                callback(t)
                return super.onResponse(t)
            }
        }.isResponse(true).isList(false), timeOut = timeOut)
    }

    //.Get(ArrayList<BaseBean>()){}
    inline fun <reified T : Any> Get(t: ArrayList<T>, vararg field: String, noinline callback: (t: ArrayList<T>) -> Unit) {
        onPreParameter()
        LogParams()
        Http.Get(url, activity, this, requestCallBack = object : GenericsCallback<T>(this, T::class.java, *field) {
            override fun onResponseList(t: ArrayList<T>): GenericsCallback<T> {
                callback(t)
                return super.onResponseList(t)
            }
        }.isResponse(true).isList(true), timeOut = timeOut)
    }

    //fixme Post请求

    //fixme Post请求,所有参数设置完成之后再调用
    fun Post(vararg field: String, callback: ((t: String) -> Unit)? = null) {
        onPreParameter()
        LogParams()
        Http.Post(url, activity, this, requestCallBack = object : GenericsCallback<String>(this, String::class.java, *field) {
            override fun onResponse(response: String?) {
                callback?.let {
                    response?.let {
                        callback(response)//fixme 默认返回原始数据String(包括缓存数据)，数据不为空的时候调用
                    }
                }
                super.onResponse(response)
            }

        }.isResponse(false).isList(false), timeOut = timeOut)
    }

    //fixme 必须创一个实例进去(因为无论是我还是fastjson都无法获取ArrayList里的类型) .Get(BaseBean()){}
    inline fun <reified T : Any> Post(t: T, vararg field: String, noinline callback: (t: T) -> Unit) {
        onPreParameter()
        LogParams()
        Http.Post(url, activity, this, requestCallBack = object : GenericsCallback<T>(this, T::class.java, *field) {
            override fun onResponse(t: T): GenericsCallback<T> {
                callback(t)
                return super.onResponse(t)
            }
        }.isResponse(true).isList(false), timeOut = timeOut)
    }

    //.Get(ArrayList<BaseBean>()){}
    inline fun <reified T : Any> Post(t: ArrayList<T>, vararg field: String, noinline callback: (t: ArrayList<T>) -> Unit) {
        onPreParameter()
        LogParams()
        Http.Post(url, activity, this, requestCallBack = object : GenericsCallback<T>(this, T::class.java, *field) {
            override fun onResponseList(t: ArrayList<T>): GenericsCallback<T> {
                callback(t)
                return super.onResponseList(t)
            }
        }.isResponse(true).isList(true), timeOut = timeOut)
    }

}