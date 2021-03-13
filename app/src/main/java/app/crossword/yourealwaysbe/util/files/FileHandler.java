
package app.crossword.yourealwaysbe.util.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Iterable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    public abstract DirHandle getTempDirectory();
    public abstract FileHandle getFileHandle(Uri uri);
    public abstract boolean exists(DirHandle dir);
    public abstract boolean exists(FileHandle file);
    public abstract Iterable<FileHandle> listFiles(final DirHandle dir);
    public abstract Uri getUri(DirHandle f);
    public abstract Uri getUri(FileHandle f);
    public abstract String getName(FileHandle f);
    public abstract long getLastModified(FileHandle file);
    public abstract void delete(FileHandle fileHandle);
    public abstract OutputStream getOutputStream(FileHandle fileHandle)
        throws IOException;
    public abstract InputStream getInputStream(FileHandle fileHandle)
        throws IOException;
    public abstract boolean isStorageMounted();
    public abstract boolean isStorageFull();

    /**
     * Create a new file in the directory with the given display name
     *
     * Return null if could not be created. E.g. if the file already
     * exists.
     */
    public abstract FileHandle createFileHandle(DirHandle dir, String fileName);

    /**
     * Move from srcDir to destDir
     */
    public abstract void moveTo(
        FileHandle fileHandle, DirHandle srcDirHandle, DirHandle destDirHandle
    );

    public boolean exists(PuzMetaFile pm) {
        return exists(pm.getPuzHandle());
    }

    public boolean exists(PuzHandle ph) {
        FileHandle metaHandle = ph.getMetaFileHandle();
        if (metaHandle != null) {
            return exists(ph.getPuzFileHandle())
                && exists(ph.getMetaFileHandle());
        } else {
            return exists(ph.getPuzFileHandle());
        }
    }

    public void delete(PuzMetaFile pm) {
        delete(pm.getPuzHandle());
    }

    public void delete(PuzHandle ph) {
        delete(ph.getPuzFileHandle());
        FileHandle metaHandle = ph.getMetaFileHandle();
        if (metaHandle != null)
            delete(metaHandle);
    }

    public void moveTo(
        PuzMetaFile pm, DirHandle srcDirHandle, DirHandle destDirHandle
    ) {
        moveTo(pm.getPuzHandle(), srcDirHandle, destDirHandle);
    }

    public void moveTo(
        PuzHandle ph, DirHandle srcDirHandle, DirHandle destDirHandle
    ) {
        moveTo(ph.getPuzFileHandle(), srcDirHandle, destDirHandle);
        FileHandle metaHandle = ph.getMetaFileHandle();
        if (metaHandle != null)
            moveTo(metaHandle, srcDirHandle, destDirHandle);
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

        // Use a caching approach to avoid repeated interaction with
        // filesystem (which is good for content resolver)
        Map<String, FileHandle> puzFiles = new HashMap<>();
        Map<String, FileHandle> metaFiles = new HashMap<>();

        for (FileHandle f : listFiles(dirHandle)) {
            String fileName = getName(f);
            if (fileName.endsWith(".puz")) {
                puzFiles.put(fileName, f);
            } else if (fileName.endsWith(".forkyz")) {
                metaFiles.put(fileName, f);
            } else {
            }
        }

        for (Map.Entry<String, FileHandle> entry : puzFiles.entrySet()) {
            String fileName = entry.getKey();
            FileHandle puzFile = entry.getValue();
            FileHandle metaFile = null;

            String metaName = getMetaFileName(puzFile);

            if (metaFiles.containsKey(metaName)) {
                metaFile = metaFiles.get(metaName);
            }

            PuzMetaFile pm = loadPuzMetaFile(
                new PuzHandle(dirHandle, puzFile, metaFile)
            );

            if ((sourceMatch == null) || sourceMatch.equals(pm.getSource())) {
                files.add(pm);
            }
        }

        return files.toArray(new PuzMetaFile[files.size()]);
    }

    public PuzMetaFile loadPuzMetaFile(PuzHandle puzHandle) {
        FileHandle metaHandle = puzHandle.getMetaFileHandle();
        PuzzleMeta meta = null;

        if (metaHandle != null) {
            try (
                DataInputStream is = new DataInputStream(
                    getInputStream(metaHandle)
                )
            ) {
                meta = IO.readMeta(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new PuzMetaFile(puzHandle, meta);
    }

    /**
     * Gets the set of file names in the two directories
     *
     * Slightly odd method, but useful in various places. dir1 and dir2
     * are usually the crosswords and archive folders.
     */
    public Set<String> getFileNames(DirHandle dir1, DirHandle dir2) {
        Set<String> fileNames = new HashSet<>();
        for (FileHandle fh : listFiles(dir1))
            fileNames.add(getName(fh));
        for (FileHandle fh : listFiles(dir2))
            fileNames.add(getName(fh));
        return fileNames;
    }

    public Puzzle load(PuzMetaFile pm) throws IOException {
        return load(pm.getPuzHandle());
    }

    /**
     * Loads puzzle with meta
     *
     * If the meta file of puz handle is null, loads without meta
     */
    public Puzzle load(PuzHandle ph) throws IOException {
        FileHandle metaFile = ph.getMetaFileHandle();

        if (metaFile == null)
            return load(ph.getPuzFileHandle());

        try (
            DataInputStream pis
                = new DataInputStream(
                    getInputStream(ph.getPuzFileHandle())
                );
            DataInputStream mis
                = new DataInputStream(
                    getInputStream(ph.getMetaFileHandle())
                )
        ) {
            return IO.load(pis, mis);
        }
    }

    /**
     * Loads without any meta data
     */
    public Puzzle load(FileHandle fileHandle) throws IOException {
        try (
            DataInputStream fis
                = new DataInputStream(getInputStream(fileHandle))
        ) {
            return IO.loadNative(fis);
        }
    }

    public void save(Puzzle puz, PuzMetaFile puzMeta) throws IOException {
        save(puz, puzMeta.getPuzHandle());
    }

    /**
     * Save puzzle and meta data
     *
     * If puzHandle's meta handle is null, a new meta file will be
     * created and puzHandle is updated with the new meta file handle
     */
    public void save(Puzzle puz, PuzHandle puzHandle) throws IOException {
        long incept = System.currentTimeMillis();

        FileHandle puzFile = puzHandle.getPuzFileHandle();
        FileHandle metaFile = puzHandle.getMetaFileHandle();
        DirHandle puzDir = puzHandle.getDirHandle();

        if (metaFile == null) {
            String metaName = getMetaFileName(puzFile);
            metaFile = createFileHandle(puzHandle.getDirHandle(), metaName);
            if (metaFile == null)
                throw new IOException("Could not create meta file");
        }

        try (
            DataOutputStream puzzle
                = new DataOutputStream(getOutputStream(puzFile));
            DataOutputStream meta
                = new DataOutputStream(getOutputStream(metaFile));
        ) {
            IO.save(puz, puzzle, meta);
        }

        puzHandle.setMetaFileHandle(metaFile);
    }

    /**
     * Save the puz file to the file handle and create a meta file
     *
     * Assumed that a meta file does not exist already
     *
     * @param puzDir the directory containing puzFile (and where the
     * metta will be created)
     */
    public void saveCreateMeta(Puzzle puz, DirHandle puzDir, FileHandle puzFile)
        throws IOException {
        save(puz, new PuzHandle(puzDir, puzFile, null));
    }

    public LocalDate getModifiedDate(FileHandle file) {
        return Instant.ofEpochMilli(getLastModified(file))
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
    }

    protected String getMetaFileName(FileHandle puzFile) {
        String name = getName(puzFile);
        return name.substring(0, name.lastIndexOf(".")) + ".forkyz";
    }
}
