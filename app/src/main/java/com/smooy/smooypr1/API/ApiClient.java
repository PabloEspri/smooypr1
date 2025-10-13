package com.smooy.smooypr1.API;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.smooy.smooypr1.MyApplication;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    public static final String BASE_URL = "http://212.227.147.252:8000";
    private static Retrofit retrofit = null;
    
    public static ApiService getApiService() {
        if (retrofit == null) {
            // Interceptor para logs
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // Crear el cliente HTTP con interceptor para token
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(new TokenAuthInterceptor())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
            
            // Configurar GSON
            Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .registerTypeAdapter(Date.class, new DateDeserializer())
                // Add this line to make GSON more lenient with unexpected types
                .setLenient()
                .create();
            
            // Construir Retrofit
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
            
            Log.d(TAG, "Retrofit configurado con URL base: " + BASE_URL);
        }
        
        return retrofit.create(ApiService.class);
    }
    
    /**
     * Interceptor que añade automáticamente el token a las peticiones
     */
    private static class TokenAuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            
            // No añadir token a login
            if (original.url().toString().contains("/login")) {
                Log.d(TAG, "Petición de login - No se añade token");
                return chain.proceed(original);
            }
            
            // Para otras rutas, intentar añadir el token
            Context context = MyApplication.getContext();
            if (context == null) {
                Log.e(TAG, "❌ ERROR CRÍTICO: Contexto no disponible");
                return chain.proceed(original);
            }
            
            // Buscar token en AppPrefs (principal)
            SharedPreferences appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            String token = appPrefs.getString("token", "");
            
            // Si no lo encuentra, buscar en UserPrefs (respaldo)
            if (token.isEmpty()) {
                SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                token = userPrefs.getString("token", "");
                
                // Si está en UserPrefs pero no en AppPrefs, sincronizar
                if (!token.isEmpty()) {
                    Log.d(TAG, "Sincronizando token de UserPrefs a AppPrefs");
                    appPrefs.edit().putString("token", token).apply();
                }
            }
            
            if (token.isEmpty()) {
                Log.e(TAG, "❌ No hay token disponible para: " + original.url());
                return chain.proceed(original);
            }
            
            Log.d(TAG, "✅ Token encontrado para: " + original.url());
            
            // Añadir token al header
            Request.Builder requestBuilder = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .method(original.method(), original.body());
            
            Request request = requestBuilder.build();
            return chain.proceed(request);
        }
    }
    
    /**
     * Reinicia el cliente para forzar una reconstrucción
     */
    public static void resetClient() {
        retrofit = null;
        Log.d(TAG, "Cliente API reiniciado");
    }
    
    /**
     * Deserializador para fechas 
     */
    private static class DateDeserializer implements JsonDeserializer<Date> {
        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(json.getAsString());
            } catch (ParseException e) {
                Log.e(TAG, "Error al parsear fecha", e);
                return null;
            }
        }
    }
}

