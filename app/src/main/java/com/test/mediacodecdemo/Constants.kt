package com.test.mediacodecdemo

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Created by yangpeng on 2019/2/14.
 */
class Constants {
    companion object {
        /**
         * 屏幕宽高
         */
        var screenWidth: Int = 0
        var screenHeight: Int = 0

        /**
         * 画幅,视频的样式 9:16 1：1 16:9
         */
        val MODE_POR_9_16 = 0
        val MODE_POR_1_1 = 1
        val MODE_POR_16_9 = 2

        /**
         * 三种画幅的具体显示尺寸
         */
        var mode_por_width_9_16: Int = 0
        var mode_por_height_9_16: Int = 0
        var mode_por_width_1_1: Int = 0
        var mode_por_height_1_1: Int = 0
        var mode_por_width_16_9: Int = 0
        var mode_por_height_16_9: Int = 0

        /**
         * 三种画幅的具体编码尺寸(参考VUE)
         */
        val mode_por_encode_width_9_16 = 540
        val mode_por_encode_height_9_16 = 960
        val mode_por_encode_width_1_1 = 540
        val mode_por_encode_height_1_1 = 540
        val mode_por_encode_width_16_9 = 960
        val mode_por_encode_height_16_9 = 540

        fun init(context: Context) {
            val mDisplayMetrics = context.resources
                .displayMetrics
            screenWidth = mDisplayMetrics.widthPixels
            screenHeight = mDisplayMetrics.heightPixels
            mode_por_width_9_16 = screenWidth
            mode_por_height_9_16 = screenHeight
            mode_por_width_1_1 = screenWidth
            mode_por_height_1_1 = screenWidth
            mode_por_width_16_9 = screenWidth
            mode_por_height_16_9 = screenWidth / 16 * 9
        }


        fun getBaseFolder(): String {
            var baseFolder = Environment.getExternalStorageDirectory().toString() + "/Codec/"
            val f = File(baseFolder)
            if (!f.exists()) {
                val b = f.mkdirs()
            }
            return baseFolder
        }

        //获取VideoPath
        fun getPath(path: String, fileName: String): String {
            val p = getBaseFolder() + path
            val f = File(p)
            return if (!f.exists() && !f.mkdirs()) {
                getBaseFolder() + fileName
            } else p + fileName
        }
    }
}