package com.test.mediacodecdemo.play

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import android.view.Surface
import com.test.mediacodecdemo.util.LogUtil

/**
 * Created by yangpeng on 2019/3/6.
 */
class VideoPlayThread(path: String) : Thread() {
    val TIMEOUT_S: Long = 12000
    val mSourcePath = path
    var mSurface: Surface? = null
    var mMediaCodec: MediaCodec? = null

    var mExtractor: MediaExtractor? = null
    public fun setSurface(surface: Surface) {
        mSurface = surface
    }


    public fun prepare() {
        mExtractor = MediaExtractor()
        mExtractor?.setDataSource(mSourcePath)
        var mediaFormat: MediaFormat? = null
        var currentTrack = 0
        for (i in 0 until mExtractor!!.trackCount) {
            mediaFormat = mExtractor?.getTrackFormat(i)
            val mine = mediaFormat!!.getString(MediaFormat.KEY_MIME)
            if (mine!!.startsWith(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                currentTrack = i
                break
            }
        }
        mExtractor?.selectTrack(currentTrack)
        mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        mMediaCodec?.configure(mediaFormat, mSurface, null, 0)
        mMediaCodec?.start()

    }


    override fun run() {
        try {
            val bufferInfo = MediaCodec.BufferInfo()
            val startMs = System.currentTimeMillis()
            var index = 0
            while (true) {
                LogUtil.log("videoplaythread:run")
                val inputIndex = mMediaCodec!!.dequeueInputBuffer(TIMEOUT_S)
                if (inputIndex < 0) {
                    LogUtil.log("videoplaythread:inputIndex<0 locked")
                    SystemClock.sleep(50)//没有输入数据线程阻塞50ms再循环
                    continue
                }
                val inputBuffer = mMediaCodec?.getInputBuffer(inputIndex)
                inputBuffer?.clear()
                val sampleSize = mExtractor!!.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    LogUtil.log("video mExtractor readSampleData err")
                    continue
                }
                mMediaCodec?.queueInputBuffer(inputIndex, 0, sampleSize, mExtractor!!.sampleTime, 0)
                var outputIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_S)
                while (outputIndex > 0) {
                    while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                        SystemClock.sleep(50)
                    }
                    LogUtil.log("videoplaythread:outputIndex > 0")
                    mMediaCodec?.releaseOutputBuffer(outputIndex, true)
                    outputIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_S)
                }
                if (!mExtractor!!.advance()) break
            }
            LogUtil.log("videoplaythread:release")
            release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    public fun release() {
        try {
            mExtractor?.release()
            mExtractor = null
            mMediaCodec?.stop()
            mMediaCodec?.release()
            mMediaCodec = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}