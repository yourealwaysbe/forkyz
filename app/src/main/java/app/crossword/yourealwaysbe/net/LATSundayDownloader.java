package app.crossword.yourealwaysbe.net;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Created by keber_000 on 2/11/14.
 */
public class LATSundayDownloader extends AbstractJPZDownloader {

    private static final String NAME = "LAT Sunday Calendar";
    private final DateTimeFormatter df
        = DateTimeFormatter.ofPattern("yyMMdd");


    public LATSundayDownloader() {
        super("https://washingtonpost.as.arkadiumhosted.com/clients/washingtonpost-content/SundayCrossword/mreagle_", DOWNLOAD_DIR, NAME);
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return df.format(date) +".xml";
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_SUNDAY;
    }

    public String getName() {
        return NAME;
    }

    public File download(LocalDate date) {
        return download(date, this.createUrlSuffix(date), EMPTY_MAP);
    }
}
