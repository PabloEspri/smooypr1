package com.smooy.smooypr1.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@GlideModule
public class GlideAuthModule extends AppGlideModule {

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request original = chain.request();

                SharedPreferences appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                String token = appPrefs.getString("token", "");

                if (token.isEmpty()) {
                    SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    token = userPrefs.getString("token", "");
                }

                if (!token.isEmpty()) {
                    Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", "Bearer " + token);
                    
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }

                return chain.proceed(original);
            })
            .build();

        registry.replace(GlideUrl.class, InputStream.class,
            new OkHttpUrlLoader.Factory((Call.Factory) client));
    }
}