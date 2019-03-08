package com.test.mediacodecdemo.record

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.test.mediacodecdemo.bean.MuxerBean
import com.test.mediacodecdemo.util.LogUtil
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by yangpeng on 2019/2/26.
 */
class VideoRecordThread(muxerThread: MediaMuxerThread, width: Int, height: Int) : Thread(), Runnable {
    val TIMEOUT_S: Long = 10000
    val mMuxerThread = muxerThread
    val mWidth = width
    val mHeight = height
    val dataQueue: LinkedList<ByteArray> = LinkedList()
    var mMediaCodec: MediaCodec? = null
    var mMediaFormat: MediaFormat? = null
    val mFrameRate: Int = 30
    var mBitRate: Int = height * width * 3 * 8 * mFrameRate / 256
    val mIFrameInterval: Int = 10
    var mIsRecording: Boolean = false
    val mYuv420sp: ByteArray = ByteArray(width * height * 3 / 2)


    private fun initMediacodec() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)

            mMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight)
            mMediaFormat?.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            )
            mMediaFormat?.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate)
            mMediaFormat?.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate)
            mMediaFormat?.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval)

            mMediaCodec?.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mMediaCodec?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public fun onFrame(data: ByteArray) {
        if (mIsRecording) {
            // LogUtil.log("VideoThread--OnFrame")
            synchronized(dataQueue) {
                dataQueue.offer(data)
            }
        }
    }

    public fun prepare() {
        initMediacodec()
    }

    public fun begin() {
        dataQueue.clear()
        mIsRecording = true
        start()
    }

    public fun end() {
        LogUtil.log("VideoThread--end()")
        mIsRecording = false;
    }

    override fun run() {
        while (mIsRecording) {
            //LogUtil.log("VideoThread--dataQueue.poll()")
            var data: ByteArray? = null
            synchronized(dataQueue) {
                data = dataQueue.poll()
            }
            if (data != null) {
                // LogUtil.log("VideoThread--dataQueue.poll()-data != null")
                NV21toI420SemiPlanar(data!!, mYuv420sp, mWidth, mHeight)
                encode(mYuv420sp)
            }
        }
        release()
    }

    /*
    * 将nv21转换成I420格式再处理
    * */
    private fun NV21toI420SemiPlanar(nv21bytes: ByteArray, i420bytes: ByteArray, width: Int, height: Int) {
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height)
        var i = width * height
        while (i < nv21bytes.size) {
            i420bytes[i] = nv21bytes[i + 1]
            i420bytes[i + 1] = nv21bytes[i]
            i += 2
        }
    }

    private fun encode(input: ByteArray) {
        val inputBufferIndex = mMediaCodec!!.dequeueInputBuffer(TIMEOUT_S)
        if (inputBufferIndex >= 0) {
            val pts = getPts()
            val inputBuffer = mMediaCodec?.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()
            inputBuffer?.put(input)
            mMediaCodec?.queueInputBuffer(inputBufferIndex, 0, input.size, pts, 0)
        }

        var bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_S)
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (!mMuxerThread.isVideoTrackExist()) {
                mMuxerThread.addVideoTrack(mMediaCodec!!.getOutputFormat())
            }
        }

        while (outputBufferIndex >= 0) {
            val outputBuffer = mMediaCodec?.getOutputBuffer(outputBufferIndex)
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                bufferInfo.size = 0
            }
            if (bufferInfo.size > 0) {
                val outData: ByteArray = ByteArray(bufferInfo.size)
                outputBuffer?.get(outData)
                outputBuffer?.position(bufferInfo.offset)
                outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)
                bufferInfo.presentationTimeUs = getPts()
                mMuxerThread.addMutexData(MuxerBean(true, ByteBuffer.wrap(outData), bufferInfo))
            }

            mMediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
            bufferInfo = MediaCodec.BufferInfo()
            outputBufferIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_S)
        }
    }


    private fun getPts(): Long {
        return System.nanoTime() / 1000L
    }

    private fun release() {
        // 停止编解码器并释放资源
        try {
            mMediaCodec?.stop()
            mMediaCodec?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}