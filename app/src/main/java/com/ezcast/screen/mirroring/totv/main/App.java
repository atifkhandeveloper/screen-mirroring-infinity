package com.ezcast.screen.mirroring.totv.main;

import android.app.Application;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ezcast.screen.mirroring.totv.AdAdmob;


public class App extends Application {
    private static App app;
    public static boolean isAppPurchase = false;
    private FirebaseAnalytics mFirebaseAnalytics;

    public static App getApp() {
        return app;
    }

    @Override 
    public void onCreate() {

        super.onCreate();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        AdAdmob.AppOpenAdManager.init(this); // ← that's it
        app = this;

    }

}
