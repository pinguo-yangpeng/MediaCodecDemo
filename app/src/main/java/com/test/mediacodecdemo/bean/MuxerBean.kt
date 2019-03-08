package com.test.mediacodecdemo.bean

import android.media.MediaCodec
import java.nio.ByteBuffer

/**
 * Created by yangpeng on 2019/2/26.
 */
data class MuxerBean(val isVideo: Boolean, val byteBuffer: ByteBuffer, val bufferInfo: MediaCodec.BufferInfo)