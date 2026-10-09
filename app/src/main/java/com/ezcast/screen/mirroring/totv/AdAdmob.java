package com.ezcast.screen.mirroring.totv;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.analytics.FirebaseAnalytics;

public class AdAdmob {

    // TEST IDS
//    public static String BannerAdID = "ca-app-pub-3940256099942544/9214589741";
//    public static String CollapsibleBannerAdID = "ca-app-pub-3940256099942544/2014213617";
//    public static String FullscreenAdID = "ca-app-pub-3940256099942544/1033173712";
//    public static String NativeAdID = "ca-app-pub-3940256099942544/2247696110";
//    public static String AppOpenAdID = "ca-app-pub-3940256099942544/9257395921";

//    Real ids
    public static String BannerAdID = "ca-app-pub-3940256099942544/9214589741";
    public static String CollapsibleBannerAdID = "ca-app-pub-3940256099942544/9214589741";
    public static String FullscreenAdID = "ca-app-pub-3940256099942544/1033173712";
    public static String NativeAdID = "ca-app-pub-3940256099942544/2247696110";
    public static String AppOpenAdID = "ca-app-pub-3940256099942544/9257395921";

    static ProgressDialog ProgressDialog;

    public AdAdmob(Activity activity) {

        MobileAds.initialize(activity,
                new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(
                            InitializationStatus initializationStatus) {
                    }
                });
    }

    // =========================================================
    // FIREBASE AD REVENUE
    // =========================================================

    private static void sendRevenueToFirebase(Context context,
                                              double value,
                                              String currency,
                                              String adFormat) {

        Bundle bundle = new Bundle();

        bundle.putDouble("value", value);
        bundle.putString("currency", currency);
        bundle.putString("ad_platform", "admob");
        bundle.putString("ad_source", "admob");
        bundle.putString("ad_format", adFormat);

        FirebaseAnalytics.getInstance(context)
                .logEvent("ad_impression", bundle);

        Log.d("AdRevenue",
                adFormat + " Revenue: " + value + " " + currency);
    }

    // =========================================================
    // APP OPEN AD
    // =========================================================

    public static class AppOpenAdManager
            implements Application.ActivityLifecycleCallbacks,
            DefaultLifecycleObserver {

        public interface SplashActivity {
        }

        private static final String TAG = "AppOpenAdManager";
        private static final long AD_EXPIRY_MS =
                4 * 60 * 60 * 1000L;

        private AppOpenAd appOpenAd = null;
        private long loadTimeMs = 0L;
        private boolean isShowingAd = false;
        private boolean isLoadingAd = false;
        private Activity currentActivity = null;

        private final Application application;

        private AppOpenAdManager(Application application) {

            this.application = application;

            application.registerActivityLifecycleCallbacks(this);

            ProcessLifecycleOwner.get()
                    .getLifecycle()
                    .addObserver(this);
        }

        public static AppOpenAdManager init(
                Application application) {

            return new AppOpenAdManager(application);
        }

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {

            if (currentActivity instanceof SplashActivity) {
                return;
            }

            showAdIfAvailable();
        }

        private void loadAd() {

            if (isLoadingAd || isAdAvailable()) {
                return;
            }

            isLoadingAd = true;

            AppOpenAd.load(
                    application,
                    AppOpenAdID,
                    new AdRequest.Builder().build(),

                    new AppOpenAd.AppOpenAdLoadCallback() {

                        @Override
                        public void onAdLoaded(
                                @NonNull AppOpenAd ad) {

                            appOpenAd = ad;
                            loadTimeMs =
                                    System.currentTimeMillis();

                            isLoadingAd = false;

                            ad.setOnPaidEventListener(
                                    new OnPaidEventListener() {
                                        @Override
                                        public void onPaidEvent(
                                                @NonNull AdValue adValue) {

                                            double revenue =
                                                    adValue.getValueMicros()
                                                            / 1000000.0;

                                            String currency =
                                                    adValue.getCurrencyCode();

                                            sendRevenueToFirebase(
                                                    application,
                                                    revenue,
                                                    currency,
                                                    "app_open");
                                        }
                                    });

                            Log.d(TAG,
                                    "App Open Ad Loaded");
                        }

                        @Override
                        public void onAdFailedToLoad(
                                @NonNull LoadAdError error) {

                            isLoadingAd = false;

                            Log.e(TAG,
                                    error.getMessage());
                        }
                    });
        }

        private void showAdIfAvailable() {

            if (isShowingAd) {
                return;
            }

            if (!isAdAvailable()) {
                loadAd();
                return;
            }

            appOpenAd.setFullScreenContentCallback(
                    new FullScreenContentCallback() {

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isShowingAd = true;
                        }

                        @Override
                        public void onAdDismissedFullScreenContent() {

                            isShowingAd = false;
                            appOpenAd = null;

                            loadAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(
                                @NonNull AdError adError) {

                            isShowingAd = false;
                            appOpenAd = null;

                            loadAd();
                        }
                    });

            if (currentActivity != null) {
                appOpenAd.show(currentActivity);
            }
        }

        private boolean isAdAvailable() {

            return appOpenAd != null
                    && (System.currentTimeMillis()
                    - loadTimeMs)
                    < AD_EXPIRY_MS;
        }

        @Override
        public void onActivityStarted(
                @NonNull Activity activity) {

            currentActivity = activity;
        }

        @Override
        public void onActivityResumed(
                @NonNull Activity activity) {

            currentActivity = activity;
        }

        @Override
        public void onActivityCreated(
                @NonNull Activity activity,
                Bundle bundle) {
        }

        @Override
        public void onActivityPaused(
                @NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(
                @NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(
                @NonNull Activity activity,
                @NonNull Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(
                @NonNull Activity activity) {

            if (currentActivity == activity) {
                currentActivity = null;
            }
        }
    }

    // =========================================================
    // BANNER AD
    // =========================================================

    public void BannerAd(final RelativeLayout Ad_Layout,
                         Activity activity) {

        AdView mAdView = new AdView(activity);

        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdUnitId(BannerAdID);

        mAdView.setOnPaidEventListener(
                new OnPaidEventListener() {
                    @Override
                    public void onPaidEvent(
                            @NonNull AdValue adValue) {

                        double revenue =
                                adValue.getValueMicros()
                                        / 1000000.0;

                        String currency =
                                adValue.getCurrencyCode();

                        sendRevenueToFirebase(
                                activity,
                                revenue,
                                currency,
                                "banner");
                    }
                });

        mAdView.loadAd(new AdRequest.Builder().build());

        Ad_Layout.addView(mAdView);

        mAdView.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                Ad_Layout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(
                    @NonNull LoadAdError loadAdError) {

                mAdView.destroy();

                Ad_Layout.setVisibility(View.GONE);
            }
        });
    }

    // =========================================================
    // COLLAPSIBLE BANNER
    // =========================================================

    public void CollapsibleBannerAd(
            final RelativeLayout Ad_Layout,
            Activity activity,
            String collapseDirection) {

        AdView mAdView = new AdView(activity);

        mAdView.setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        activity,
                        getScreenWidthDp(activity)));

        mAdView.setAdUnitId(CollapsibleBannerAdID);

        Bundle extras = new Bundle();

        extras.putString(
                "collapsible",
                collapseDirection != null
                        ? collapseDirection
                        : "bottom");

        AdRequest adRequest =
                new AdRequest.Builder()
                        .addNetworkExtrasBundle(
                                AdMobAdapter.class,
                                extras)
                        .build();

        mAdView.setOnPaidEventListener(
                new OnPaidEventListener() {
                    @Override
                    public void onPaidEvent(
                            @NonNull AdValue adValue) {

                        double revenue =
                                adValue.getValueMicros()
                                        / 1000000.0;

                        String currency =
                                adValue.getCurrencyCode();

                        sendRevenueToFirebase(
                                activity,
                                revenue,
                                currency,
                                "collapsible_banner");
                    }
                });

        mAdView.loadAd(adRequest);

        Ad_Layout.addView(mAdView);
    }

    // =========================================================
    // NATIVE AD
    // =========================================================

    public void NativeAd(
            final RelativeLayout nativeAdContainer,
            final int nativeAdViewLayout,
            final Activity activity) {

        nativeAdContainer.setVisibility(View.GONE);

        AdLoader adLoader =
                new AdLoader.Builder(activity, NativeAdID)

                        .forNativeAd(
                                new NativeAd.OnNativeAdLoadedListener() {

                                    @Override
                                    public void onNativeAdLoaded(
                                            @NonNull NativeAd nativeAd) {

                                        nativeAd.setOnPaidEventListener(
                                                new OnPaidEventListener() {
                                                    @Override
                                                    public void onPaidEvent(
                                                            @NonNull AdValue adValue) {

                                                        double revenue =
                                                                adValue.getValueMicros()
                                                                        / 1000000.0;

                                                        String currency =
                                                                adValue.getCurrencyCode();

                                                        sendRevenueToFirebase(
                                                                activity,
                                                                revenue,
                                                                currency,
                                                                "native");
                                                    }
                                                });

                                        NativeAdView adView =
                                                (NativeAdView)
                                                        LayoutInflater
                                                                .from(activity)
                                                                .inflate(
                                                                        nativeAdViewLayout,
                                                                        null);

                                        populateNativeAdView(
                                                nativeAd,
                                                adView);

                                        nativeAdContainer.removeAllViews();

                                        nativeAdContainer.addView(adView);

                                        nativeAdContainer.setVisibility(
                                                View.VISIBLE);
                                    }
                                })

                        .withNativeAdOptions(
                                new NativeAdOptions.Builder()
                                        .build())

                        .build();

        adLoader.loadAd(
                new AdRequest.Builder().build());
    }

    private void populateNativeAdView(
            NativeAd nativeAd,
            NativeAdView adView) {

        TextView headlineView =
                adView.findViewById(R.id.ad_headline);

        headlineView.setText(nativeAd.getHeadline());

        adView.setHeadlineView(headlineView);

        TextView bodyView =
                adView.findViewById(R.id.ad_body);

        if (nativeAd.getBody() != null) {

            bodyView.setText(nativeAd.getBody());

            adView.setBodyView(bodyView);
        }

        ImageView iconView =
                adView.findViewById(R.id.ad_icon);

        if (nativeAd.getIcon() != null) {

            iconView.setImageDrawable(
                    nativeAd.getIcon().getDrawable());

            adView.setIconView(iconView);
        }

        Button ctaView =
                adView.findViewById(
                        R.id.ad_call_to_action);

        if (nativeAd.getCallToAction() != null) {

            ctaView.setText(
                    nativeAd.getCallToAction());

            adView.setCallToActionView(ctaView);
        }

        RatingBar ratingBar =
                adView.findViewById(R.id.ad_stars);

        if (nativeAd.getStarRating() != null) {

            ratingBar.setRating(
                    nativeAd.getStarRating()
                            .floatValue());

            adView.setStarRatingView(ratingBar);
        }

        adView.setNativeAd(nativeAd);
    }

    // =========================================================
    // INTERSTITIAL AD
    // =========================================================

    public void FullscreenAd(final Activity activity) {

        Ad_Popup(activity);

        InterstitialAd.load(
                activity,
                FullscreenAdID,
                new AdRequest.Builder().build(),

                new InterstitialAdLoadCallback() {

                    @Override
                    public void onAdLoaded(
                            @NonNull InterstitialAd interstitialAd) {

                        interstitialAd.setOnPaidEventListener(
                                new OnPaidEventListener() {
                                    @Override
                                    public void onPaidEvent(
                                            @NonNull AdValue adValue) {

                                        double revenue =
                                                adValue.getValueMicros()
                                                        / 1000000.0;

                                        String currency =
                                                adValue.getCurrencyCode();

                                        sendRevenueToFirebase(
                                                activity,
                                                revenue,
                                                currency,
                                                "interstitial");
                                    }
                                });

                        interstitialAd.show(activity);

                        ProgressDialog.dismiss();
                    }

                    @Override
                    public void onAdFailedToLoad(
                            @NonNull LoadAdError loadAdError) {

                        ProgressDialog.dismiss();
                    }
                });
    }

    // =========================================================
    // COUNTER INTERSTITIAL
    // =========================================================

    private static final String Count_Ads =
            "Count_Ads";

    public static void setCount_Ads(
            Context context,
            int value) {

        context.getSharedPreferences(
                        context.getPackageName(),
                        0)
                .edit()
                .putInt(Count_Ads, value)
                .apply();
    }

    public static int getCount_Ads(
            Context context) {

        return context.getSharedPreferences(
                        context.getPackageName(),
                        0)
                .getInt(Count_Ads, 1);
    }

    public static void FullscreenAd_Counter(
            final Activity activity) {

        int counter_ads = getCount_Ads(activity);

        if (counter_ads >= 3) {

            setCount_Ads(activity, 1);

            Ad_Popup(activity);

            InterstitialAd.load(
                    activity,
                    FullscreenAdID,
                    new AdRequest.Builder().build(),

                    new InterstitialAdLoadCallback() {

                        @Override
                        public void onAdLoaded(
                                @NonNull InterstitialAd interstitialAd) {

                            interstitialAd.setOnPaidEventListener(
                                    new OnPaidEventListener() {
                                        @Override
                                        public void onPaidEvent(
                                                @NonNull AdValue adValue) {

                                            double revenue =
                                                    adValue.getValueMicros()
                                                            / 1000000.0;

                                            String currency =
                                                    adValue.getCurrencyCode();

                                            sendRevenueToFirebase(
                                                    activity,
                                                    revenue,
                                                    currency,
                                                    "interstitial");
                                        }
                                    });

                            interstitialAd.show(activity);

                            ProgressDialog.dismiss();
                        }

                        @Override
                        public void onAdFailedToLoad(
                                @NonNull LoadAdError loadAdError) {

                            ProgressDialog.dismiss();
                        }
                    });

        } else {

            setCount_Ads(activity,
                    counter_ads + 1);
        }
    }

    // =========================================================
    // PROGRESS DIALOG
    // =========================================================

    private static void Ad_Popup(Context context) {

        ProgressDialog =
                ProgressDialog.show(
                        context,
                        "",
                        "Ad Loading . . .",
                        true);

        ProgressDialog.setCancelable(true);

        ProgressDialog.show();
    }

    // =========================================================
    // SCREEN WIDTH
    // =========================================================

    private int getScreenWidthDp(Context context) {

        return (int)
                (context.getResources()
                        .getDisplayMetrics()
                        .widthPixels
                        /
                        context.getResources()
                                .getDisplayMetrics()
                                .density);
    }
}