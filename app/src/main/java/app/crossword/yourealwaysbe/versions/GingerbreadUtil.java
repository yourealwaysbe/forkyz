package app.crossword.yourealwaysbe.versions;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;

import app.crossword.yourealwaysbe.ForkyzActivity;
import app.crossword.yourealwaysbe.net.DownloadReceiver;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.NightModeHelper;
import app.crossword.yourealwaysbe.util.files.DirHandle;


public class GingerbreadUtil extends DefaultUtil {
    protected Context ctx;

    public void setContext(Context ctx) {
        this.ctx = ctx;
    }

    public void finishOnHomeButton(AppCompatActivity a) {
    }

    public void holographic(AppCompatActivity playActivity) {
    }

    public void onActionBarWithText(MenuItem a) {
    }

    public void onActionBarWithText(SubMenu reveal) {
    }

    @Override
    public void restoreNightMode(ForkyzActivity forkyzActivity) {

    }

    @Override
    public void restoreNightMode(NightModeHelper nightMode) {

    }

    public void storeMetas(Uri uri, PuzzleMeta meta, DirHandle parentDir) {
        DownloadReceiver.metas.put(uri,
            new DownloadReceiver.Metas(meta, parentDir)
        );
	}

    public DownloadReceiver.Metas removeMetas(Uri uri) {
        return DownloadReceiver.metas.remove(uri);
    }

	public void hideWindowTitle(AppCompatActivity a) {
		a.requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	public void hideActionBar(AppCompatActivity a) {
		; //no op;
	}

    public void onActionBarWithoutText(MenuItem a) {
        ; //no op
    }

    public void hideTitleOnPortrait(AppCompatActivity a) {

    }

    @Override
    public boolean isBackgroundDownloadAvaliable() {
        return false;
    }

    @Override
    public boolean checkBackgroundDownload(SharedPreferences prefs, boolean hasWritePermissions) {
        return false;
    }

    @Override
    public void clearBackgroundDownload(SharedPreferences prefs) {

    }

    @Override
    public void createNotificationChannel(Context context) {

    }

    @Override
    public void nextNightMode(ForkyzActivity activity) {

    }

    @Override
    public boolean isNightModeAvailable() {
        return false;
    }

    public View onActionBarCustom(AppCompatActivity a, int id) {
        System.out.println("Setting custom ActionBar view");
        ActionBar bar = a.getSupportActionBar();
        if(bar == null){
            return null;
        }
        bar.setDisplayShowCustomEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowHomeEnabled(true);
        bar.setCustomView(id);
        return bar.getCustomView();
    }

}
