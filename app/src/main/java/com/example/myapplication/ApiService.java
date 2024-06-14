package com.example.myapplication;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface ApiService {
    @GET
    @Streaming // 大文件时需要添加 @Streaming 注解，防止内存溢出
    Call<ResponseBody> downloadAudioFile(@Url String fileUrl);
}
