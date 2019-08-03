package com.aot.httpBuild.common.http;

import com.aot.httpBuild.App;

import dagger.Component;

/**
 * Create by yHai on 2019-08-02
 * EXPLAIN:
 */
@Component(modules = HttpModule.class)
public  interface HttpComponent {

    void inject(App app);
}
