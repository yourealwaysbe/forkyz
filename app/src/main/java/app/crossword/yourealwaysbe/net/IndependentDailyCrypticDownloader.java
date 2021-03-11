package app.crossword.yourealwaysbe.net;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.IndependentXMLIO;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * Independent Daily Cryptic downloader
 * https://puzzles.independent.co.uk/games/webgl-cryptic-crossword-independent
 * Date = Daily
 */
public class IndependentDailyCrypticDownloader extends AbstractDownloader {
    private static final String NAME = "The Independent's Cryptic Crossword";

    public IndependentDailyCrypticDownloader() {
        super(
            "https://ams.cdn.arkadiumhosted.com/assets/gamesfeed/independent/daily-crossword/",
            getStandardDownloadDir(),
            NAME
        );
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_DAILY;
    }

    public String getName() {
        return NAME;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        return download(date, this.createUrlSuffix(date), EMPTY_MAP);
    }

    protected Downloader.DownloadResult download(
        LocalDate date,
        String urlSuffix,
        Map<String, String> headers,
        boolean canDefer
    ) {
        URL url = null;
        try {
            url = new URL(this.baseUrl + urlSuffix);
        } catch (MalformedURLException e) {
            LOG.log(Level.SEVERE, "Error downloading Independent puzzle: " + e);
            return null;
        }

        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        String fileName = this.createFileName(date);

        FileHandle f = fileHandler.createFileHandle(
            this.downloadDirectory, this.createFileName(date)
        );
        if (f == null)
            return null;

        try (
            InputStream is = url.openStream();
            DataOutputStream dos = new DataOutputStream(
                 fileHandler.getOutputStream(f)
            )
        ) {
            boolean retVal =
                IndependentXMLIO.convertPuzzle(is, dos,
                                               "Copyright unknown.", date);

            if (!retVal) {
                LOG.log(Level.SEVERE,
                        "Unable to convert Independent XML puzzle into Across Lite format.");
            } else {
                return new Downloader.DownloadResult(f);
            }
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "Exception converting Independent XML puzzle into Across Lite format.", ioe);
        }

        return null;
    }

    protected String createUrlSuffix(LocalDate date) {
        return String.format("c_%02d%02d%02d.xml",
                             date.getYear() % 100,
                             date.getMonthValue(),
                             date.getDayOfMonth());
    }
}
