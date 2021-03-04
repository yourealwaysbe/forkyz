package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;


/**
 * Washington Post downloader
 * URL: http://www.washingtonpost.com/r/WashingtonPost/Content/Puzzles/Daily/
 * Date = Daily
 */
public class WaPoDownloader extends AbstractJPZDownloader {
    private static final String NAME = "Washington Post";
    NumberFormat nf = NumberFormat.getInstance();

    public WaPoDownloader() {
        super("https://washingtonpost.as.arkadiumhosted.com/clients/washingtonpost-content/crossynergy/", DOWNLOAD_DIR, NAME);
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
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

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return "cs" + (date.getYear() % 100) + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) + ".jpz";
    }
}
