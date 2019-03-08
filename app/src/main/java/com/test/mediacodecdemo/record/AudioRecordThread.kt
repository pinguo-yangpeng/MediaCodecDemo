package com.test.mediacodecdemo.record

import android.media.*
import com.test.mediacodecdemo.bean.MuxerBean
import java.nio.ByteBuffer

/**
 * Created by yangpeng on 2019/2/26.
 */
class AudioRecordThread(muxerThread: MediaMuxerThread) : Thread(), Runnable {
    val TIMEOUT_S: Long = 10000
    val mMuxerThread = muxerThread
    var mMediaCodec: MediaCodec? = null
    var mMediaFormat: MediaFormat? = null
    val mSampleRate = 16000
    val mBitRate = 64000
    var mIsRecording: Boolean = false
    var mAudioRecorder: AudioRecord? = null
    var mMinBufferSize: Int = 0
    var mPrevOutputPTSUs: Long = 0

    fun initMediaCodec() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)

            mMediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, mSampleRate, 1)
            mMediaFormat?.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate)
            mMediaFormat?.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
            mMediaFormat?.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate)

            mMediaCodec?.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mMediaCodec?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initAudioRecorder(): Boolean {
        mMinBufferSize =
            AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecorder = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            mSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            2 * mMinBufferSize
        )
        if (mAudioRecorder?.state != AudioRecord.STATE_INITIALIZED) {
            mIsRecording = false
            return false
        }
        mAudioRecorder?.startRecording()
        return true
    }


    public fun prepare() {
        initMediaCodec()
        initAudioRecorder()
    }

    public fun begin() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
        mPrevOutputPTSUs = 0
        mIsRecording = true
        start()
    }

    public fun end() {
        mIsRecording = false;
    }


    override fun run() {
        val bufferByte: ByteArray = ByteArray(mMinBufferSize)
        var len: Int = 0
        while (mIsRecording) {
            len = mAudioRecorder!!.read(bufferByte, 0, mMinBufferSize)
            if (len > 0) {
                record(bufferByte, len, getPTSUs())
            }
        }
        release()
    }


    fun record(buffer: ByteArray, len: Int, presentationTimeUs: Long) {
        var inputBufferIndex = mMediaCodec!!.dequeueInputBuffer(TIMEOUT_S)
        if (inputBufferIndex >= 0) {
            val inputBuffer = mMediaCodec?.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()
            inputBuffer?.put(buffer)

            if (len <= 0) {
                mMediaCodec?.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    0,
                    presentationTimeUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            } else {
                mMediaCodec?.queueInputBuffer(inputBufferIndex, 0, len, presentationTimeUs, 0)
            }
        }

        var bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_S)
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (!mMuxerThread.isAudioTrackExist()) {
                mMuxerThread.addAudioTrack(mMediaCodec!!.getOutputFormat())
            }
        }
        while (outputBufferIndex >= 0) {
            var outputBuffer = mMediaCodec?.getOutputBuffer(outputBufferIndex)
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                bufferInfo.size = 0
            }

            if (bufferInfo.size > 0) {
                val outData: ByteArray = ByteArray(bufferInfo.size)
                outputBuffer?.get(outData)
                outputBuffer?.position(bufferInfo.offset)
                outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)
                bufferInfo.presentationTimeUs = getPTSUs()
                mMuxerThread.addMutexData(MuxerBean(false, ByteBuffer.wrap(outData), bufferInfo))
                mPrevOutputPTSUs = bufferInfo.presentationTimeUs
            }
            mMediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
            bufferInfo = MediaCodec.BufferInfo()
            outputBufferIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_S)
        }


    }

    private fun getPTSUs(): Long {
        val result = System.nanoTime() / 1000L
        return if (result < mPrevOutputPTSUs) mPrevOutputPTSUs else result
    }

    private fun release() {
        if (mAudioRecorder != null) {
            mAudioRecorder?.stop()
            mAudioRecorder?.release()
            mAudioRecorder = null
        }
        if (mMediaCodec != null) {
            mMediaCodec?.stop()
            mMediaCodec?.release()
            mMediaCodec = null
        }
    }

}