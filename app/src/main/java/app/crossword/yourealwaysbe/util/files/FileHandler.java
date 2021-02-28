
package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Activity;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;

/**
 * Abstraction layer for file operations
 */
@SuppressWarnings("deprecation")
public class FileHandler {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandler.class.getCanonicalName());

    private Activity activity;

    public FileHandler(Activity activity) {
        this.activity = activity;
    }

    public DirHandle getCrosswordsDirectory() {
        return new DirHandle(
            new File(Environment.getExternalStorageDirectory(), "crosswords")
        );
    }

    public DirHandle getArchiveDirectory() {
        return new DirHandle(
            new File(
                Environment.getExternalStorageDirectory(),
                "crosswords/archive"
            )
        );
    }

    public FileHandle getFileHandle(Uri uri) {
        return new FileHandle(new File(uri.getPath()));
    }

    public boolean isStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState()
        );
    }

    public boolean isStorageFull() {
        StatFs stats = new StatFs(
            Environment.getExternalStorageDirectory().getAbsolutePath()
        );

        return (
            stats.getAvailableBlocksLong() * stats.getBlockSizeLong()
                < 1024L * 1024L
        );
    }
}
