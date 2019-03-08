package com.test.mediacodecdemo

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import com.test.mediacodecdemo.record.MediaMuxerThread
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {
    val mCameraController: CameraController
    var mMuxerThread: MediaMuxerThread
    var mPath: String

    init {
        var path = Environment.getExternalStorageDirectory().absolutePath + "/testMediacodec/"
        val mDir = File(path)
        if (!mDir.exists()) {
            mDir.mkdirs()
        }
        mPath = path + "mediacodec.mp4"
        mMuxerThread = MediaMuxerThread(mPath)
        mCameraController = CameraController()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        surfaceView.holder.addCallback(this)
        button.setOnClickListener {
            if (mMuxerThread.mIsMuxerStart) {
                mMuxerThread.end()
                button.setText("开始录制")
            } else {
                mMuxerThread = MediaMuxerThread(mPath)
                mMuxerThread.begin(mCameraController.mPreSize!!.y, mCameraController.mPreSize!!.x)
                button.setText("结束录制")
            }
        }
        button1.setOnClickListener {
            startActivity(Intent(this@MainActivity, PlayActivity::class.java))
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
        mCameraController.preview()
        mCameraController.setOnPreviewFrameCallback(object : CameraController.PreviewFrameCallback {
            override fun onPreviewFrame(bytes: ByteArray, width: Int, height: Int) {
                mMuxerThread.onFrame(rotateYUV420Degree90(bytes, width, height))
            }
        })
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mCameraController.openCamera(0)
        mCameraController.setPreviewHolder(holder)
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        mCameraController.close()
    }

    fun rotateYUV420Degree90(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
        val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
        var i = 0
        for (x in 0 until imageWidth) {
            for (y in imageHeight - 1 downTo 0) {
                yuv[i] = data[y * imageWidth + x]
                i++
            }
        }
        i = imageWidth * imageHeight * 3 / 2 - 1
        var x = imageWidth - 1
        while (x > 0) {
            for (y in 0 until imageHeight / 2) {
                yuv[i] = data[imageWidth * imageHeight + y * imageWidth + x]
                i--
                yuv[i] = data[imageWidth * imageHeight + y * imageWidth + (x - 1)]
                i--
            }
            x = x - 2
        }
        return yuv
    }
}
