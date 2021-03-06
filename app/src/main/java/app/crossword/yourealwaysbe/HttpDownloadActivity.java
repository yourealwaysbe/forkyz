package app.crossword.yourealwaysbe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.view.StoragePermissionDialog;

public class HttpDownloadActivity extends ForkyzActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1001;

    private DirHandle crosswordsFolder
        = getFileHandler().getCrosswordsDirectory();

    /**
    * Copies the data from an InputStream object to an OutputStream object.
    *
    * @param sourceStream
    *            The input stream to be read.
    * @param destinationStream
    *            The output stream to be written to.
    * @return int value of the number of bytes copied.
    * @exception IOException
    *                from java.io calls.
    */
    public static int copyStream(InputStream sourceStream, OutputStream destinationStream)
        throws IOException {
        int bytesRead = 0;
        int totalBytes = 0;
        byte[] buffer = new byte[1024];

        while (bytesRead >= 0) {
            bytesRead = sourceStream.read(buffer, 0, buffer.length);

            if (bytesRead > 0) {
                destinationStream.write(buffer, 0, bytesRead);
            }

            totalBytes += bytesRead;
        }

        destinationStream.flush();
        destinationStream.close();

        return totalBytes;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                DialogFragment dialog = new StoragePermissionDialog();
                Bundle args = new Bundle();
                args.putInt(
                    StoragePermissionDialog.RESULT_CODE_KEY,
                    REQUEST_EXTERNAL_STORAGE
                );
                dialog.setArguments(args);
                dialog.show(
                    getSupportFragmentManager(), "StoragePermissionDialog"
                );
            } else {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_EXTERNAL_STORAGE);
            }

            return;
        }

        initializeDownload();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeDownload();
                }
        }
    }

    private void initializeDownload() {
        FileHandler fileHandler = getFileHandler();
        if (!fileHandler.isStorageMounted() || fileHandler.isStorageFull()) {
            showSDCardHelp();
            finish();

            return;
        }

        Uri u = this.getIntent()
                    .getData();
        String filename = u.toString();
        filename = filename.substring(filename.lastIndexOf('/') + 1);

        OkHttpClient client = new OkHttpClient();

        try {
            Request request = new Request.Builder()
                    .url(u.toString())
                    .build();

            Response response = client.newCall(request).execute();

            if (response.code() != 200) {
                throw new IOException("Non 200 downloading...");
            }

            InputStream is = response.body().byteStream();
            FileHandle puzFile = fileHandler.getFileHandle(
                crosswordsFolder, filename
            );

            try (
                OutputStream fos = fileHandler.getOutputStream(
                    fileHandler.getFileHandle(crosswordsFolder, filename)
                )
            ) {
                copyStream(is, fos);
            }

            Intent i = new Intent(
                Intent.ACTION_EDIT,
                fileHandler.getUri(puzFile),
                this,
                PlayActivity.class
            );
            this.startActivity(i);
        } catch (Exception e) {
            e.printStackTrace();

            Toast t = Toast.makeText(this, "Unable to download from\n" + u.toString(), Toast.LENGTH_LONG);
            t.show();
        }

        finish();
    }
}
