package app.crossword.yourealwaysbe.net;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.logging.Level;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.KingFeaturesPlaintextIO;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.versions.DefaultUtil;

/**
 * King Features Syndicate Puzzles
 * URL: http://[puzzle].king-online.com/clues/YYYYMMDD.txt
 * premier = Sunday
 * joseph = Monday-Saturday
 * sheffer = Monday-Saturday
 */
public class KFSDownloader extends AbstractDownloader {
    DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
    NumberFormat nf = NumberFormat.getInstance();
    private String author;
    private String fullName;
    private DayOfWeek[] days;

    public KFSDownloader(String shortName, String fullName, String author, DayOfWeek[] days) {
        super(
            "http://puzzles.kingdigital.com/javacontent/clues/"+shortName+"/",
            getStandardDownloadDir(),
            fullName
        );
        this.fullName = fullName;
        this.author = author;
        this.days = days;
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public DayOfWeek[] getDownloadDates() {
        return days;
    }

    public String getName() {
        return fullName;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        String fileName = this.createFileName(date);

        FileHandle downloadTo = fileHandler.createFileHandle(
            this.downloadDirectory, this.createFileName(date)
        );
        if (downloadTo == null)
            return null;

        FileHandle plainText = downloadToTempFile(this.getName(), date);
        if (plainText == null) {
            return null;
        }

        String copyright = "\u00a9 " + date.getYear() + " King Features Syndicate.";

        try (
            InputStream is = fileHandler.getInputStream(plainText);
            DataOutputStream os = new DataOutputStream(
                fileHandler.getOutputStream(downloadTo)
            );
        ) {
            boolean retVal = KingFeaturesPlaintextIO.convertKFPuzzle(
                is, os,
                fullName + ", " + df.format(date),
                author, copyright, date
            );
            os.close();
            is.close();
            fileHandler.delete(plainText);

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

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return date.getYear() + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) + ".txt";
    }


}
