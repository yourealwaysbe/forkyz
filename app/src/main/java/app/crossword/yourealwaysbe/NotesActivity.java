package app.crossword.yourealwaysbe;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.view.BoardEditText.BoardEditFilter;
import app.crossword.yourealwaysbe.view.BoardEditText;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.view.ScrollingImageView;

public class NotesActivity extends PuzzleActivity {
    private static final Logger LOG = Logger.getLogger(NotesActivity.class.getCanonicalName());

    private static final String TRANSFER_RESPONSE_REQUEST_KEY
        = "transferResponseRequest";

    private enum TransferResponseRequest {
        SCRATCH_TO_BOARD,
        ANAGRAM_SOL_TO_BOARD,
        BOARD_TO_SCRATCH,
        BOARD_TO_ANAGRAM_SOL
    }

    protected KeyboardManager keyboardManager;
    private ScrollingImageView imageView;
    private EditText notesBox;
    private BoardEditText scratchView;
    private BoardEditText anagramSourceView;
    private BoardEditText anagramSolView;
    private PlayboardRenderer renderer;

    private Random rand = new Random();

    private int numAnagramLetters = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item == null || item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        utils.holographic(this);
        utils.finishOnHomeButton(this);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        this.renderer = new PlayboardRenderer(getBoard(), metrics.densityDpi, metrics.widthPixels, !prefs.getBoolean("supressHints", false), this);

        final int curWordLen = getBoard().getCurrentWord().length;
        double scale = this.renderer.fitTo(metrics.widthPixels, curWordLen);
        if (scale > 1)
            this.renderer.setScale((float) 1);

        Playboard board = getBoard();
        Puzzle puz = board.getPuzzle();

        setContentView(R.layout.notes);

        Clue c = board.getClue();

        boolean showCount = prefs.getBoolean("showCount", false);

        TextView clue = (TextView) this.findViewById(R.id.clueLine);
        if (clue != null && clue.getVisibility() != View.GONE) {
            clue.setVisibility(View.GONE);
            clue = (TextView) utils.onActionBarCustom(this,
                R.layout.clue_line_only).findViewById(R.id.clueLine);
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            clue.setAutoSizeTextTypeUniformWithConfiguration(
                5, prefs.getInt("clueSize", 12), 1,
                TypedValue.COMPLEX_UNIT_SP
            );
        }

        clue.setText("("
            + (board.isAcross() ? "across" : "down")
            + ") "
            + c.number
            + ". "
            + c.hint
            + (showCount ? ("  ["
            + curWordLen + "]") : ""));

        imageView = (ScrollingImageView) this.findViewById(R.id.miniboard);
        imageView.setAllowOverScroll(false);
        this.imageView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                View focused = getWindow().getCurrentFocus();
                switch (focused.getId()) {
                case R.id.scratchMiniboard:
                    NotesActivity.this.executeTransferResponseRequest(
                        TransferResponseRequest.BOARD_TO_SCRATCH, true
                    );
                    break;
                case R.id.anagramSolution:
                    NotesActivity.this.executeTransferResponseRequest(
                        TransferResponseRequest.BOARD_TO_ANAGRAM_SOL, true
                    );
                    break;
                }
            }

            public void onTap(Point e) {
                NotesActivity.this.keyboardManager.showKeyboard(imageView);

                Word current = getBoard().getCurrentWord();
                int newAcross = current.start.across;
                int newDown = current.start.down;
                int box = renderer.findBox(e).across;

                if (box < current.length) {
                    if (getBoard().isAcross()) {
                        newAcross += box;
                    } else {
                        newDown += box;
                    }
                }

                Position newPos = new Position(newAcross, newDown);

                if (!newPos.equals(getBoard().getHighlightLetter())) {
                    getBoard().setHighlightLetter(newPos);
                }
            }
        });
        this.imageView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP)
                    return NotesActivity.this.onMiniboardKeyUp(keyCode, event);
                else
                    return false;
            }
        });

        Note note = puz.getNote(c.number, getBoard().isAcross());
        notesBox = (EditText) this.findViewById(R.id.notesBox);

        if (note != null) {
            notesBox.setText(note.getText());
        }

        notesBox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                NotesActivity.this.moveScratchToNote();
                return true;
            }
        });
        notesBox.setOnFocusChangeListener(
            new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean gainFocus) {
                    NotesActivity.this.onNotesBoxFocusChanged(gainFocus);
                }
            }
        );

        scratchView = (BoardEditText) this.findViewById(R.id.scratchMiniboard);
        if (note != null) {
            scratchView.setFromString(note.getScratch());
        }
        scratchView.setRenderer(renderer);
        scratchView.setLength(curWordLen);
        scratchView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                executeTransferResponseRequest(
                    TransferResponseRequest.SCRATCH_TO_BOARD, true
                );
            }

            public void onTap(Point e) {
                NotesActivity.this.keyboardManager.showKeyboard(scratchView);
                NotesActivity.this.render();
            }
        });

        anagramSourceView = (BoardEditText) this.findViewById(R.id.anagramSource);
        if (note != null) {
            String src = note.getAnagramSource();
            if (src != null) {
                anagramSourceView.setFromString(src);
                for (int i = 0; i < src.length(); i++) {
                    if (Character.isLetter(src.charAt(i))) {
                        numAnagramLetters++;
                    }
                }
            }
        }

        anagramSolView = (BoardEditText) this.findViewById(R.id.anagramSolution);
        if (note != null) {
            String sol = note.getAnagramSolution();
            if (sol != null) {
                anagramSolView.setFromString(sol);
                for (int i = 0; i < sol.length(); i++) {
                    if (Character.isLetter(sol.charAt(i))) {
                        numAnagramLetters++;
                    }
                }
            }
        }

        BoardEditFilter sourceFilter = new BoardEditFilter() {
            public boolean delete(char oldChar, int pos) {
                if (Character.isLetter(oldChar)) {
                    numAnagramLetters--;
                }
                return true;
            }

            public char filter(char oldChar, char newChar, int pos) {
                if (Character.isLetter(newChar)) {
                    if (Character.isLetter(oldChar)) {
                        return newChar;
                    } else if (numAnagramLetters < curWordLen) {
                        numAnagramLetters++;
                        return newChar;
                    } else {
                        return '\0';
                    }
                } else {
                    return '\0';
                }
            }
        };

        anagramSourceView.setRenderer(renderer);
        anagramSourceView.setLength(curWordLen);
        anagramSourceView.setFilters(new BoardEditFilter[]{sourceFilter});
        anagramSourceView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                // reshuffle squares
                int len = anagramSourceView.getLength();
                for (int i = 0; i < len; i++) {
                    int j = rand.nextInt(len);
                    char ci = anagramSourceView.getResponse(i);
                    char cj = anagramSourceView.getResponse(j);
                    anagramSourceView.setResponse(i, cj);
                    anagramSourceView.setResponse(j, ci);
                }
                NotesActivity.this.render();
            }

            public void onTap(Point e) {
                NotesActivity
                    .this
                    .keyboardManager
                    .showKeyboard(anagramSourceView);
                NotesActivity.this.render();
            }
        });

        BoardEditFilter solFilter = new BoardEditFilter() {
            public boolean delete(char oldChar, int pos) {
                if (Character.isLetter(oldChar)) {
                    for (int i = 0; i < curWordLen; i++) {
                        if (anagramSourceView.isBlank(i)) {
                            anagramSourceView.setResponse(i, oldChar);
                            return true;
                        }
                    }
                }
                return true;
            }

            public char filter(char oldChar, char newChar, int pos) {
                boolean changed
                    = NotesActivity.this.preAnagramSolResponse(pos, newChar);
                return changed ? newChar : '\0';
            }
        };

        anagramSolView.setRenderer(renderer);
        anagramSolView.setLength(curWordLen);
        anagramSolView.setFilters(new BoardEditFilter[]{solFilter});
        anagramSolView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                executeTransferResponseRequest(
                    TransferResponseRequest.ANAGRAM_SOL_TO_BOARD, true
                );
            }

            public void onTap(Point e) {
                NotesActivity.this.keyboardManager.showKeyboard(anagramSolView);
                NotesActivity.this.render();
            }
        });

        ForkyzKeyboard keyboardView
            = (ForkyzKeyboard) findViewById(R.id.keyboard);
        keyboardManager = new KeyboardManager(this, keyboardView, imageView);
        keyboardManager.showKeyboard(imageView);

        this.render();
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            if (!keyboardManager.handleBackKey())
                this.finish();
            return true;
        default:
            return super.onKeyUp(keyCode, event);
        }
    }

    public void onPause() {
        EditText notesBox = (EditText) this.findViewById(R.id.notesBox);
        String text = notesBox.getText().toString();

        String scratch = scratchView.toString();
        String anagramSource = anagramSourceView.toString();
        String anagramSolution = anagramSolView.toString();

        Puzzle puz = getPuzzle();
        if (puz != null) {
            Note note = new Note(scratch, text, anagramSource, anagramSolution);
            Clue c = getBoard().getClue();
            puz.setNote(note, c.number, getBoard().isAcross());
        }

        super.onPause();

        keyboardManager.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (keyboardManager != null)
            keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardManager != null)
            keyboardManager.onDestroy();
    }

    private boolean onMiniboardKeyUp(int keyCode, KeyEvent event) {
        Word w = getBoard().getCurrentWord();
        Position last = new Position(w.start.across
                + (w.across ? (w.length - 1) : 0), w.start.down
                + ((!w.across) ? (w.length - 1) : 0));

        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
            return false;

        case KeyEvent.KEYCODE_DPAD_LEFT:

            if (!getBoard().getHighlightLetter().equals(
                    getBoard().getCurrentWord().start)) {
                getBoard().moveLeft();
            }

            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:

            if (!getBoard().getHighlightLetter().equals(last)) {
                getBoard().moveRight();
            }

            return true;

        case KeyEvent.KEYCODE_DEL:
            w = getBoard().getCurrentWord();
            getBoard().deleteLetter();

            Position p = getBoard().getHighlightLetter();

            if (!w.checkInWord(p.across, p.down)) {
                getBoard().setHighlightLetter(w.start);
            }

            return true;

        case KeyEvent.KEYCODE_SPACE:
            getBoard().playLetter(' ');

            Position curr = getBoard().getHighlightLetter();

            if (!getBoard().getCurrentWord().equals(w)
                    || (getBoard().getBoxes()[curr.across][curr.down] == null)) {
                getBoard().setHighlightLetter(last);
            }

            return true;
        }

        char c = Character .toUpperCase(event.getDisplayLabel());

        if (PlayActivity.ALPHA.indexOf(c) != -1) {
            getBoard().playLetter(c);

            Position p = getBoard().getHighlightLetter();

            if (!getBoard().getCurrentWord().equals(w)
                    || (getBoard().getBoxes()[p.across][p.down] == null)) {
                getBoard().setHighlightLetter(last);
            }

            return true;
        }

        return false;
    }

    @Override
    public void onPlayboardChange(Word currentWord, Word previousWord) {
        super.onPlayboardChange(currentWord, previousWord);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        keyboardManager.onResume();

        this.render();
    }

    protected void render() {
        boolean displayScratch = prefs.getBoolean("displayScratch", false);
        boolean displayScratchAcross = displayScratch && !getBoard().isAcross();
        boolean displayScratchDown = displayScratch && getBoard().isAcross();
        this.imageView.setBitmap(renderer.drawWord(displayScratchAcross,
                                                   displayScratchDown));
    }

    private void moveScratchToNote() {
        EditText notesBox = (EditText) this.findViewById(R.id.notesBox);
        String notesText = notesBox.getText().toString();

        String scratchText = scratchView.toString();

        if (notesText.length() > 0)
            notesText += "\n";
        notesText += scratchText;

        scratchView.clear();
        notesBox.setText(notesText);

        render();
    }

    private void copyBoxesToAnagramSol(Box[] boxes) {
        for (int i = 0; i < boxes.length; i++) {
            if (!boxes[i].isBlank()) {
                char newChar = boxes[i].getResponse();
                boolean allowed = preAnagramSolResponse(i, newChar);
                if (allowed)
                    anagramSolView.setResponse(i, newChar);
            }
        }
    }

    private boolean hasConflict(Box[] source,
                                Box[] dest,
                                boolean copyBlanks) {
        int length = Math.min(source.length, dest.length);
        for (int i = 0; i < length; i++) {
            if ((copyBlanks || !source[i].isBlank()) &&
                !dest[i].isBlank() &&
                source[i].getResponse() != dest[i].getResponse()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies non-blank characters from response to the view
     */
    private void overlayBoxesOnBoardView(Box[] boxes,
                                         BoardEditText view) {
        int length = Math.min(boxes.length, view.getLength());
        for (int i = 0; i < length; i++) {
            if (!boxes[i].isBlank()) {
                view.setResponse(i, boxes[i].getResponse());
            }
        }
    }

    /**
     * Make arrangements for anagram letter to be played
     *
     * Changes source/sol boxes by moving required letters around.
     *
     * @return true if play of letter can proceed
     */
    private boolean preAnagramSolResponse(int pos, char newChar) {
        char oldChar = anagramSolView.getResponse(pos);
        if (Character.isLetter(newChar)) {
            int sourceLen = anagramSourceView.getLength();
            for (int i = 0; i < sourceLen; i++) {
                if (anagramSourceView.getResponse(i) == newChar) {
                    anagramSourceView.setResponse(i, oldChar);
                    return true;
                }
            }
            // if failed to find it in the source view, see if we can
            // find one to swap it with one in the solution
            int solLen = anagramSolView.getLength();
            for (int i = 0; i < solLen; i++) {
                if (anagramSolView.getResponse(i) == newChar) {
                    anagramSolView.setResponse(i, oldChar);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Transfer one board view to another
     *
     * Somewhat inelegant as a dialog confirmation has to be robust
     * against activity recreation, so it all gets funnelled through
     * here with little external state.
     *
     * The alternative is to copy-paste the dialog construction several
     * times. I'm not sure which is better.
     */
    private void executeTransferResponseRequest(
        TransferResponseRequest request,
        boolean confirmOverwrite
    ) {
        Playboard board = getBoard();
        if (board == null)
            return;

        boolean conflict = false;
        Box[] curWordBoxes = board.getCurrentWordBoxes();

        if (confirmOverwrite) {
            switch (request) {
            case SCRATCH_TO_BOARD:
                conflict = hasConflict(
                    scratchView.getBoxes(), curWordBoxes, true
                );
                break;
            case ANAGRAM_SOL_TO_BOARD:
                conflict = hasConflict(
                    anagramSolView.getBoxes(), curWordBoxes, true
                );
                break;
            case BOARD_TO_SCRATCH:
                conflict = hasConflict(
                    curWordBoxes, scratchView.getBoxes(), false
                );
                break;
            case BOARD_TO_ANAGRAM_SOL:
                conflict = false;
            }
        }

        if (conflict) {
            confirmAndExecuteTransferRequest(request);
        } else {
            switch (request) {
            case SCRATCH_TO_BOARD:
                board.setCurrentWord(scratchView.getBoxes());
                break;
            case ANAGRAM_SOL_TO_BOARD:
                board.setCurrentWord(anagramSolView.getBoxes());
                break;
            case BOARD_TO_SCRATCH:
                overlayBoxesOnBoardView(curWordBoxes, scratchView);
                break;
            case BOARD_TO_ANAGRAM_SOL:
                copyBoxesToAnagramSol(curWordBoxes);
                break;
            }
        }
    }

    private void confirmAndExecuteTransferRequest(
        TransferResponseRequest request
    ) {
        DialogFragment dialog = new TransferResponseRequestDialog();
        Bundle args = new Bundle();
        args.putSerializable(TRANSFER_RESPONSE_REQUEST_KEY, request);
        dialog.setArguments(args);
        dialog.show(
            getSupportFragmentManager(), "TransferResponseRequestDialog"
        );
    }

    public static class TransferResponseRequestDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final NotesActivity activity = (NotesActivity) getActivity();

            Bundle args = getArguments();
            TransferResponseRequest request
                = (TransferResponseRequest)
                    args.getSerializable(TRANSFER_RESPONSE_REQUEST_KEY);

            AlertDialog.Builder builder
                = new AlertDialog.Builder(activity);

            builder.setTitle(R.string.copy_conflict)
                .setMessage(R.string.transfer_overwrite_warning)
                .setPositiveButton(R.string.yes,
                                      new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        NotesActivity activity = ((NotesActivity) getActivity());
                        activity.executeTransferResponseRequest(request, false);
                    }
                })
                .setNegativeButton(R.string.no,
                                          new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

            return builder.create();
        }
    }

    /**
     * Force hide in-app keyboard if focus gained, hide soft keyboard if
     * focus lost
     */
    private void onNotesBoxFocusChanged(boolean gainFocus) {
        if (gainFocus) {
            keyboardManager.hideKeyboard(true);
        } else {
            InputMethodManager imm
                = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(notesBox.getWindowToken(), 0);
            }
        }
    }
}
