package com.test.mediacodecdemo.record

import android.media.MediaFormat
import android.media.MediaMuxer
import com.test.mediacodecdemo.bean.MuxerBean
import com.test.mediacodecdemo.util.LogUtil
import java.util.*

/**
 * Created by yangpeng on 2019/2/26.
 */
class MediaMuxerThread(path: String) : Thread(), Runnable {
    private var mVideoTrack: Int = 0
    private var mAudioTrack: Int = 0
    var mIsRecording: Boolean = false
    var mIsMuxerStart: Boolean = false
    val mDataQueue: LinkedList<MuxerBean> = LinkedList()
    var mMediaMuxer: MediaMuxer? = null
    var mVideoRecordThread: VideoRecordThread? = null
    var mAudioRecordThread: AudioRecordThread? = null
    val mPath = path
    var mCallback: MediaMuxerCallback? = null

    public fun prepareMediaMuxer(width: Int, height: Int) {
        mVideoTrack = -1
        mAudioTrack = -1
        mMediaMuxer = MediaMuxer(mPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        mVideoRecordThread = VideoRecordThread(this, width, height)
        mAudioRecordThread = AudioRecordThread(this)
        mVideoRecordThread?.prepare()
        mAudioRecordThread?.prepare()
    }

    public fun startMuxer() {
        if (!mIsMuxerStart && isAudioTrackExist() && isVideoTrackExist()) {
            LogUtil.log("mMediaMuxer start()")
            mMediaMuxer?.start()
            mIsMuxerStart = true
            start()
        }

    }

    public fun onFrame(data: ByteArray) {
        if (mIsRecording) {
            mVideoRecordThread?.onFrame(data)
        }
    }

    public fun isVideoTrackExist(): Boolean {
        return mVideoTrack >= 0
    }

    fun isAudioTrackExist(): Boolean {
        return mAudioTrack >= 0
    }

    public fun addVideoTrack(mediaFormat: MediaFormat) {
        LogUtil.log("addVideoTrack")
        mVideoTrack = mMediaMuxer!!.addTrack(mediaFormat)
        startMuxer()
    }

    public fun addAudioTrack(mediaFormat: MediaFormat) {
        LogUtil.log("addAudioTrack")
        mAudioTrack = mMediaMuxer!!.addTrack(mediaFormat)
        startMuxer()
    }

    public fun begin(width: Int, height: Int) {
        prepareMediaMuxer(width, height)
        mIsRecording = true
        mIsMuxerStart = false
        mAudioRecordThread?.begin()
        mVideoRecordThread?.begin()
    }

    public fun addMutexData(data: MuxerBean) {
        synchronized(mDataQueue) {
            LogUtil.log("addMutexData")
            mDataQueue.offer(data)
        }
    }


    override fun run() {

        while (true) {
            val out = doMuxer()
            if (out) break
        }
        release()
        mCallback?.onFinishMediaMutex(mPath)

    }

    private fun doMuxer(): Boolean {
        var hasNoData: Boolean
        var mMuxerBean: MuxerBean?
        synchronized(mDataQueue) {
            hasNoData = mDataQueue.isEmpty()
            mMuxerBean = mDataQueue.poll()
        }
        if (hasNoData) {
            LogUtil.log("mDataQueue.isEmpty()")
            try {
                Thread.sleep(300)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (!mIsRecording && mDataQueue.isEmpty()) {
                return true
            }
        } else {
            if (mMuxerBean!!.isVideo) {
                LogUtil.log("writeSampleData-video")
                mMediaMuxer?.writeSampleData(mVideoTrack, mMuxerBean!!.byteBuffer, mMuxerBean!!.bufferInfo)
            } else {
                LogUtil.log("writeSampleData-audio")
                mMediaMuxer?.writeSampleData(mAudioTrack, mMuxerBean!!.byteBuffer, mMuxerBean!!.bufferInfo)
            }
        }
        return false

    }

    private fun release() {
        if (mIsMuxerStart) {
            LogUtil.log("mMediaMuxer-release")
            mMediaMuxer?.stop()
            mMediaMuxer?.release()
            mMediaMuxer = null
            mIsMuxerStart = false
        }
    }

    public fun end() {
        try {
            mIsRecording = false
            mVideoRecordThread?.end()
            mVideoRecordThread?.join()
            mVideoRecordThread = null
            mAudioRecordThread?.end()
            mAudioRecordThread?.join()
            mAudioRecordThread = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public fun isMuxerRun(): Boolean {
        return mIsMuxerStart
    }

    public fun setCallback(callback: MediaMuxerCallback) {
        mCallback = callback
    }

    interface MediaMuxerCallback {
        fun onFinishMediaMutex(path: String)
    }
}