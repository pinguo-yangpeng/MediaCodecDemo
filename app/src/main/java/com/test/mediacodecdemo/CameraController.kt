package com.test.mediacodecdemo

import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import java.io.IOException
import java.util.*

/**
 * Created by yangpeng on 2019/2/13.
 */
class CameraController {
    var mCamera: Camera? = null
    var mConfig: Config
    var mPreSize: Point? = null
    var mPicSize: Point? = null

    init {
        mConfig = Config()
        mConfig.minPreviewWidth = 720
        mConfig.minPictureWidth = 720
        mConfig.rate = 1.778f
    }

    fun openCamera(cameraId: Int) {
        mCamera = Camera.open(cameraId)
        val parameter = mCamera!!.parameters
        val preSize = getPropPreviewSize(
            parameter.getSupportedPreviewSizes(), mConfig.rate, mConfig.minPreviewWidth
        )
        val picSize = getPropPreviewSize(
            parameter.getSupportedPictureSizes(), mConfig.rate,
            mConfig.minPictureWidth
        )
        parameter.setPictureSize(picSize.width, picSize.height)
        parameter.setPreviewSize(preSize.width, preSize.height)
        parameter.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameter.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameter.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameter.previewFormat = ImageFormat.NV21;
        mCamera!!.parameters = parameter
        mCamera!!.setDisplayOrientation(90);

        val pre = mCamera!!.parameters.previewSize
        val pic = mCamera!!.parameters.pictureSize
        mPicSize = Point(pic.width, pic.height)
        mPreSize = Point(pre.width, pre.height)
    }

    fun setPreviewTexture(texture: SurfaceTexture) {
        try {
            Log.e("hero", "----setPreviewTexture")
            mCamera?.setPreviewTexture(texture)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setPreviewHolder(surfaceHolder: SurfaceHolder?) {
        mCamera?.setPreviewDisplay(surfaceHolder)
    }

    fun setConfig(config: Config) {
        this.mConfig = config
    }

    fun setOnPreviewFrameCallback(callback: PreviewFrameCallback) {
        mCamera?.setPreviewCallback(Camera.PreviewCallback { data, camera ->
            if (data != null) {
                callback.onPreviewFrame(
                    data,
                    mPreSize!!.x,
                    mPreSize!!.y
                )
            }
        })
    }

    fun preview() {
        mCamera?.startPreview()
        mCamera?.autoFocus(object : Camera.AutoFocusCallback {
            override fun onAutoFocus(p0: Boolean, p1: Camera?) {

            }
        })
    }

    fun getPreviewSize(): Point? {
        return mPreSize
    }

    fun getPictureSize(): Point? {
        return mPicSize
    }

    fun close(): Boolean {
        mCamera?.stopPreview()
        mCamera?.setPreviewCallback(null)
        mCamera?.release()
        mCamera = null
        return false
    }

    /**
     * 手动聚焦
     *
     * @param point 触屏坐标 必须传入转换后的坐标
     */
    fun onFocus(point: Point, callback: Camera.AutoFocusCallback) {
        val parameters = mCamera!!.getParameters()
        var supportFocus = true
        var supportMetering = true
        //不支持设置自定义聚焦，则使用自动聚焦，返回
        if (parameters.maxNumFocusAreas <= 0) {
            supportFocus = false
        }
        if (parameters.maxNumMeteringAreas <= 0) {
            supportMetering = false
        }
        val areas = ArrayList<Camera.Area>()
        val areas1 = ArrayList<Camera.Area>()
        //再次进行转换
        point.x = (point.x.toFloat() / Constants.screenWidth * 2000 - 1000) as Int
        point.y = (point.y.toFloat() / Constants.screenHeight * 2000 - 1000) as Int

        var left = point.x - 300
        var top = point.y - 300
        var right = point.x + 300
        var bottom = point.y + 300
        left = if (left < -1000) -1000 else left
        top = if (top < -1000) -1000 else top
        right = if (right > 1000) 1000 else right
        bottom = if (bottom > 1000) 1000 else bottom
        areas.add(Camera.Area(Rect(left, top, right, bottom), 100))
        areas1.add(Camera.Area(Rect(left, top, right, bottom), 100))
        if (supportFocus) {
            parameters.focusAreas = areas
        }
        if (supportMetering) {
            parameters.meteringAreas = areas1
        }

        try {
            mCamera?.setParameters(parameters)// 部分手机 会出Exception（红米）
            mCamera?.autoFocus(callback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPropPreviewSize(list: List<Camera.Size>, th: Float, minWidth: Int): Camera.Size {

        Collections.sort<Camera.Size>(list, sizeComparator)
        var i = 0
        for (s in list) {
            if (s.height >= minWidth && equalRate(s, th)) {
                break
            }
            i++
        }
        if (i == list.size) {
            i = 0
        }
        return list[i]
    }

    private fun equalRate(s: Camera.Size, rate: Float): Boolean {
        val r = s.width.toFloat() / s.height.toFloat()
        return if (Math.abs(r - rate) <= 0.03) {
            true
        } else {
            false
        }
    }

    private val sizeComparator = Comparator<Camera.Size> { lhs, rhs ->
        if (lhs.height == rhs.height) {
            0
        } else if (lhs.height > rhs.height) {
            1
        } else {
            -1
        }
    }

    class Config {
        var rate = 1.778f //宽高比
        var minPreviewWidth: Int = 0
        var minPictureWidth: Int = 0
    }

    interface PreviewFrameCallback {
        fun onPreviewFrame(bytes: ByteArray, width: Int, height: Int)
    }
}