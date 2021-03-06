package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;

public class LATimesDownloader extends AbstractJPZDownloader {

    private static final String NAME = "Los Angeles Times";
    private final HashMap<String, String> headers = new HashMap<String, String>();
    NumberFormat nf = NumberFormat.getInstance();

    public LATimesDownloader() {
        super(
                "http://cdn.games.arkadiumhosted.com/latimes/assets/DailyCrossword/",
                DOWNLOAD_DIR, NAME);
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
        headers.put("Accept","*/*");
        headers.put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        headers.put("Accept-Language", "en-US,en;q=0.8");
        headers.put("Connection", "keep-alive");
        headers.put("Host", "cdn.games.arkadiumhosted.com");
        headers.put(
                "Referer",
                "http://cdn.games.arkadiumhosted.com/latimes/games/daily-crossword/game/crossword-expert.swf");
        headers.put("Content-Length", "0");

    }

    public DayOfWeek[] getDownloadDates() {
        return LATimesDownloader.DATE_DAILY;

    }

    public String getName() {
        return NAME;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        return download(date, this.createUrlSuffix(date), headers);
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        String val = "la";
        if(date.isBefore(LocalDate.of(114, 0, 27))){
            val = "puzzle_";
        }
        return val + (date.getYear() % 100)
                + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth())
                + ".xml";
    }

}
