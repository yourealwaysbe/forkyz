package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;
import java.time.LocalDate;

import android.content.Context;


/**
 * Does not actually download any puzzles; just adds an "All Available" option to the dropdown.
 */
public class DummyDownloader implements Downloader {
    @Override
    public void setContext(Context context) {}

    @Override
    public DayOfWeek[] getDownloadDates() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String createFileName(LocalDate date) {
        return null;
    }

    @Override
    public Downloader.DownloadResult download(LocalDate date) {
        return null;
    }

    @Override
    public String sourceUrl(LocalDate date) {
        return null;
    }

    @Override
    public String toString() {
        return "All available";
    }

    @Override
    public boolean alwaysRun(){
        return false;
    }

    @Override
    public LocalDate getGoodThrough() {
        return LocalDate.now();
    }

    @Override
    public LocalDate getGoodFrom() {
        return LocalDate.ofEpochDay(0L);
    }
}
