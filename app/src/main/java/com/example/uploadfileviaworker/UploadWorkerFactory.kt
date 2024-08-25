package com.example.uploadfileviaworker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.uploadfileviaworker.api.FileUploadApiService
import javax.inject.Inject

class UploadWorkerFactory @Inject constructor(private val uploadApiService: FileUploadApiService) : WorkerFactory() {

    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker {
        return UploadWorker(appContext, workerParameters, uploadApiService)
    }
}