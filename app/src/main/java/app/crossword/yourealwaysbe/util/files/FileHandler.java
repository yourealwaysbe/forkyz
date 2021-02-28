
package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Activity;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

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

    public boolean exists(DirHandle dir) {
        return dir.getFile().exists();
    }

    public int numFiles(DirHandle dir) {
        return dir.getFile().list().length;
    }

    public FileHandle[] getPuzFiles(DirHandle dir) {
        return getPuzFiles(dir, null);
    }

    /**
     * Get puz files in directory matching a source
     *
     * Matches any source if sourceMatch is null
     */
    public FileHandle[] getPuzFiles(DirHandle dirHandle, String sourceMatch) {
        File dir = dirHandle.getFile();
        ArrayList<FileHandle> files = new ArrayList<FileHandle>();
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".puz")) {
                PuzzleMeta m = null;

                try {
                    m = IO.meta(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                FileHandle h = new FileHandle(f, m);

                if ((sourceMatch == null) || sourceMatch.equals(h.getSource())) {
                    files.add(h);
                }
            }
        }

        return files.toArray(new FileHandle[files.size()]);
    }

    public FileHandle getFileHandle(DirHandle dir, String fileName) {
        return new FileHandle(new File(dir.getFile(), fileName));
    }

    public void reloadMeta(FileHandle fileHandle) throws IOException {
        fileHandle.setMeta(IO.meta(fileHandle.getFile()));
    }

    public void delete(FileHandle fileHandle){
        File file = fileHandle.getFile();
        File metaFile = new File(file.getParentFile(), file.getName().substring(0, file.getName().lastIndexOf(".")) + ".forkyz");
        file.delete();
        metaFile.delete();
    }

    public void moveTo(FileHandle fileHandle, DirHandle dirHandle){
        File file = fileHandle.getFile();
        File directory = dirHandle.getFile();
        File metaFile = new File(file.getParentFile(), file.getName().substring(0, file.getName().lastIndexOf(".")) + ".forkyz");
        file.renameTo(new File(directory, file.getName()));
        metaFile.renameTo(new File(directory, metaFile.getName()));
    }

    public OutputStream getOutputStream(FileHandle fileHandle)
            throws IOException {
        return new FileOutputStream(fileHandle.getFile());
    }
}
