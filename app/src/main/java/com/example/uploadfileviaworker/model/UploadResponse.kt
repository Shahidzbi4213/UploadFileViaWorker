package com.example.uploadfileviaworker.model


import com.google.gson.annotations.SerializedName

data class UploadResponse(
    @SerializedName("filename")
    val filename: String, // b5d7.png
    @SerializedName("location")
    val location: String, // https://api.escuelajs.co/api/v1/files/b5d7.png
    @SerializedName("originalname")
    val originalname: String // Screenshot from 2024-08-17 23-42-35.png
)