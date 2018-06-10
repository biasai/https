package com.github.biasai.repositories.https.warebia.base;

/**
 * Created by 彭治铭 on 2016/11/2.
 */

public interface BaseListenner<T> {
    public void onResult(T result);
}
