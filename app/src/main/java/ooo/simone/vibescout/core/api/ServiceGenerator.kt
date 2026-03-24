package ooo.simone.vibescout.core.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ooo.simone.vibescout.core.defaultApiBaseUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ServiceGenerator {
    companion object {
        private val httpClient get() = OkHttpClient.Builder()
        private val builder: Retrofit.Builder
            get() {
                return Retrofit.Builder()
            }

        fun <S> createService(serviceClass: Class<S>, baseUrl: String = defaultApiBaseUrl): S {
            /* Add Logging Interceptor */
            val httpClient = httpClient.addInterceptor(HttpLoggingInterceptor())
            /* Set Last Builder Params */
            val builder = builder.apply {
                this.baseUrl(baseUrl)
                this.addConverterFactory(GsonConverterFactory.create())
                this.client(httpClient.build())
            }
            /* Build Api Service Class */
            return builder.build().create(serviceClass)
        }
    }
}