package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;


/**
 * Ink Well Crosswords
 * URL: http://herbach.dnsalias.com/Tausig/vvYYMMDD.puz
 * Date = Fridays
 */
public class InkwellDownloader extends AbstractDownloader {
    private static final String NAME = "InkWellXWords.com";
    NumberFormat nf = NumberFormat.getInstance();

    public InkwellDownloader() {
        super("http://herbach.dnsalias.com/Tausig/", getStandardDownloadDir(), NAME);
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
        return "vv" + (date.getYear() % 100) + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) + ".puz";
    }
}
