
package app.crossword.yourealwaysbe.util.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

/**
 * Abstraction layer for file operations
 */
@SuppressWarnings("deprecation")
public class FileHandler {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandler.class.getCanonicalName());

    private static File TEMP_FOLDER;
    static {
        try {
            TEMP_FOLDER = new File(System.getProperty("java.io.tmpdir", "tmp"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileHandler() { }

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

    public boolean exists(FileHandle file) {
        return file.getFile().exists();
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
        File metaFile = fileHandle.getMetaFile();
        file.delete();
        metaFile.delete();
    }

    public void moveTo(FileHandle fileHandle, DirHandle dirHandle){
        File file = fileHandle.getFile();
        File directory = dirHandle.getFile();
        File metaFile = fileHandle.getMetaFile();
        file.renameTo(new File(directory, file.getName()));
        metaFile.renameTo(new File(directory, metaFile.getName()));
    }

    public OutputStream getOutputStream(FileHandle fileHandle)
            throws IOException {
        return new FileOutputStream(fileHandle.getFile());
    }

    public static Puzzle load(FileHandle fileHandle) throws IOException {
        File baseFile = fileHandle.getFile();
        File metaFile = fileHandle.getMetaFile();
        FileInputStream fis = new FileInputStream(baseFile);
        Puzzle puz = IO.loadNative(new DataInputStream(fis));
        fis.close();

        if (metaFile.exists()) {
            fis = new FileInputStream(metaFile);
            IO.readCustom(puz, new DataInputStream(fis));
            fis.close();
        }

        return puz;
    }

    public static void save(Puzzle puz, FileHandle fileHandle)
            throws IOException {
        File baseFile = fileHandle.getFile();
        long incept = System.currentTimeMillis();
        File metaFile = fileHandle.getMetaFile();

        File puztemp = new File(TEMP_FOLDER, baseFile.getName());
        File metatemp = new File(TEMP_FOLDER, metaFile.getName());

        FileOutputStream puzzle = new FileOutputStream(puztemp);
        FileOutputStream meta = new FileOutputStream(metatemp);

        IO.save(puz, new DataOutputStream(puzzle), new DataOutputStream(meta));

        puztemp.renameTo(baseFile);
        metatemp.renameTo(metaFile);
        System.out.println("Save complete in "
                + (System.currentTimeMillis() - incept));
    }
}
