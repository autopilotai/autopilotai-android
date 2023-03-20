package org.autopilotai.objectdetection.iftttconnecter

import com.fasterxml.jackson.annotation.JsonProperty
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException
import java.util.UUID

object AutopilotAIApiHelper {
    private val autopilotAIApi: AutopilotAIApi

    init {
        val client =
            OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://webapp-autopilotai-api.azurewebsites.net")
            .addConverterFactory(JacksonConverterFactory.create())
            .client(client)
            .build()

        autopilotAIApi = retrofit.create(AutopilotAIApi::class.java)
    }

    fun sendImageDescription(imageInfo: ImageInfo): ImageInfo? {
        if (imageInfo == null) {
            return null
        }

        return try {
            val response = autopilotAIApi.sendImageDescription(imageInfo).execute()
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    interface AutopilotAIApi {
        @POST("/request")
        fun sendImageDescription(@Body imageInfo: ImageInfo): Call<ImageInfo>
    }

    data class ImageInfo(
        @JsonProperty("id") val id: UUID,
        @JsonProperty("account") val account: String,
        @JsonProperty("description") val description: String
    )
}
