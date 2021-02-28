package app.crossword.yourealwaysbe;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.view.ClueTabs;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.view.ScrollingImageView;

import java.io.IOException;

public class ClueListActivity extends PuzzleActivity
                              implements ClueTabs.ClueTabsListener {
    private KeyboardManager keyboardManager;
    private ScrollingImageView imageView;
    private PlayboardRenderer renderer;
    private ClueTabs clueTabs;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case R.id.clue_list_menu_notes:
            launchNotes();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        utils.holographic(this);
        utils.finishOnHomeButton(this);
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        Playboard board = getBoard();
        Puzzle puz = getBoard().getPuzzle();

        this.renderer = new PlayboardRenderer(board, metrics.densityDpi, metrics.widthPixels, !prefs.getBoolean("supressHints", false), this);

        scaleRendererToCurWord();

        setContentView(R.layout.clue_list);

        this.imageView = (ScrollingImageView) this.findViewById(R.id.miniboard);
        this.imageView.setAllowOverScroll(false);

        this.imageView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                onTap(e);
                launchNotes();
            }

            public void onTap(Point e) {
                Playboard board = getBoard();

                if (board == null)
                    return;

                Word current = board.getCurrentWord();
                boolean across = board.isAcross();

                int newAcross = current.start.across;
                int newDown = current.start.down;
                int box = renderer.findBox(e).across;

                if (box < current.length) {
                    if (across) {
                        newAcross += box;
                    } else {
                        newDown += box;
                    }
                }

                Position newPos = new Position(newAcross, newDown);

                if (!newPos.equals(board.getHighlightLetter())) {
                    board.setHighlightLetter(newPos);
                }

                displayKeyboard();
            }
        });

        // i know this is not needed because of onKeyUp / onKeyDown,
        // but it seems cleaner to me that we don't rely on the
        // keypress finding its way from the board to the activity
        this.imageView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP)
                    return ClueListActivity.this.onKeyUp(keyCode, event);
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                    return ClueListActivity.this.onKeyDown(keyCode, event);
                else
                    return false;
            }
        });

        this.clueTabs = this.findViewById(R.id.clueListClueTabs);
        this.clueTabs.setBoard(getBoard());

        ForkyzKeyboard keyboard = (ForkyzKeyboard) findViewById(R.id.keyboard);
        keyboardManager = new KeyboardManager(this, keyboard, imageView);

        this.render();
    }

    @Override
    public void onResume() {
        super.onResume();
        keyboardManager.onResume();

        if (clueTabs != null) {
            clueTabs.addListener(this);
            clueTabs.listenBoard();
            clueTabs.refresh();

            if (clueTabs.getBoardDelMeNotReallyNeeded() != getBoard()) {
                Toast t = Toast.makeText(this,
                                         "Clue tabs board not match app",
                                         Toast.LENGTH_SHORT);
                t.show();
            }
        }

        this.render();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.clue_list_menu, menu);
        return true;
    }

    @Override
    public void onPlayboardChange(Word currentWord, Word previousWord) {
        super.onPlayboardChange(currentWord, previousWord);
        this.render();
    }

    @Override
    public void onClueTabsClick(Clue clue,
                                int index,
                                boolean across,
                                ClueTabs view) {
        Playboard board = getBoard();
        if (board != null) {
            Word old = board.getCurrentWord();
            board.jumpTo(index, across);
            displayKeyboard(old);
        }
    }

    @Override
    public void onClueTabsLongClick(Clue clue,
                                    int index,
                                    boolean across,
                                    ClueTabs view) {
        Playboard board = getBoard();
        if (board != null) {
            board.jumpTo(index, across);
            launchNotes();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Playboard board = getBoard();
        Word w = board.getCurrentWord();
        Position last = new Position(w.start.across
                + (w.across ? (w.length - 1) : 0), w.start.down
                + ((!w.across) ? (w.length - 1) : 0));

        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
            return false;

        case KeyEvent.KEYCODE_BACK:
            if (!keyboardManager.handleBackKey())
                this.finish();
            return true;

        case KeyEvent.KEYCODE_DPAD_LEFT:

            if (!board.getHighlightLetter().equals(
                    board.getCurrentWord().start)) {
                board.moveLeft();
            }

            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:

            if (!board.getHighlightLetter().equals(last)) {
                board.moveRight();
            }

            return true;

        case KeyEvent.KEYCODE_DEL:
            w = board.getCurrentWord();
            board.deleteLetter();

            Position p = board.getHighlightLetter();

            if (!w.checkInWord(p.across, p.down)) {
                board.setHighlightLetter(w.start);
            }

            return true;

        case KeyEvent.KEYCODE_SPACE:

            if (!prefs.getBoolean("spaceChangesDirection", true)) {
                board.playLetter(' ');

                Position curr = board.getHighlightLetter();

                if (!board.getCurrentWord().equals(w)
                        || (board.getBoxes()[curr.across][curr.down] == null)) {
                    board.setHighlightLetter(last);
                }

                return true;
            }
        }

        char c = Character.toUpperCase(event.getDisplayLabel());

        if (PlayActivity.ALPHA.indexOf(c) != -1) {
            board.playLetter(c);

            Position p = board.getHighlightLetter();

            if (!board.getCurrentWord().equals(w)
                    || (board.getBoxes()[p.across][p.down] == null)) {
                board.setHighlightLetter(last);
            }

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();

        keyboardManager.onPause();

        if (clueTabs != null) {
            clueTabs.removeListener(this);
            clueTabs.unlistenBoard();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        keyboardManager.onDestroy();
    }

    private void displayKeyboard() {
        keyboardManager.showKeyboard(imageView);
    }

    private void displayKeyboard(Word previousWord) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Playboard board = getBoard();
        if (board != null) {
            Position newPos = board.getHighlightLetter();
            if ((previousWord != null) &&
                previousWord.checkInWord(newPos.across, newPos.down)) {
                keyboardManager.showKeyboard(imageView);
            } else {
                keyboardManager.hideKeyboard();
            }
        }
    }

    private void render() {
        scaleRendererToCurWord();
        boolean displayScratch = prefs.getBoolean("displayScratch", false);
        this.imageView.setBitmap(renderer.drawWord(displayScratch, displayScratch));
    }

    /**
     * Scale the current renderer to fit the length of the currently
     * selected word.
     */
    private void scaleRendererToCurWord() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int curWordLen = getBoard().getCurrentWord().length;
        double scale = this.renderer.fitTo(metrics.widthPixels, curWordLen);
        if (scale > 1)
            this.renderer.setScale((float) 1);
    }

    private void launchNotes() {
        Intent i = new Intent(this, NotesActivity.class);
        ClueListActivity.this.startActivityForResult(i, 0);
    }
}
