package com.test.mediacodecdemo.play

import android.view.Surface

/**
 * Created by yangpeng on 2019/3/7.
 */
class MediaPlayer(path: String) {
    val mVideoPlayThread: VideoPlayThread
    val mAudioPlayThread: AudioPlayThread

    init {
        mVideoPlayThread = VideoPlayThread(path)
        mAudioPlayThread = AudioPlayThread(path)
    }

    public fun setSurface(surface: Surface) {
        mVideoPlayThread.setSurface(surface)
    }

    public fun start() {
        mVideoPlayThread.prepare()
        mAudioPlayThread.prepare()

        mVideoPlayThread.start()
        mAudioPlayThread.start()
    }

    public fun release() {
        mVideoPlayThread.release()
        mAudioPlayThread.release()
    }
}