package com.aot.httpBuild.common.http;

import android.text.TextUtils;
import android.util.Log;

import com.aot.httpBuild.App;
import com.aot.httpBuild.BuildConfig;
import com.aot.httpBuild.common.constant.GlobalConstant;
import com.aot.httpBuild.common.utils.NetUtil;
import com.google.gson.Gson;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Create by yHai on 2019-08-02
 * EXPLAIN:
 */
@Module
public class HttpModule {
    private static final int DEFAULT_CONNECT_TIMEOUT = 30;
    private static final int DEFAULT_WRITE_TIMEOUT = 30;
    private static final int DEFAULT_READ_TIMEOUT = 30;
    private final App app;

    public HttpModule(App app) {
        this.app = app;
    }

    @Provides
    ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }

    @Provides
    @Singleton
    Gson provideGson() {
        return new Gson();
    }

    @Provides
    Retrofit provideRetrofit(OkHttpClient.Builder clientBuilder, String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .client(clientBuilder.build())
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        return retrofit;
    }

    @Provides
    String provideBaseUrl() {
        return GlobalConstant.BaseUrl.BASE_URL;
    }

    @Provides
    @Named("cache")
    Interceptor provideInterceptor() {
        Interceptor cacheInterceptor = chain -> {
            Request request = chain.request();
            if (!NetUtil.isNetworkConnected(app)) {
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build();
            }
            Response response = chain.proceed(request);
            if (!NetUtil.isNetworkConnected(app)) {
                int maxAge = 0;
                // 有网络时 设置缓存超时时间0个小时
                response.newBuilder()
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .removeHeader("CACHE_NAME")// 清除头信息，因为服务器如果不支持，会返回一些干扰信息，不清除下面无法生效
                        .build();
            } else {
                // 无网络时，设置超时为4周
                int maxStale = 60 * 60 * 24 * 28;
                response.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .removeHeader("CACHE_NAME")
                        .build();
            }
            return response;
        };
        return cacheInterceptor;
    }

    /**
     * 设置头信息
     */
    @Provides
    @Named("Head")
    Interceptor provideHeadInterceptor() {

        Interceptor headerInterceptor = chain -> {
            Request originalRequest = chain.request();
            Request.Builder requestBuilder = originalRequest.newBuilder()
                    .addHeader("user-agent", "http.agent")
                    .addHeader("Accept-Encoding", "gzip")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json; charset=utf-8")

                    .method(originalRequest.method(), originalRequest.body());
            Request request = requestBuilder.build();
            return chain.proceed(request);
        };
        return headerInterceptor;
    }

    /**
     * 服务端返回数据拦截
     */
    @Provides
    @Named("format")
    Interceptor provideResponseInterceptor() {

        Interceptor responseInterceptor = chain -> {
            Request request = chain.request();
            Response response = chain.proceed(request);
            String body = response.body().string();
            Log.i("YHLog", BuildConfig.DEBUG ? (request.url() + "    =    " + body) : "");
            return response;
        };
        return responseInterceptor;
    }


    @Provides
    OkHttpClient.Builder provideOkHttpClient(@Named("cache") Interceptor cacheInterceptor,
                                             @Named("Head") Interceptor headerInterceptor,
                                             @Named("format") Interceptor responseInterceptor) {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient
                .Builder();
        /**
         * 设置缓存
         */
        File cacheFile = new File(app.getApplicationContext().getExternalCacheDir(), "CACHE_NAME");
        Cache cache = new Cache(cacheFile, 1024 * 1024 * 50);
        okHttpBuilder
                .cache(cache)
                .addInterceptor(cacheInterceptor);
        okHttpBuilder.addInterceptor(headerInterceptor);
        okHttpBuilder.addInterceptor(responseInterceptor);

        /**
         * 设置超时和重新连接
         */
        okHttpBuilder.connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS);
        okHttpBuilder.readTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS);
        okHttpBuilder.writeTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS);
        //错误重连
        okHttpBuilder.retryOnConnectionFailure(true);

        return okHttpBuilder;
    }
}
