package com.example.data.api

import android.content.Context
import com.example.data.pref.PreferencesManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = "https://courses.tncnursing.site/"

    @Volatile
    private var instance: ApiService? = null

    fun getService(context: Context): ApiService {
        return instance ?: synchronized(this) {
            instance ?: createService(context).also { instance = it }
        }
    }

    private fun createService(context: Context): ApiService {
        val prefManager = PreferencesManager(context)

        val headerInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")

            val installId = prefManager.getInstallId()
            if (installId.isNotBlank()) {
                requestBuilder.header("x-tnc-install-id", installId)
            }

            chain.proceed(requestBuilder.build())
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
