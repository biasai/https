package cn.android.support.v7.lib.sin.crown.kotlin.https

import android.util.Log
import com.github.biasai.repositories.https.warebia.utils.CacheUtils
import com.github.biasai.repositories.https.warebia.utils.JSonUtils
import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by 彭治铭 on 2018/6/6.
 */
//最后的可变参数，对应json解析的字段
abstract class GenericsCallback<T : Any>(var https: Https? = null, var clazz: Class<T>? = null, vararg var field: String) {

    //开始
    open fun onStart() {
        https?.start?.let {
            it()
            //https?.start!!()
        }
        //fixme 显示进度条
        if (https?.load ?: false) {
            https?.showProgress()
        }
    }

    //成功
    open fun onSuccess(response: String) {
        var result = https?.onPostResponse(response) ?: ""//对服务器返回数据，在解析之前，优先做处理。如解密等
        https?.success?.let {
            it(result)
        }
        if (https?.cacle ?: false) {
            https?.getUrlUnique()?.let {
                //缓存数据
                CacheUtils.getInstance().put(it, result)
            }
        }
        result?.let {
            onResponse(JSonUtils.parseJson(it, *field))
        }
        //最后执行
        onFinish()
    }

    //失败【基本可以断定是网络异常】
    open fun onFailure(errStr: String?) {
        https?.failure?.let {
            it(errStr)
        }
        var response: String? = null
        if (https?.cacle ?: false) {
            https?.getUrlUnique()?.let {
                //读取缓存数据
                response = CacheUtils.getInstance().getAsString(it)
            }
        }
        response?.let {
            onResponse(JSonUtils.parseJson(it, *field))
        }
        //最后执行
        onFinish()
    }

    //返回的数据为服务器原始数据，或缓存数据。如果断网，且缓存数据为空，则返回空。
    open fun onResponse(response: String?) {
        response?.let {
            if (isResponse) {
                //数据解析
                if (isList) {//List数组
                    clazz?.let {
                        var list = ArrayList<T>()
                        var jsonArray: JSONArray? = null
                        try {
                            jsonArray = JSONArray(response)
                        } catch (e: Exception) {
                            Log.e("test", "url:\t" + https?.url + "\t服务器json[== 数组 ==]解析异常，json数据:\t" + response)
                        }
                        var length = jsonArray?.length() ?: 0
                        if (length > 0) {
                            length -= 1
                            for (i in 0..length) {
                                var jsonObjec: JSONObject? = null
                                try {
                                    jsonObjec = jsonArray?.getJSONObject(i)
                                } catch (e: Exception) {
                                    Log.e("test", "url:\t" + https?.url + "\t服务器json解析异常，json数据:\t" + response)
                                }
                                list.add(JSonUtils.parseObject(jsonObjec, it)!!)
                            }
                        }
                        list?.let {
                            //数组类型
                            onResponseList(it)//哪怕json解析异常，也会返回一个空的实例对象
                        }
                    }
                } else {
                    clazz?.let {
                        var jsonObjec: JSONObject? = null
                        try {
                            jsonObjec = JSONObject(response)
                        } catch (e: Exception) {
                            Log.e("test", "url:\t" + https?.url + "\t服务器json解析异常，json数据:\t" + response)
                        }
                        var bean = JSonUtils.parseObject(jsonObjec, it!!)//哪怕json解析异常，也会返回一个空的实例对象
                        bean?.let {
                            //单个实体类类型
                            onResponse(it)
                        }
                    }
                }
            }
        }
    }

    var isResponse = false//是否执行onResponse(t: T)方法，默认不执行
    fun isResponse(isResponse: Boolean = true): GenericsCallback<T> {
        this.isResponse = isResponse
        return this
    }

    //isResponse为true,且数据不为空时调用。
    open fun onResponse(t: T): GenericsCallback<T> {
        return this
    }

    var isList = false//是否返回ArrayList数组格式
    fun isList(isList: Boolean = true): GenericsCallback<T> {
        this.isList = isList
        return this
    }

    open fun onResponseList(t: ArrayList<T>): GenericsCallback<T> {
        return this
    }

    //结束，无论是成功还是失败都会调用。且最后执行
    open fun onFinish() {

        //结束回调
        https?.finish?.let {
            it()
        }

        //fixme 去除网络请求标志(网络请求结束)
        https?.let {
            Http.map.remove(it?.getUrlUnique())
        }

        //fixme 关闭进度条
        if (https?.load ?: false) {
            https?.dismissProgress()
        }
        https?.activity = null
        https?.headers?.clear()
        https?.params?.clear()
        https?.files?.clear()
        https?.body.let {
            https?.body = null
        }
        https?.url = null
        https = null
        clazz = null
    }
}
