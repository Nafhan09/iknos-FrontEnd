package com.example.iknos.network;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

//
public class RetrofitClient {
    private static Retrofit retrofit = null;
    // SESUAIKAN IP TERGANTUNG RUNNING DEVICES
    private static final String BASE_URL = "https://iknos-be.onrender.com/api/";

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // Interceptor berfungsi untuk memasukkan Token JWT secara otomatis
            OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    SharedPreferences pref = context.getSharedPreferences("IknosPref", Context.MODE_PRIVATE);
                    String token = pref.getString("JWT_TOKEN", "");

                    Request.Builder builder = chain.request().newBuilder();
                    if (!token.isEmpty()) {
                        builder.addHeader("Authorization", "Bearer " + token);
                    }
                    return chain.proceed(builder.build());
                }
            }).build();

            // GsonConverterFactory berfungsi untuk mengubah format JSON dari server menjadi objek Java secara otomatis.
            retrofit = new Retrofit.Builder().baseUrl(BASE_URL).client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build();
        }
        return retrofit;
    }
}