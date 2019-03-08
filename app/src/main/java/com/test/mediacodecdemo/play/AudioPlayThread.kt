package com.test.mediacodecdemo.play

import android.media.*
import android.os.SystemClock
import com.test.mediacodecdemo.util.LogUtil

/**
 * Created by yangpeng on 2019/3/6.
 */
class AudioPlayThread(path: String) : Thread() {
    val mSourcePath = path
    val TIMEOUT_S: Long = 10000
    var mAudioTrack: AudioTrack? = null
    var mExtractor: MediaExtractor? = null
    var mMediaCodec: MediaCodec? = null

    public fun prepare() {
        mExtractor = MediaExtractor()
        mExtractor?.setDataSource(mSourcePath)
        var mediaFormat: MediaFormat? = null
        var currentTrack = 0
        for (i in 0 until mExtractor!!.trackCount) {
            mediaFormat = mExtractor?.getTrackFormat(i)
            val mine = mediaFormat?.getString(MediaFormat.KEY_MIME)
            if (mine!!.startsWith(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                currentTrack = i
                break
            }
        }
        mExtractor?.selectTrack(currentTrack)
        val sampleRate = mediaFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelConfig = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var miniBuffersize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
        miniBuffersize = Math.max(miniBuffersize, 1024)
        mAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT,
            miniBuffersize,
            AudioTrack.MODE_STREAM
        )
        mAudioTrack?.play()
        mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        mMediaCodec?.configure(mediaFormat, null, null, 0)
        mMediaCodec?.start()
    }


    override fun run() {
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val inputIndex = mMediaCodec!!.dequeueInputBuffer(TIMEOUT_S)
            if (inputIndex < 0) {
                SystemClock.sleep(50)//没有输入数据线程阻塞50ms再循环
                continue
            }
            val inputBuffer = mMediaCodec!!.getInputBuffer(inputIndex)
            inputBuffer?.clear()
            val sampleSize = mExtractor!!.readSampleData(inputBuffer!!, 0)
            if (sampleSize < 0) {
                LogUtil.log("audio mExtractor readSampleData err")
                break
            }
            mMediaCodec?.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0)
            mExtractor?.advance()
            var outputIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_S)
            var chunkPCM: ByteArray?
            if (outputIndex > 0) {
                val outputBuffer = mMediaCodec?.getOutputBuffer(outputIndex)
                chunkPCM = ByteArray(bufferInfo.size)
                outputBuffer?.get(chunkPCM)
                outputBuffer?.clear()
                mAudioTrack?.write(chunkPCM, 0, chunkPCM!!.size)
                mMediaCodec?.releaseOutputBuffer(outputIndex, false)
                outputIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_S)
            }

        }

        release()
    }


    public fun release() {
        mMediaCodec?.stop()
        mMediaCodec?.release()
        mMediaCodec = null

        mExtractor?.release()
        mExtractor = null

        mAudioTrack?.stop()
        mAudioTrack?.release()
        mAudioTrack = null
    }
}