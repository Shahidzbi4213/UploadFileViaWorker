package com.example.uploadfileviaworker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

fun File.asRequestBodyWithProgress(
    progressCallback: suspend (progress: Long) -> Unit
): RequestBody {
    return object : RequestBody() {

        val file = this@asRequestBodyWithProgress

        override fun contentType(): MediaType? {
            return file.extension.toMediaTypeOrNull()
        }

        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: BufferedSink) {
            val totalLength = contentLength()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var uploadedBytes = 0L

            FileInputStream(file).use { fis ->
                var bytesRead = fis.read(buffer)
                while (bytesRead != -1) {
                    sink.write(buffer, 0, bytesRead)
                    uploadedBytes += bytesRead
                    val progress = (100 * uploadedBytes / totalLength)

                    runBlocking {
                        withContext(Dispatchers.Main) {
                            progressCallback(progress)
                        }
                    }

                    bytesRead = fis.read(buffer)
                }
            }
        }
    }
}