package app.crossword.yourealwaysbe.forkyz;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.CookieJar;

public class ForkyzApplication extends Application {

    public static String PUZZLE_DOWNLOAD_CHANNEL_ID = "forkyz.downloads";
    private static ForkyzApplication INSTANCE;
    private Playboard board;
    private FileHandle baseFile;
    private PlayboardRenderer renderer;
    public static File DEBUG_DIR;
    public static File CROSSWORDS = new File(
            Environment.getExternalStorageDirectory(), "crosswords");
    private SharedPreferences settings;
    private AtomicReference<PersistentCookieJar> cookieJar = new AtomicReference<>(null);

    private FileHandler fileHandler = new FileHandler();

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    /**
     * Set the board and base file of the puzzle loaded on it
     */
    public void setBoard(Playboard board, FileHandle baseFile){
        this.board = board;
        this.baseFile = baseFile;
    }

    public Playboard getBoard() {
         return board;
    }

    public FileHandle getBaseFile() {
        return baseFile;
    }

    public void saveBoard() throws IOException {
        FileHandle baseFile = getBaseFile();
        if (baseFile == null)
            throw new IOException("No base file to save puzzle to.");

        Playboard board = getBoard();
        if (board == null)
            throw new IOException("No board to save.");

        Puzzle puz = board.getPuzzle();
        if (puz == null)
            throw new IOException("No puzzle associated to the board to save.");

        IO.save(puz, baseFile);
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
        super.onCreate();

        AndroidVersionUtils.Factory.getInstance().createNotificationChannel(this);

        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {
            IO.TEMP_FOLDER = new File(CROSSWORDS, "temp");
            if(!IO.TEMP_FOLDER.mkdirs()){
                return;
            }
            DEBUG_DIR = new File(CROSSWORDS, "debug");
            if(!DEBUG_DIR.mkdirs()){
                return;
            }
            File info = new File(DEBUG_DIR, "device");
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new FileWriter(info));
                writer.println("VERSION INT: "
                        + android.os.Build.VERSION.SDK_INT);
                writer.println("VERSION RELEASE: "
                        + android.os.Build.VERSION.RELEASE);
                writer.println("MODEL: " + android.os.Build.DEVICE);
                writer.println("DISPLAY: " + android.os.Build.DISPLAY);
                writer.println("MANUFACTURER: " + android.os.Build.MANUFACTURER);
                writer.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(writer != null){
                    writer.close();
                }
            }
        }
    }


    public static void zipDir(String dir2zip, ZipOutputStream zos) {
        FileInputStream fis = null;
        try {
            File zipDir = new File(dir2zip);
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDir(filePath, zos);
                    continue;
                }
                try {
                    fis = new FileInputStream(f);

                    ZipEntry anEntry = new ZipEntry(f.getPath());
                    zos.putNextEntry(anEntry);
                    while ((bytesIn = fis.read(readBuffer)) != -1) {
                        zos.write(readBuffer, 0, bytesIn);
                    }
                } finally {
                    if(fis != null){
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
