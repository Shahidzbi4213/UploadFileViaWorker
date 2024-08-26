package com.example.uploadfileviaworker

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

fun File.asRequestBodyWithProgress(
    progressCallback: ((progress: Long) -> Unit)
): RequestBody {
    return object : RequestBody() {


        override fun contentType(): MediaType? {
            return this@asRequestBodyWithProgress.extension.toMediaTypeOrNull()
        }

        override fun contentLength() = length()


        override fun writeTo(sink: BufferedSink) {
            runCatching {
                val total = this@asRequestBodyWithProgress.length()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var uploaded = 0L
                FileInputStream(this@asRequestBodyWithProgress).use { fis ->
                    var read: Int
                    val handler = Handler(Looper.getMainLooper())
                    while (fis.read(buffer).also { read = it } != -1) {
                        handler.post {
                            progressCallback(100 * uploaded / total)
                        }
                        uploaded += read.toLong()
                        sink.write(buffer, 0, read)
                    }
                }
            }
        }
    }
}