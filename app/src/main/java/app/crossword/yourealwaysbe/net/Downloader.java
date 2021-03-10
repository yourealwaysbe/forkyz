package app.crossword.yourealwaysbe.net;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;

import android.content.Context;

import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

public interface Downloader {
    // These lists must be sorted for binary search.
    DayOfWeek[] DATE_SUNDAY = new DayOfWeek[] { DayOfWeek.SUNDAY };
    DayOfWeek[] DATE_MONDAY = new DayOfWeek[] { DayOfWeek.MONDAY };
    DayOfWeek[] DATE_TUESDAY = new DayOfWeek[] { DayOfWeek.TUESDAY };
    DayOfWeek[] DATE_WEDNESDAY = new DayOfWeek[] { DayOfWeek.WEDNESDAY };
    DayOfWeek[] DATE_THURSDAY = new DayOfWeek[] { DayOfWeek.THURSDAY };
    DayOfWeek[] DATE_FRIDAY = new DayOfWeek[] { DayOfWeek.FRIDAY };
    DayOfWeek[] DATE_SATURDAY = new DayOfWeek[] { DayOfWeek.SATURDAY };
    DayOfWeek[] DATE_DAILY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    };
    DayOfWeek[] DATE_NO_SUNDAY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    };

    public static class DownloadResult {
        private FileHandle fileHandle;
        private boolean isDeferred;
        public static final DownloadResult DEFERRED_FILE = new DownloadResult();

        public DownloadResult(FileHandle fileHandle) {
            this.fileHandle = fileHandle;
            this.isDeferred = false;
        }

        private DownloadResult() {
            this.isDeferred = true;
        }

        public FileHandle getFileHandle() { return fileHandle; }
        public boolean getIsDeferred() { return isDeferred; }
    }

    void setContext(Context context);

    DayOfWeek[] getDownloadDates();

    DirHandle getDownloadDir();

    String getName();

    String createFileName(LocalDate date);

    DownloadResult download(LocalDate date);

    String sourceUrl(LocalDate date);

    boolean alwaysRun();

    LocalDate getGoodThrough();

    LocalDate getGoodFrom();
}
