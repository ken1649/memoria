package com.guoyuan.memoria.utils

import android.util.Log
import com.guoyuan.memoria.BuildConfig

object Logger {
    // 透過 BuildConfig.DEBUG 自動判斷目前是否為 Debug 版本
    private val isDebug = BuildConfig.DEBUG

    fun d(tag: String, message: String) {
        if (isDebug) {
            Log.d(tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebug) {
            Log.e(tag, message, throwable)
        }
    }

    // 你可以根據需要新增 i, w, v 等方法
}