
package app.crossword.yourealwaysbe.util.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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

    public FileHandler() { }

    public DirHandle getCrosswordsDirectory() {
        File cwDir =
            new File(Environment.getExternalStorageDirectory(), "crosswords");
        cwDir.mkdirs();
        return new DirHandle(cwDir);
    }

    public DirHandle getArchiveDirectory() {
        File arDir = new File(
            Environment.getExternalStorageDirectory(),
            "crosswords/archive"
        );
        arDir.mkdirs();
        return new DirHandle(arDir);
    }

    public DirHandle getTempDirectory(DirHandle baseDir) {
        File tempDir = new File(baseDir.getFile(), "temp");
        tempDir.mkdirs();
        return new DirHandle(tempDir);
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

    public Uri getUri(FileHandle f) {
        return Uri.fromFile(f.getFile());
    }

    public String getName(FileHandle f) {
        return f.getFile().getName();
    }

    public long getLastModified(FileHandle file) {
        return file.getFile().lastModified();
    }

    public PuzMetaFile[] getPuzFiles(DirHandle dir) {
        return getPuzFiles(dir, null);
    }

    /**
     * Get puz files in directory matching a source
     *
     * Matches any source if sourceMatch is null
     */
    public PuzMetaFile[] getPuzFiles(DirHandle dirHandle, String sourceMatch) {
        File dir = dirHandle.getFile();
        ArrayList<PuzMetaFile> files = new ArrayList<>();
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".puz")) {
                PuzzleMeta m = null;

                try {
                    File mf = getMetaFile(f);
                    if (mf.exists()) {
                        try (
                            DataInputStream is
                                = new DataInputStream(new FileInputStream(mf))
                        ) {
                            m = IO.readMeta(is);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                PuzMetaFile h = new PuzMetaFile(new FileHandle(f), m);

                if ((sourceMatch == null) || sourceMatch.equals(h.getSource())) {
                    files.add(h);
                }
            }
        }

        return files.toArray(new PuzMetaFile[files.size()]);
    }

    public FileHandle getFileHandle(DirHandle dir, String fileName) {
        return new FileHandle(new File(dir.getFile(), fileName));
    }

    public void reloadMeta(PuzMetaFile fileHandle) throws IOException {
        try (
            DataInputStream is
                = new DataInputStream(
                    new FileInputStream(getMetaFile(fileHandle))
                )
        ) {
            fileHandle.setMeta(IO.readMeta(is));
        };
    }

    public void delete(FileHandle fileHandle){
        fileHandle.getFile().delete();
    }

    public void delete(PuzMetaFile pm) {
        delete(pm.getFileHandle());
        File metaFile = getMetaFile(pm);
        if (metaFile.exists())
            metaFile.delete();
    }

    public void moveTo(FileHandle fileHandle, DirHandle dirHandle){
        File file = fileHandle.getFile();
        File directory = dirHandle.getFile();
        file.renameTo(new File(directory, file.getName()));
    }

    public void moveTo(PuzMetaFile pm, DirHandle dirHandle){
        moveTo(pm.getFileHandle(), dirHandle);
        File directory = dirHandle.getFile();
        File metaFile = getMetaFile(pm);
        if (metaFile.exists())
            metaFile.renameTo(new File(directory, metaFile.getName()));
    }

    public void renameTo(FileHandle src, FileHandle dest) {
        src.getFile().renameTo(dest.getFile());
    }

    public OutputStream getOutputStream(FileHandle fileHandle)
            throws IOException {
        return new FileOutputStream(fileHandle.getFile());
    }

    public InputStream getInputStream(FileHandle fileHandle)
            throws IOException {
        return new FileInputStream(fileHandle.getFile());
    }

    public LocalDate getModifiedDate(FileHandle file) {
        return Instant.ofEpochMilli(file.getFile().lastModified())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
    }

    public Puzzle load(PuzMetaFile puzMeta) throws IOException {
        return load(puzMeta.getFileHandle());
    }

    public Puzzle load(FileHandle fileHandle) throws IOException {
        File baseFile = fileHandle.getFile();
        File metaFile = getMetaFile(baseFile);
        try (
            DataInputStream fis
                = new DataInputStream(new FileInputStream(baseFile))
        ) {
            Puzzle puz = IO.loadNative(fis);
            if (metaFile.exists()) {
                try (
                    DataInputStream mis
                        = new DataInputStream(new FileInputStream(metaFile))
                ) {
                    IO.readCustom(puz, mis);
                }
            }
            return puz;
        }
    }

    public void save(Puzzle puz, PuzMetaFile puzMeta) throws IOException {
        save(puz, puzMeta.getFileHandle());
    }

    public void save(Puzzle puz, FileHandle fileHandle) throws IOException {
        File baseFile = fileHandle.getFile();
        long incept = System.currentTimeMillis();
        File metaFile = getMetaFile(baseFile);

        File tempFolder = getSaveTempDirectory().getFile();
        File puztemp = new File(tempFolder, baseFile.getName());
        File metatemp = new File(tempFolder, metaFile.getName());

        try (
            DataOutputStream puzzle
                = new DataOutputStream(new FileOutputStream(puztemp));
            DataOutputStream meta
                = new DataOutputStream(new FileOutputStream(metatemp));
        ) {
            IO.save(puz, puzzle, meta);
        }

        puztemp.renameTo(baseFile);
        metatemp.renameTo(metaFile);
    }

    private File getMetaFile(PuzMetaFile pm) {
        return getMetaFile(pm.getFileHandle().getFile());
    }

    private File getMetaFile(File puzFile) {
        return new File(
            puzFile.getParentFile(),
            puzFile.getName().substring(0, puzFile.getName().lastIndexOf("."))
                + ".forkyz"
        );
    }

    private DirHandle getSaveTempDirectory() {
        return getTempDirectory(getCrosswordsDirectory());
    }
}
