package com.test.mediacodecdemo.util

import android.util.Log

/**
 * Created by yangpeng on 2019/3/1.
 */
class LogUtil {
    companion object {
        public fun log(str: String) {
            Log.i("info", "===========" + str);
        }
    }

}