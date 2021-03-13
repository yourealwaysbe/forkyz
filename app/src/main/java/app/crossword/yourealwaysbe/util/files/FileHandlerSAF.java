
package app.crossword.yourealwaysbe.util.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract;
import androidx.preference.PreferenceManager;

public class FileHandlerSAF extends FileHandler {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerSAF.class.getCanonicalName());

    private static final String SAF_BASE_URI_PREF
        = "safBaseUri";
    private static final String SAF_CROSSWORDS_URI_PREF
        = "safCrosswordsFolderUri";
    private static final String SAF_ARCHIVE_URI_PREF
        = "safArchiveFolderUri";
    private static final String SAF_TEMP_URI_PREF
        = "safTempFolderUri";

    private static final String ARCHIVE_NAME = "archive";
    private static final String TEMP_NAME = "temp";

    private Uri crosswordsFolderUri;
    private Uri archiveFolderUri;
    private Uri tempFolderUri;

    /**
     * Construct FileHandler from context and folder URIs
     *
     * Context should be an application context, not an activity that
     * may go out of date.
     */
    public FileHandlerSAF(
        Context context,
        Uri crosswordsFolderUri,
        Uri archiveFolderUri,
        Uri tempFolderUri
    ) {
        this.crosswordsFolderUri = crosswordsFolderUri;
        this.archiveFolderUri = archiveFolderUri;
        this.tempFolderUri = tempFolderUri;
    }

    // TODO
    @Override
    public DirHandle getCrosswordsDirectory() { return null; }

    // TODO
    @Override
    public DirHandle getArchiveDirectory() { return null; }

    // TODO
    @Override
    public DirHandle getTempDirectory() { return null; }

    // TODO
    @Override
    public DirHandle getDirHandle(Uri uri){ return null; }

    // TODO
    @Override
    public FileHandle getFileHandle(Uri uri) { return null; }

    // TODO
    @Override
    public boolean exists(DirHandle dir) { return false; }

    // TODO
    @Override
    public boolean exists(FileHandle file) { return false; }

    // TODO
    @Override
    public Iterable<FileHandle> listFiles(final DirHandle dir) { return null; }

    // TODO
    @Override
    public Uri getUri(DirHandle f) { return null; }

    // TODO
    @Override
    public Uri getUri(FileHandle f) { return null; }

    // TODO
    @Override
    public String getName(FileHandle f) { return null; }

    // TODO
    @Override
    public long getLastModified(FileHandle file) { return 0L; }

    // TODO
    @Override
    public void delete(FileHandle fileHandle) { }

    // TODO
    @Override
    public void moveTo(FileHandle fileHandle, DirHandle dirHandle) { }

    // TODO
    @Override
    public void renameTo(FileHandle src, FileHandle dest) { }

    // TODO
    @Override
    public OutputStream getOutputStream(FileHandle fileHandle)
        throws IOException {
        return null;
    }

    // TODO
    @Override
    public InputStream getInputStream(FileHandle fileHandle)
        throws IOException {
        return null;
    }

    // TODO
    @Override
    public boolean isStorageMounted() { return false; }

    // TODO
    @Override
    public boolean isStorageFull() { return false; }

    // TODO
    @Override
    public FileHandle createFileHandle(DirHandle dir, String fileName) {
        return null;
    }

    /**
     * Initialise a crosswords directory in the give baseUri
     *
     * This will search the contents of baseUri to see if directories
     * already exist (and use them if so). If not, it will create the
     * required folders.
     *
     * Once this has been called, then readHandlerFromPrefs can be used
     * to retrieve a handler using these directories.
     *
     * @return false if something went wrong
     */
    public static boolean initialiseSAFPrefs(
        Context context, Uri baseUri
    ) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);
        ContentResolver resolver = context.getContentResolver();
        String baseTreeId = DocumentsContract.getTreeDocumentId(baseUri);

        Uri crosswordsFolderUri = baseUri;
        Uri archiveFolderUri = null;
        Uri tempFolderUri = null;

        // first iterate over directory looking for subdirs with the
        // right name.

        Uri childrenUri
            = DocumentsContract.buildChildDocumentsUriUsingTree(
                baseUri, baseTreeId
            );

        Cursor cursor = resolver.query(
            childrenUri,
            new String[] {
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
            },
            null, null, null
        );

        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            String mimeType = cursor.getString(1);
            String id = cursor.getString(2);

            if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                if (ARCHIVE_NAME.equals(name)) {
                    archiveFolderUri
                        = DocumentsContract.buildDocumentUriUsingTree(
                            baseUri, id
                        );
                } else if (TEMP_NAME.equals(name)) {
                    tempFolderUri
                        = DocumentsContract.buildDocumentUriUsingTree(
                            baseUri, id
                        );
                }
            }
        }

        // if not found, create new

        try {
            Uri baseTreeUri = DocumentsContract.buildDocumentUriUsingTree(
                baseUri, baseTreeId
            );

            if (archiveFolderUri == null) {
                archiveFolderUri = DocumentsContract.createDocument(
                    resolver, baseTreeUri, Document.MIME_TYPE_DIR, ARCHIVE_NAME
                );
            }

            if (tempFolderUri == null) {
                tempFolderUri = DocumentsContract.createDocument(
                    resolver, baseTreeUri, Document.MIME_TYPE_DIR, TEMP_NAME
                );
            }
        } catch (FileNotFoundException e) {
            // fall through -- will be caught by null Uris below
        }

        // if all ok, save to prefs and keep permission
        if (crosswordsFolderUri != null
                && archiveFolderUri != null
                && tempFolderUri != null) {

            // persist permissions
            final int takeFlags = (
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
            resolver.takePersistableUriPermission(baseUri, takeFlags);

            // save locations
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(
                SAF_BASE_URI_PREF, baseUri.toString()
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

            return true;
        } else {
            return false;
        }
    }

    /**
     * Read handler using locations stored in shared prefs.
     *
     * Requires initialiseSAFPrefs to have been called first. Returns
     * null if the handler could not be created. (E.g. if there are no
     * configured directories.)
     */
    public static FileHandlerSAF readHandlerFromPrefs(Context context) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);

        Uri crosswordsFolderUri
            = Uri.parse(prefs.getString(SAF_CROSSWORDS_URI_PREF, null));
        Uri archiveFolderUri
            = Uri.parse(prefs.getString(SAF_ARCHIVE_URI_PREF, null));
        Uri tempFolderUri
            = Uri.parse(prefs.getString(SAF_TEMP_URI_PREF, null));

        if (crosswordsFolderUri != null
                && archiveFolderUri != null
                && tempFolderUri != null) {
            return new FileHandlerSAF(
                context, crosswordsFolderUri, archiveFolderUri, tempFolderUri
            );
        } else {
            return null;
        }
    }
}
