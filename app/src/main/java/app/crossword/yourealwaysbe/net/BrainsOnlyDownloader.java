package app.crossword.yourealwaysbe.net;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.BrainsOnlyIO;
import app.crossword.yourealwaysbe.io.KingFeaturesPlaintextIO;
import app.crossword.yourealwaysbe.io.UclickXMLIO;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Created by keber_000 on 2/9/14.
 */
public class BrainsOnlyDownloader extends AbstractDownloader {

    private final DateTimeFormatter df
        = DateTimeFormatter.ofPattern("yyMMdd");
    private final String fullName;

    public BrainsOnlyDownloader(String baseUrl, String fullName) {
        super(baseUrl, DOWNLOAD_DIR, fullName);
        this.fullName = fullName;
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return df.format(date);
    }

    public DayOfWeek[] getDownloadDates() {
        return Downloader.DATE_DAILY;
    }

    public String getName() {
        return this.fullName;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        FileHandle downloadTo = fileHandler.getFileHandle(
            this.downloadDirectory, this.createFileName(date)
        );

        if (fileHandler.exists(downloadTo)) {
            return null;
        }

        FileHandle plainText = downloadToTempFile(this.getName(), date);

        if (plainText == null) {
            return null;
        }

        try (
            InputStream is = fileHandler.getInputStream(plainText);
            DataOutputStream os = new DataOutputStream(
                fileHandler.getOutputStream(downloadTo)
            );
        ) {
            boolean retVal = BrainsOnlyIO.convertBrainsOnly(is, os, date);

            // not sure if i should really leave these closes in...
            os.close();
            is.close();

            fileHandler.delete(plainText);
            LOG.log(Level.INFO, "Saved to "+downloadTo);
            if (!retVal) {
                LOG.log(Level.SEVERE, "Unable to convert KFS puzzle into Across Lite format.");
                fileHandler.delete(downloadTo);
                downloadTo = null;
            }
        } catch (Exception ioe) {
            LOG.log(Level.SEVERE, "Exception converting KFS puzzle into Across Lite format.", ioe);
            fileHandler.delete(downloadTo);
            downloadTo = null;
        }

        return new Downloader.DownloadResult(downloadTo);
    }

}
