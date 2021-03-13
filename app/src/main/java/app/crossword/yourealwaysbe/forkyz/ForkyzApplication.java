package app.crossword.yourealwaysbe.forkyz;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.widget.Toast;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.util.files.FileHandlerInternal;
import app.crossword.yourealwaysbe.util.files.FileHandlerLegacy;
import app.crossword.yourealwaysbe.util.files.FileHandlerSAF;
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.CookieJar;

public class ForkyzApplication extends Application {
    private static final Logger LOGGER
        = Logger.getLogger(ForkyzApplication.class.getCanonicalName());

    public static final String PUZZLE_DOWNLOAD_CHANNEL_ID = "forkyz.downloads";
    public static final String STORAGE_LOC_PREF = "storageLocation";

    private static ForkyzApplication INSTANCE;
    private Playboard board;
    private PuzHandle puzHandle;
    private PlayboardRenderer renderer;
    private SharedPreferences settings;
    private AtomicReference<PersistentCookieJar> cookieJar = new AtomicReference<>(null);

    private FileHandler fileHandler;

    private OnSharedPreferenceChangeListener prefChangeListener
        = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(
                SharedPreferences prefs, String key
            ) {
                if (STORAGE_LOC_PREF.equals(key)
                        || FileHandlerSAF.SAF_ROOT_URI_PREF.equals(key)) {
                    Toast t = Toast.makeText(
                        ForkyzApplication.this,
                        R.string.storage_changed_please_restart,
                        Toast.LENGTH_LONG
                    );
                    t.show();
                    ForkyzApplication.this.setFileHander();
                }
            }
        };

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    /**
     * Set the board and base file of the puzzle loaded on it
     */
    public void setBoard(Playboard board, PuzHandle puzHandle){
        this.board = board;
        this.puzHandle = puzHandle;
    }

    public Playboard getBoard() {
         return board;
    }

    public PuzHandle getPuzHandle() {
        return puzHandle;
    }

    public void saveBoard() throws IOException {
        PuzHandle puzHandle = getPuzHandle();
        if (puzHandle == null)
            throw new IOException("No puz handle to save puzzle to.");

        Playboard board = getBoard();
        if (board == null)
            throw new IOException("No board to save.");

        Puzzle puz = board.getPuzzle();
        if (puz == null)
            throw new IOException("No puzzle associated to the board to save.");

        getFileHandler().save(puz, puzHandle);
    }

    public void setRenderer(PlayboardRenderer renderer){
        this.renderer = renderer;
    }

    public PlayboardRenderer getRenderer() {
        return renderer;
    }

    @Override
    public void onCreate() {
        INSTANCE = this;
        // Initialize credentials and service object.
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        setFileHander();

        settings.registerOnSharedPreferenceChangeListener(prefChangeListener);

        super.onCreate();

        AndroidVersionUtils.Factory.getInstance().createNotificationChannel(this);
    }

    public static boolean isLandscape(DisplayMetrics metrics){
        return metrics.widthPixels > metrics.heightPixels;
    }

    public static boolean isTabletish(DisplayMetrics metrics) {
        if(android.os.Build.VERSION.SDK_INT > 12) {

            double x = Math.pow(metrics.widthPixels / metrics.xdpi, 2);
            double y = Math.pow(metrics.heightPixels / metrics.ydpi, 2);
            double screenInches = Math.sqrt(x + y);
            if (screenInches > 9) { // look for a 9" or larger screen.
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isMiniTabletish(DisplayMetrics metrics) {
        switch (android.os.Build.VERSION.SDK_INT) {
        case 12:
        case 11:
        case 13:
        case 14:
        case 15:
        case 16:
            double x = Math.pow(metrics.widthPixels/metrics.xdpi,2);
            double y = Math.pow(metrics.heightPixels/metrics.ydpi,2);
            double screenInches = Math.sqrt(x+y);
            if (screenInches > 5.5 && screenInches <= 9) {
                return true;
            } else {
                return false;
            }
        default:
            return false;
        }
    }

    public SharedPreferences getSettings() {
        return settings;
    }

    public CookieJar getCookieJar(){
        if(this.cookieJar.get() == null){
            this.cookieJar.compareAndSet(null,
            new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(this)));
        }
        return this.cookieJar.get();
    }

    public static ForkyzApplication getInstance(){
        return INSTANCE;
    }

    private void setFileHander() {
        String locPref
            = settings.getString(
                STORAGE_LOC_PREF, getString(R.string.internal_storage)
            );

        if (locPref.equals(getString(R.string.external_storage_legacy))) {
            fileHandler = new FileHandlerLegacy();
        } else if (locPref.equals(getString(R.string.external_storage_saf))) {
            fileHandler = FileHandlerSAF.readHandlerFromPrefs(this);
        } else {
            fileHandler = new FileHandlerInternal(this);
        }

        if (fileHandler == null || !fileHandler.isStorageMounted()) {
            fileHandler = new FileHandlerInternal(this);
            Toast t = Toast.makeText(
                ForkyzApplication.this,
                R.string.storage_problem_falling_back_internal,
                Toast.LENGTH_LONG
            );
            t.show();
        }

        if (fileHandler.isStorageFull()) {
            Toast t = Toast.makeText(
                ForkyzApplication.this,
                R.string.storage_full_warning,
                Toast.LENGTH_LONG
            );
            t.show();
        }
    }
}
