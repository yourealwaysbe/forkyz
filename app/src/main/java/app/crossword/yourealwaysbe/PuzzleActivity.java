package app.crossword.yourealwaysbe;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.PuzHandle;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PuzzleActivity
        extends ForkyzActivity
        implements Playboard.PlayboardListener {

    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    public static final String SHOW_TIMER = "showTimer";
    public static final String PRESERVE_CORRECT
        = "preserveCorrectLettersInShowErrors";
    public static final String DONT_DELETE_CROSSING = "dontDeleteCrossing";

    private ImaginaryTimer timer;
    private Handler handler = new Handler(Looper.getMainLooper());

    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            PuzzleActivity.this.onTimerUpdate();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Puzzle puz = getPuzzle();

        if (puz != null && puz.getPercentComplete() != 100) {
            ImaginaryTimer timer = new ImaginaryTimer(puz.getTime());
            setTimer(timer);
            timer.start();

            if (prefs.getBoolean(SHOW_TIMER, false)) {
                handler.post(updateTimeTask);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (prefs.getBoolean(SHOW_TIMER, false)) {
            handler.post(updateTimeTask);
        }

        Playboard board = getBoard();
        boolean preserveCorrect = prefs.getBoolean(PRESERVE_CORRECT, true);
        board.setPreserveCorrectLettersInShowErrors(preserveCorrect);
        boolean noDelCrossing = prefs.getBoolean(DONT_DELETE_CROSSING, true);
        board.setDontDeleteCrossing(noDelCrossing);
    }

    public void onPlayboardChange(Word currentWord, Word previousWord) {
        Puzzle puz = getPuzzle();
        ImaginaryTimer timer = getTimer();

        if (puz != null &&
            puz.getPercentComplete() == 100 &&
            timer != null) {

            timer.stop();
            puz.setTime(timer.getElapsed());
            setTimer(null);
            Intent i = new Intent(PuzzleActivity.this,
                                  PuzzleFinishedActivity.class);
            this.startActivity(i);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Puzzle puz = getPuzzle();
        ImaginaryTimer timer = getTimer();

        if ((puz != null)) {
            if ((timer != null) && (puz.getPercentComplete() != 100)) {
                timer.stop();
                puz.setTime(timer.getElapsed());
                setTimer(null);
            }

            try {
                saveBoard();
            } catch (IOException ioe) {
                LOG.log(Level.SEVERE, null, ioe);
            }
        }

        Playboard board = getBoard();
        if (board != null)
            board.removeListener(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        ImaginaryTimer timer = getTimer();
        if (timer != null)
            timer.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Puzzle puz = getPuzzle();
        if (puz != null && puz.getPercentComplete() != 100) {
            ImaginaryTimer timer = new ImaginaryTimer(puz.getTime());
            setTimer(timer);
            timer.start();
        }

        if (prefs.getBoolean(SHOW_TIMER, false)) {
            handler.post(updateTimeTask);
        }

        Playboard board = getBoard();
        if (board != null)
            board.addListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        ImaginaryTimer timer = getTimer();
        if (timer != null) {
            timer.stop();
        }
    }

    /**
     * Override if you want to update your UI based on the timer
     *
     * But still call super. Only called if the showTimer pref is true
     */
    protected void onTimerUpdate() {
        if (prefs.getBoolean(SHOW_TIMER, false)) {
            handler.postDelayed(updateTimeTask, 1000);
        }
    }

    protected Playboard getBoard(){
        return ForkyzApplication.getInstance().getBoard();
    }

    protected Puzzle getPuzzle() {
        Playboard board = getBoard();
        return (board == null) ? null : getBoard().getPuzzle();
    }

    protected void setTimer(ImaginaryTimer timer) {
        this.timer = timer;
    }

    protected ImaginaryTimer getTimer() {
        return timer;
    }

    protected PuzHandle getPuzHandle() {
        return ForkyzApplication.getInstance().getPuzHandle();
    }

    protected void saveBoard() throws IOException {
        ForkyzApplication.getInstance().saveBoard();
    }
}
