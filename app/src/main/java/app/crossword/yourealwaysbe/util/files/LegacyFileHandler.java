
package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Iterable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

/**
 * Implementation of original Shortyz file access directly working with
 * external SD card directory.
 */
@SuppressWarnings("deprecation")
public class LegacyFileHandler extends FileHandler {
    private static final Logger LOGGER
        = Logger.getLogger(LegacyFileHandler.class.getCanonicalName());

    public LegacyFileHandler() { }

    @Override
    public DirHandle getCrosswordsDirectory() {
        File cwDir =
            new File(Environment.getExternalStorageDirectory(), "crosswords");
        cwDir.mkdirs();
        return new DirHandle(cwDir);
    }

    @Override
    public DirHandle getArchiveDirectory() {
        File arDir = new File(
            Environment.getExternalStorageDirectory(),
            "crosswords/archive"
        );
        arDir.mkdirs();
        return new DirHandle(arDir);
    }

    @Override
    public DirHandle getTempDirectory(DirHandle baseDir) {
        File tempDir = new File(baseDir.getFile(), "temp");
        tempDir.mkdirs();
        return new DirHandle(tempDir);
    }

    @Override
    public FileHandle getFileHandle(Uri uri) {
        return new FileHandle(new File(uri.getPath()));
    }

    @Override
    public boolean exists(DirHandle dir) {
        return dir.getFile().exists();
    }

    @Override
    public boolean exists(FileHandle file) {
        return file.getFile().exists();
    }

    @Override
    public int numFiles(DirHandle dir) {
        return dir.getFile().list().length;
    }

    @Override
    public Iterable<FileHandle> listFiles(final DirHandle dir) {
        return new Iterable<FileHandle>() {
            public Iterator<FileHandle> iterator() {
                return new Iterator<FileHandle>() {
                    private int pos = 0;
                    private File[] files = dir.getFile().listFiles();

                    public boolean hasNext() {
                        return files == null ? false : pos < files.length - 1;
                    }

                    public FileHandle next() {
                        return new FileHandle(files[pos++]);
                    }
                };
            }
        };
    }

    @Override
    public Uri getUri(FileHandle f) {
        return Uri.fromFile(f.getFile());
    }

    @Override
    public String getName(FileHandle f) {
        return f.getFile().getName();
    }

    @Override
    public long getLastModified(FileHandle file) {
        return file.getFile().lastModified();
    }

    @Override
    public FileHandle getFileHandle(DirHandle dir, String fileName) {
        return new FileHandle(new File(dir.getFile(), fileName));
    }

    @Override
    public void delete(FileHandle fileHandle) {
        fileHandle.getFile().delete();
    }

    @Override
    public void moveTo(FileHandle fileHandle, DirHandle dirHandle) {
        File file = fileHandle.getFile();
        File directory = dirHandle.getFile();
        file.renameTo(new File(directory, file.getName()));
    }

    @Override
    public void renameTo(FileHandle src, FileHandle dest) {
        src.getFile().renameTo(dest.getFile());
    }

    @Override
    public OutputStream getOutputStream(FileHandle fileHandle)
            throws IOException {
        return new FileOutputStream(fileHandle.getFile());
    }

    @Override
    public InputStream getInputStream(FileHandle fileHandle)
            throws IOException {
        return new FileInputStream(fileHandle.getFile());
    }

    @Override
    public LocalDate getModifiedDate(FileHandle file) {
        return Instant.ofEpochMilli(file.getFile().lastModified())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
    }

    @Override
    public boolean isStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState()
        );
    }

    @Override
    public boolean isStorageFull() {
        StatFs stats = new StatFs(
            Environment.getExternalStorageDirectory().getAbsolutePath()
        );

        return (
            stats.getAvailableBlocksLong() * stats.getBlockSizeLong()
                < 1024L * 1024L
        );
    }

    @Override
    protected FileHandle getMetaFileHandle(FileHandle puzHandle) {
        File puzFile = puzHandle.getFile();
        return new FileHandle(new File(
            puzFile.getParentFile(),
            puzFile.getName().substring(0, puzFile.getName().lastIndexOf("."))
                + ".forkyz"
        ));
    }
}
