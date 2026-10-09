package com.ezcast.screen.mirroring.totv.Activities.Splash;

import static com.ezcast.screen.mirroring.totv.ExtentionKt.edgeToEdge;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;

import com.ezcast.screen.mirroring.totv.Activities.LanguageActivity;
import com.ezcast.screen.mirroring.totv.R;
import com.ezcast.screen.mirroring.totv.Activities.ThemeActivity;
import com.ezcast.screen.mirroring.totv.main.SharePrefs;


public class SplashActivity extends ThemeActivity {

    private static final long SPLASH_DELAY_MS = 4000L;

    Handler handlerConfiguration;
    SharePrefs prefs;
    private ProgressBar splashProgress;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        this.prefs = new SharePrefs(this);
        edgeToEdge(this, findViewById(R.id.mainLayout));

        // Bind the inline ProgressBar
        splashProgress = findViewById(R.id.splash_progress);
        showProgress(true);

        // Move to next screen after delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showProgress(false);
                SplashActivity.this.SendUserToMainActvity();
            }
        }, SPLASH_DELAY_MS);
    }

    private void showProgress(boolean show) {
        if (splashProgress != null) {
            splashProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        // No dialog to dismiss — nothing to clean up here
        super.onDestroy();
    }

    public void SendUserToMainActvity() {
        if (this.prefs.getIsFirstTime()) {
            this.prefs.setIsFirstTime(false);
        }
        startActivity(new Intent(this, LanguageActivity.class));
        finish();
    }
}