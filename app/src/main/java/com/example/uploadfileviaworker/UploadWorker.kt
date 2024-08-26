package com.example.uploadfileviaworker

import android.annotation.SuppressLint
import android.app.Notification
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.uploadfileviaworker.api.FileUploadApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.random.Random


@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted val context: Context, @Assisted val parameters: WorkerParameters, private val uploadApiService: FileUploadApiService
) : CoroutineWorker(context, parameters) {

    private var notificationId: Int = 0

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {


        return withContext(Dispatchers.IO) {
            val fileUri = Uri.parse(parameters.inputData.getString("fileUri"))
            val file = File(context.filesDir, "${System.currentTimeMillis()}.${getFileExtension(fileUri)}")
            val fileName = file.name.toString()
            notificationId = file.name.substringBeforeLast(".").toLong().toInt()
            setForeground(createForegroundInfo(file.name))

            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }


            val fileAsRequestBody = file.asRequestBodyWithProgress { uploaded ->

                Log.d("WorkerRequest", "doWork: Uploaded $uploaded")

                NotificationManagerCompat.from(context).notify(
                    notificationId, getNotification(
                        fileName = fileName,
                        progress = uploaded.toInt()
                    )
                )
            }
            val fileAsPart = MultipartBody.Part.createFormData("file", file.name, fileAsRequestBody)
            val response = uploadApiService.uploadFile(fileAsPart)

            try {
                if (response.isSuccessful) {
                    Log.d("MyWorkResponse", "doWork: ${response.body()}")
                    file.delete()
                    Result.success(Data.Builder().putString("response", response.body().toString()).build())
                } else {
                    file.delete()
                    Result.failure(Data.Builder().putString("error", response.errorBody().toString()).build())
                }
            } catch (e: CancellationException) {
                Result.retry()
            } catch (e: java.io.InterruptedIOException) {
                Result.retry()
            } catch (e: Exception) {
                file.delete()
                Result.failure(Data.Builder().putString("error", e.message).build())
            } catch (e: java.lang.Exception) {
                file.delete()
                Result.failure(Data.Builder().putString("error", e.message).build())
            }
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        val contentResolver: ContentResolver = context.contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val extension = mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
        return extension
    }


    private fun createForegroundInfo(fileName: String): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId, getNotification(fileName, 0),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, getNotification(fileName, 0))
        }

    private fun getNotification(fileName: String, progress: Int = 0): Notification = NotificationCompat.Builder(context, "uploader")
        .setSmallIcon(android.R.drawable.ic_notification_overlay)
        .setContentTitle("File is Uploading")
        .setContentText(fileName)
        .setProgress(100, progress, false)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .build()


}