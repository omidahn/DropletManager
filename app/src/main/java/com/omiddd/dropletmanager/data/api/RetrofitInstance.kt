package com.omiddd.dropletmanager.data.api

import com.omiddd.dropletmanager.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val BASE_URL = "https://api.digitalocean.com/"

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
    }

    private val retrofitCache = ConcurrentHashMap<String, DigitalOceanService>()

    fun getClient(token: String): DigitalOceanService {
        return retrofitCache[token] ?: synchronized(this) {
            retrofitCache[token] ?: buildService(token).also { retrofitCache[token] = it }
        }
    }

    fun clear(token: String? = null) {
        if (token == null) {
            retrofitCache.clear()
        } else {
            retrofitCache.remove(token)
        }
    }

    private fun buildService(token: String): DigitalOceanService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(token))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DigitalOceanService::class.java)
    }
}
