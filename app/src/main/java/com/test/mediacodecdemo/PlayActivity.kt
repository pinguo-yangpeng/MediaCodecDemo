package com.test.mediacodecdemo

import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.View
import com.test.mediacodecdemo.play.MediaPlayer
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

/**
 * Created by yangpeng on 2019/3/7.
 */
class PlayActivity : Activity(), SurfaceHolder.Callback {
    var mPath: String
    var mediaPlayer: MediaPlayer? = null

    init {
        var path = Environment.getExternalStorageDirectory().absolutePath + "/testMediacodec/"
        val mDir = File(path)
        if (!mDir.exists()) {
            mDir.mkdirs()
        }
        path += "mediacodec.mp4"
        mPath = path
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button_layout.visibility = View.GONE
        surfaceView.holder.addCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {

    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mediaPlayer = MediaPlayer(mPath)
        mediaPlayer?.setSurface(surfaceView.holder.surface)
        mediaPlayer?.start()
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {

    }

    override fun onBackPressed() {
        super.onBackPressed()
        mediaPlayer?.release()
    }
}