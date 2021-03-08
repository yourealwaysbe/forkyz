
package app.crossword.yourealwaysbe.util.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Iterable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

/**
 * Abstraction layer for file operations
 *
 * Implementations provided for different file backends
 */
public abstract class FileHandler {
    public abstract DirHandle getCrosswordsDirectory();
    public abstract DirHandle getArchiveDirectory();
    public abstract DirHandle getTempDirectory(DirHandle baseDir);
    public abstract FileHandle getFileHandle(Uri uri);
    public abstract boolean exists(DirHandle dir);
    public abstract boolean exists(FileHandle file);
    public abstract int numFiles(DirHandle dir);
    public abstract Iterable<FileHandle> listFiles(final DirHandle dir);
    public abstract Uri getUri(FileHandle f);
    public abstract String getName(FileHandle f);
    public abstract long getLastModified(FileHandle file);
    public abstract FileHandle getFileHandle(DirHandle dir, String fileName);
    public abstract void delete(FileHandle fileHandle);
    public abstract void moveTo(FileHandle fileHandle, DirHandle dirHandle);
    public abstract void renameTo(FileHandle src, FileHandle dest);
    public abstract OutputStream getOutputStream(FileHandle fileHandle)
        throws IOException;
    public abstract InputStream getInputStream(FileHandle fileHandle)
        throws IOException;
    public abstract LocalDate getModifiedDate(FileHandle file);
    public abstract boolean isStorageMounted();
    public abstract boolean isStorageFull();

    protected abstract FileHandle getMetaFileHandle(FileHandle puzFile);

    public void delete(PuzMetaFile pm) {
        delete(pm.getFileHandle());
        delete(getMetaFileHandle(pm));
    }

    public void moveTo(PuzMetaFile pm, DirHandle dirHandle){
        moveTo(pm.getFileHandle(), dirHandle);
        moveTo(getMetaFileHandle(pm), dirHandle);
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
        ArrayList<PuzMetaFile> files = new ArrayList<>();

        for (FileHandle f : listFiles(dirHandle)) {
            if (getName(f).endsWith(".puz")) {
                PuzzleMeta m = null;

                FileHandle mf = getMetaFileHandle(f);
                if (exists(mf)) {
                    try (
                        DataInputStream is = new DataInputStream(
                            getInputStream(mf)
                        )
                    ) {
                        m = IO.readMeta(is);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                PuzMetaFile h = new PuzMetaFile(f, m);

                if ((sourceMatch == null) || sourceMatch.equals(h.getSource())) {
                    files.add(h);
                }
            }
        }

        return files.toArray(new PuzMetaFile[files.size()]);
    }

    public Puzzle load(PuzMetaFile puzMeta) throws IOException {
        return load(puzMeta.getFileHandle());
    }

    public Puzzle load(FileHandle fileHandle) throws IOException {
        FileHandle metaFile = getMetaFileHandle(fileHandle);
        try (
            DataInputStream fis
                = new DataInputStream(getInputStream(fileHandle))
        ) {
            Puzzle puz = IO.loadNative(fis);
            if (exists(metaFile)) {
                try (
                    DataInputStream mis
                        = new DataInputStream(getInputStream(metaFile))
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
        long incept = System.currentTimeMillis();
        FileHandle metaFile = getMetaFileHandle(fileHandle);

        DirHandle tempFolder = getSaveTempDirectory();
        FileHandle puzTemp = getFileHandle(tempFolder, getName(fileHandle));
        FileHandle metaTemp = getFileHandle(tempFolder, getName(metaFile));

        try (
            DataOutputStream puzzle
                = new DataOutputStream(getOutputStream(puzTemp));
            DataOutputStream meta
                = new DataOutputStream(getOutputStream(metaTemp));
        ) {
            IO.save(puz, puzzle, meta);
        }

        renameTo(puzTemp, fileHandle);
        renameTo(metaTemp, metaFile);
    }

    public void reloadMeta(PuzMetaFile fileHandle) throws IOException {
        try (
            DataInputStream is
                = new DataInputStream(
                    getInputStream(getMetaFileHandle(fileHandle))
                )
        ) {
            fileHandle.setMeta(IO.readMeta(is));
        };
    }

    protected FileHandle getMetaFileHandle(PuzMetaFile pm) {
        return getMetaFileHandle(pm.getFileHandle());
    }

    protected DirHandle getSaveTempDirectory() {
        return getTempDirectory(getCrosswordsDirectory());
    }
}
