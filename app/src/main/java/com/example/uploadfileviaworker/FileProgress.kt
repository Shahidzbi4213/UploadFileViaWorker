package com.example.uploadfileviaworker

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

fun File.asRequestBodyWithProgress(
    contentType: MediaType? = null,
    progressCallback: ((progress: Float) -> Unit)?
): RequestBody {
    return object : RequestBody() {
        override fun contentType() = contentType

        override fun contentLength() = length()


        override fun writeTo(sink: BufferedSink) {
            val fileLength = contentLength()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val inSt = FileInputStream(this@asRequestBodyWithProgress)
            var uploaded = 0L
            inSt.use {
                var read: Int = inSt.read(buffer)
                val handler = Handler(Looper.getMainLooper())
                while (read != -1) {
                    progressCallback?.let {
                        uploaded += read
                        val progress = (uploaded.toDouble() / fileLength.toDouble()).toFloat()
                        handler.post { it(progress) }

                        sink.write(buffer, 0, read)
                    }
                    read = inSt.read(buffer)
                }
            }
        }
    }
}