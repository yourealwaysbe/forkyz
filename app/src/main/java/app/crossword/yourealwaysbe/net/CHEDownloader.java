package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;


/**
 * Chronicle of Higher Education
 * URL: http://chronicle.com/items/biz/puzzles/YYYYMMDD.puz
 * Date = Fridays
 */
public class CHEDownloader extends AbstractDownloader {
    private static final String NAME = "Chronicle of Higher Education";
    NumberFormat nf = NumberFormat.getInstance();

    public CHEDownloader() {
        super("http://chronicle.com/items/biz/puzzles/", DOWNLOAD_DIR, NAME);
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_FRIDAY;
    }

    public String getName() {
        return NAME;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        return super.download(date, this.createUrlSuffix(date));
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return date.getYear() + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) + ".puz";
    }
}
