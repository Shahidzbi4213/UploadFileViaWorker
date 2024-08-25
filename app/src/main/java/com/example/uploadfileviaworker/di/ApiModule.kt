package com.example.uploadfileviaworker.di

import com.example.uploadfileviaworker.api.FileUploadApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ApiModule {


    @Provides
    @Singleton
    fun provideOkhttp(): OkHttpClient = OkHttpClient.Builder()
        .readTimeout(2, TimeUnit.MINUTES).writeTimeout(2, TimeUnit.MINUTES)
        .connectTimeout(2, TimeUnit.MINUTES).callTimeout(2, TimeUnit.MINUTES)
        .build()


    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create()).baseUrl("https://api.escuelajs.co/api/v1/").client(okHttpClient).build()

    @Provides
    @Singleton
    fun provideFileUploadApi(retrofit: Retrofit): FileUploadApiService = retrofit.create(FileUploadApiService::class.java)
}