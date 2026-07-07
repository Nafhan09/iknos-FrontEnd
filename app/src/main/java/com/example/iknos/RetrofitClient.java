package com.example.iknos;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static final String BASE_URL = "http://10.0.2.2:3000/api/";

    // Tambahkan parameter Context agar bisa membaca SharedPreferences
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {

            // Membuat interceptor untuk menyuntikkan Token JWT secara otomatis
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            SharedPreferences pref = context.getSharedPreferences("IknosPref", Context.MODE_PRIVATE);
                            String token = pref.getString("JWT_TOKEN", "");

                            Request.Builder builder = chain.request().newBuilder();
                            if (!token.isEmpty()) {
                                // Sesuai instruksi README BE
                                builder.addHeader("Authorization", "Bearer " + token);
                            }
                            return chain.proceed(builder.build());
                        }
                    }).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // Pasang okHttpClient di sini
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}