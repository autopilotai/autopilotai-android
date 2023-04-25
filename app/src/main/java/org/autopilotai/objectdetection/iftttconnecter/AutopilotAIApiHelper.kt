package org.autopilotai.objectdetection.iftttconnecter

import com.fasterxml.jackson.annotation.JsonProperty
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header
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

    fun getIFTTTUserToken(token: String, userInfo: UserInfo): String? {
        if (userInfo == null) {
            return null
        }

        return try {
            val response = autopilotAIApi.getIFTTTUserToken(token, userInfo).execute()
            if (response.isSuccessful) {
                response.body()?.user_token
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    fun getImageLabels(token: String, connectionInfo: ConnectionInfo): String? {
        if (connectionInfo == null) {
            return null
        }

        return try {
            val response = autopilotAIApi.getImageLabels(token, connectionInfo).execute()
            if (response.isSuccessful) {
                response.body()?.image_labels
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    fun setImageLabels(token: String, connectionInfo: ConnectionInfo): String? {
        if (connectionInfo == null) {
            return null
        }

        return try {
            val response = autopilotAIApi.setImageLabels(token, connectionInfo).execute()
            if (response.isSuccessful) {
                response.body()?.data
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

        @POST("/ifttt/v1/user_token")
        fun getIFTTTUserToken(@Header("Authorization") token: String, @Body userInfo: UserInfo): Call<Token>

        //@GET("/ifttt/v1/image_labels")
        @HTTP(method = "get", path = "/ifttt/v1/image_labels", hasBody = true)
        fun getImageLabels(@Header("Authorization") token: String, @Body connectionInfo: ConnectionInfo): Call<Labels>

        @POST("/ifttt/v1/image_labels")
        fun setImageLabels(@Header("Authorization") token: String, @Body connectionInfo: ConnectionInfo): Call<SetImageResult>
    }

    data class ImageInfo(
        @JsonProperty("id") val id: UUID,
        @JsonProperty("account") val account: String,
        @JsonProperty("description") val description: String
    )

    data class UserInfo(
        @JsonProperty("user_id") val user_id: String?,
    )

    data class Token (
        @JsonProperty("user_token") val user_token: String?
    )

    data class ConnectionInfo(
        @JsonProperty("user_id") val user_id: String?,
        @JsonProperty("connection_id") val connection_id: String,
        @JsonProperty("image_labels") val image_labels: String
    )

    data class Labels (
        @JsonProperty("image_labels") val image_labels: String?
    )

    data class SetImageResult (
        @JsonProperty("data") val data: String?
    )
}
