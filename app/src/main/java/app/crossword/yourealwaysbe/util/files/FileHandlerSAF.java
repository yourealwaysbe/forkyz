
package app.crossword.yourealwaysbe.util.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Iterable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;
import androidx.preference.PreferenceManager;

public class FileHandlerSAF extends FileHandler {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerSAF.class.getCanonicalName());

    public static final String SAF_ROOT_URI_PREF
        = "safRootUri";
    private static final String SAF_CROSSWORDS_URI_PREF
        = "safCrosswordsFolderUri";
    private static final String SAF_ARCHIVE_URI_PREF
        = "safArchiveFolderUri";
    private static final String SAF_TEMP_URI_PREF
        = "safTempFolderUri";

    private static final String ARCHIVE_NAME = "archive";
    private static final String TEMP_NAME = "temp";

    private Context context;
    private Uri rootUri;
    private Uri crosswordsFolderUri;
    private Uri archiveFolderUri;
    private Uri tempFolderUri;

    public static class Meta {
        private String name;
        private long lastModified;

        public Meta(String name, long lastModified) {
            this.name = name;
            this.lastModified = lastModified;
        }

        public String getName() { return name; }
        public long getLastModified() { return lastModified; }
    }

    /**
     * Construct FileHandler from context and folder URIs
     *
     * Context should be an application context, not an activity that
     * may go out of date.
     *
     * @param rootUri the tree the user has granted permission to
     * @param crosswordsFolderUri the document uri for the crosswords
     * folder
     * @param archiveFolderUri the document uri for the archive folder
     * @param tempFolderUri the document uri for the temp folder
     */
    public FileHandlerSAF(
        Context context,
        Uri rootUri,
        Uri crosswordsFolderUri,
        Uri archiveFolderUri,
        Uri tempFolderUri
    ) {
        this.context = context;
        this.rootUri = rootUri;
        this.crosswordsFolderUri = crosswordsFolderUri;
        this.archiveFolderUri = archiveFolderUri;
        this.tempFolderUri = tempFolderUri;
    }

    @Override
    public DirHandle getCrosswordsDirectory() {
        return new DirHandle(crosswordsFolderUri);
    }

    @Override
    public DirHandle getArchiveDirectory() {
        return new DirHandle(archiveFolderUri);
    }

    @Override
    public DirHandle getTempDirectory() {
        return new DirHandle(tempFolderUri);
    }

    @Override
    public FileHandle getFileHandle(Uri uri) {
        Meta meta = getMetaFromUri(uri);
        if (meta != null)
            return new FileHandle(uri, meta);
        else
            return null;
    }

    @Override
    public boolean exists(DirHandle dir) {
        return exists(context.getContentResolver(), dir.getUri());
    }

    @Override
    public boolean exists(FileHandle file) {
        return exists(context.getContentResolver(), file.getUri());
    }

    @Override
    public Iterable<FileHandle> listFiles(DirHandle dir) {
        ContentResolver resolver = context.getContentResolver();
        Uri dirUri = dir.getUri();
        String dirTreeId = DocumentsContract.getDocumentId(dirUri);
        Uri dirTreeUri = DocumentsContract.buildDocumentUriUsingTree(
            rootUri, dirTreeId
        );

        Uri childrenUri
            = DocumentsContract.buildChildDocumentsUriUsingTree(
                dirTreeUri, dirTreeId
            );

        ArrayList<FileHandle> files = new ArrayList<>();

        try (
            Cursor cursor = resolver.query(
                childrenUri,
                new String[] {
                    Document.COLUMN_DOCUMENT_ID,
                    Document.COLUMN_DISPLAY_NAME,
                    Document.COLUMN_LAST_MODIFIED,
                    Document.COLUMN_MIME_TYPE
                },
                null, null, null
            )
        ) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                long modified = cursor.getLong(2);
                String mimeType = cursor.getString(3);

                if (!Document.MIME_TYPE_DIR.equals(mimeType)) {
                    Uri uri = DocumentsContract.buildDocumentUriUsingTree(
                        dirUri, id
                    );

                    files.add(new FileHandle(uri, new Meta(name, modified)));
                }
            }
        }

        return files;
    }

    @Override
    public Uri getUri(DirHandle f) { return f.getUri(); }

    @Override
    public Uri getUri(FileHandle f) { return f.getUri(); }

    @Override
    public String getName(FileHandle f) {
        return f.getSAFMeta().getName();
    }

    @Override
    public long getLastModified(FileHandle file) {
        return file.getSAFMeta().getLastModified();
    }

    @Override
    public void delete(FileHandle fileHandle) {
        try {
            DocumentsContract.deleteDocument(
                context.getContentResolver(),
                fileHandle.getUri()
            );
        } catch (FileNotFoundException e) {
            // seems like our work is done
        }
    }

    @Override
    public void moveTo(
        FileHandle fileHandle, DirHandle srcDirHandle, DirHandle destDirHandle
    ) {
        try {
            DocumentsContract.moveDocument(
                context.getContentResolver(),
                fileHandle.getUri(),
                srcDirHandle.getUri(),
                destDirHandle.getUri()
            );
        } catch (FileNotFoundException e) {
            LOGGER.severe(
                "Attempt to move " + fileHandle + " to " +
                destDirHandle + " failed."
            );
            e.printStackTrace();
        }
    }

    @Override
    public OutputStream getOutputStream(FileHandle fileHandle)
        throws IOException {
        return context.getContentResolver().openOutputStream(
            fileHandle.getUri()
        );
    }

    // TODO
    @Override
    public InputStream getInputStream(FileHandle fileHandle)
        throws IOException {
        return context.getContentResolver().openInputStream(
            fileHandle.getUri()
        );
    }

    @Override
    public boolean isStorageMounted() {
        ContentResolver resolver = context.getContentResolver();
        try {
            return exists(resolver, crosswordsFolderUri)
                && exists(resolver, archiveFolderUri)
                && exists(resolver, tempFolderUri);
        } catch (UnsupportedOperationException e) {
            LOGGER.severe("Unsupported operation accessing SAF");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isStorageFull() {
        // with thanks to https://stackoverflow.com/a/40848958
        Uri folderUri = DocumentsContract.buildDocumentUriUsingTree(
                rootUri,
                DocumentsContract.getTreeDocumentId(rootUri)
        );
        try {
            ParcelFileDescriptor pfd
                = context
                    .getContentResolver()
                    .openFileDescriptor(folderUri, "r");

            if (pfd != null) {
                StructStatVfs stats = Os.fstatvfs(pfd.getFileDescriptor());
                long availableBytes = stats.f_bavail * stats.f_bsize;
                return availableBytes < 1024L * 1024L;
            }
        } catch (FileNotFoundException | ErrnoException e) {
            LOGGER.info("Could not calculate storage available");
            e.printStackTrace();
            // fall through
        }

        // assume false if we failed to find out
        return false;
    }

    @Override
    public FileHandle createFileHandle(DirHandle dir, String fileName) {
        try {
            String mimeType = "application/octet-stream";
            if (fileName.endsWith(".puz"))
                mimeType = "application/x-crossword";
            Uri uri = DocumentsContract.createDocument(
                context.getContentResolver(), dir.getUri(), mimeType, fileName
            );
            if (uri != null) {
                return new FileHandle(
                    uri, new Meta(fileName, System.currentTimeMillis())
                );
            }
        } catch (FileNotFoundException e) {
            // fall through
        }

        return null;
    }

    /**
     * Initialise a crosswords directory in the give rootUri
     *
     * This will search the contents of rootUri to see if directories
     * already exist (and use them if so). If not, it will create the
     * required folders.
     *
     * Once this has been called, then readHandlerFromPrefs can be used
     * to retrieve a handler using these directories.
     *
     * @param context the application context
     * @param rootUri the root permitted folder for storage
     * @return an initiated file handler if successful, else null
     */
    public static FileHandlerSAF initialiseSAFPrefs(
        Context context, Uri rootUri
    ) {
        try {
            SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);
            ContentResolver resolver = context.getContentResolver();
            String dirId
                = DocumentsContract.getTreeDocumentId(rootUri);
            Uri dirUri
                = DocumentsContract.buildDocumentUriUsingTree(
                    rootUri, dirId
                );

            Uri crosswordsFolderUri = dirUri;
            Uri archiveFolderUri = null;
            Uri tempFolderUri = null;

            // first iterate over directory looking for subdirs with the
            // right name.

            Uri childrenUri
                = DocumentsContract.buildChildDocumentsUriUsingTree(
                    rootUri, dirId
                );

            try (
                Cursor cursor = resolver.query(
                    childrenUri,
                    new String[] {
                        Document.COLUMN_DISPLAY_NAME,
                        Document.COLUMN_MIME_TYPE,
                        Document.COLUMN_DOCUMENT_ID
                    },
                    null, null, null
                )
            ) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String mimeType = cursor.getString(1);
                    String id = cursor.getString(2);

                    if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                        if (ARCHIVE_NAME.equals(name)) {
                            archiveFolderUri
                                = DocumentsContract.buildDocumentUriUsingTree(
                                    rootUri, id
                                );
                        } else if (TEMP_NAME.equals(name)) {
                            tempFolderUri
                                = DocumentsContract.buildDocumentUriUsingTree(
                                    rootUri, id
                                );
                        }
                    }
                }
            }

            // if not found, create new

            if (archiveFolderUri == null) {
                archiveFolderUri = DocumentsContract.createDocument(
                    resolver, dirUri, Document.MIME_TYPE_DIR, ARCHIVE_NAME
                );
            }

            if (tempFolderUri == null) {
                tempFolderUri = DocumentsContract.createDocument(
                    resolver, dirUri, Document.MIME_TYPE_DIR, TEMP_NAME
                );
            }

            // if all ok, save to prefs and keep permission
            if (crosswordsFolderUri != null
                    && archiveFolderUri != null
                    && tempFolderUri != null) {

                // persist permissions
                int takeFlags = (
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
                resolver.takePersistableUriPermission(rootUri, takeFlags);

                // save locations
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(
                    SAF_ROOT_URI_PREF, rootUri.toString()
                );
                editor.putString(
                    SAF_CROSSWORDS_URI_PREF, crosswordsFolderUri.toString()
                );
                editor.putString(
                    SAF_ARCHIVE_URI_PREF, archiveFolderUri.toString()
                );
                editor.putString(
                    SAF_TEMP_URI_PREF, tempFolderUri.toString()
                );
                editor.apply();

                return new FileHandlerSAF(
                    context,
                    rootUri,
                    crosswordsFolderUri, archiveFolderUri, tempFolderUri
                );
            }
        } catch (Exception e) {
            LOGGER.severe("Unable to (re-)configure SAF directory.");
        }

        return null;
    }

    /**
     * Read handler using locations stored in shared prefs.
     *
     * Requires initialiseSAFPrefs to have been called first. Returns
     * null if the handler could not be created. (E.g. if there are no
     * configured directories.) Will reinitialise if the rootUri is
     * still available.
     */
    public static FileHandlerSAF readHandlerFromPrefs(Context context) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);

        FileHandlerSAF fileHandler = null;

        String rootFolder = prefs.getString(SAF_ROOT_URI_PREF, null);
        String crosswordsFolder
            = prefs.getString(SAF_CROSSWORDS_URI_PREF, null);
        String archiveFolder
            = prefs.getString(SAF_ARCHIVE_URI_PREF, null);
        String tempFolder
            = prefs.getString(SAF_TEMP_URI_PREF, null);

        if (rootFolder != null
                && crosswordsFolder != null
                && archiveFolder != null
                && tempFolder != null) {
            Uri rootFolderUri = Uri.parse(rootFolder);
            Uri crosswordsFolderUri = Uri.parse(crosswordsFolder);
            Uri archiveFolderUri = Uri.parse(archiveFolder);
            Uri tempFolderUri = Uri.parse(tempFolder);

            ContentResolver resolver = context.getContentResolver();

            try {
                if (exists(resolver, crosswordsFolderUri)
                        && exists(resolver, archiveFolderUri)
                        && exists(resolver, tempFolderUri)) {
                    fileHandler = new FileHandlerSAF(
                        context,
                        rootFolderUri,
                        crosswordsFolderUri, archiveFolderUri, tempFolderUri
                    );
                }
            } catch (SecurityException e) {
                LOGGER.severe("Permission not granted to configured SAF directories.");
            } catch (UnsupportedOperationException e) {
                LOGGER.severe("Unsupported operation with SAF");
                e.printStackTrace();
            }
        }

        if (fileHandler == null && rootFolder != null) {
            fileHandler = initialiseSAFPrefs(
                context, Uri.parse(rootFolder)
            );
        }

        return fileHandler;
    }

    private static boolean exists(ContentResolver resolver, Uri uri) {
        try (
            Cursor c = resolver.query(
                uri,
                new String[] {
                    Document.COLUMN_DOCUMENT_ID
                },
                null, null, null
            )
        ) {
            return c.getCount() > 0;
        }
    }

    private Meta getMetaFromUri(Uri uri) {
        try (
            Cursor c = context.getContentResolver().query(
                uri,
                new String[] {
                    Document.COLUMN_DISPLAY_NAME,
                    Document.COLUMN_LAST_MODIFIED
                },
                null, null, null
            )
        ) {
            if (c.getCount() > 0) {
                c.moveToNext();
                return new Meta(c.getString(0), c.getLong(1));
            } else {
                return null;
            }
        }
    }
}
