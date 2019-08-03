package com.aot.httpBuild;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.aot.httpBuild.common.http.ApiService;
import com.aot.httpBuild.common.http.DaggerHttpComponent;
import com.aot.httpBuild.common.http.HttpModule;

import javax.inject.Inject;


/**
 * Create by yHai on 2019-08-02
 * EXPLAIN:
 */
public class App extends Application {
    @Inject
    ApiService apiService;

    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        DaggerHttpComponent
                .builder()
                .httpModule(new HttpModule(this))
                .build()
                .inject(this);


        Log.i("apiService", "is null = " + (apiService == null));
    }


}
