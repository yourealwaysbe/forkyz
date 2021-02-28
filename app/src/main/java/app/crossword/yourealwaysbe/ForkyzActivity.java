package app.crossword.yourealwaysbe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.util.NightModeHelper;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

import java.lang.reflect.Field;

public class ForkyzActivity extends AppCompatActivity {
    protected AndroidVersionUtils utils = AndroidVersionUtils.Factory
            .getInstance();
    protected SharedPreferences prefs;
    public NightModeHelper nightMode;

    protected FileHandler getFileHandler() {
        return ForkyzApplication.getInstance().getFileHandler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final FileHandler fileHandler = getFileHandler();

        if (!fileHandler.isStorageMounted()) {
            showSDCardHelp();
            finish();
            return;
        }

        if (fileHandler.isStorageFull()) {
            showSDCardFull();
            finish();
            return;
        }
        doOrientation();

    }

    protected void showMenuAlways(){
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        utils.restoreNightMode(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(this.nightMode == null) {
            this.nightMode = NightModeHelper.bind(this);
            this.utils.restoreNightMode(this);
        }

        if (!getFileHandler().isStorageMounted()) {
            showSDCardHelp();
            finish();
            return;
        }
        doOrientation();
    }

    protected void showSDCardFull() {
        Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("file:///android_asset/sdcard-full.html"), this,
                HTMLActivity.class);
        this.startActivity(i);
    }

    protected void showSDCardHelp() {
        Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("file:///android_asset/sdcard.html"), this,
                HTMLActivity.class);
        this.startActivity(i);
    }

    private void doOrientation() {
        try {
            if ("PORT".equals(prefs.getString("orientationLock", "UNLOCKED"))) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else if ("LAND"
                    .equals(prefs.getString("orientationLock", "UNLOCKED"))) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        } catch(RuntimeException e) {
            Toast.makeText(this, "Sorry, orientation lock is not supported without " +
                    "fullscreen mode anymore because of an Android change.", Toast.LENGTH_LONG).show();
        }
    }

    protected Bitmap createBitmap(String fontFile, String character){
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int dpi = Math.round(160F * metrics.density);
        int size = dpi / 2;
        Bitmap bitmap = Bitmap.createBitmap(size , size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.createFromAsset(getAssets(), fontFile));
        p.setTextSize(size);
        p.setAntiAlias(true);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(character, size/2, size - size / 9, p );
        return bitmap;
    }
}
