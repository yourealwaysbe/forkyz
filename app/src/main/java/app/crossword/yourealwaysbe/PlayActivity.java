package app.crossword.yourealwaysbe;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.MovementStrategy;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.view.ClueTabs;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ScaleListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;


public class PlayActivity extends PuzzleActivity
                          implements Playboard.PlayboardListener,
                                     ClueTabs.ClueTabsListener {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    private static final double BOARD_DIM_RATIO = 1.0;
    private static final String SHOW_CLUES_TAB = "showCluesOnPlayScreen";
    private static final String CLUE_TABS_PAGE = "playActivityClueTabsPage";
    static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String SHOW_TIMER = "showTimer";
    public static final String SCALE = "scale";

    private ClueTabs clueTabs;
    private ConstraintLayout constraintLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    private KeyboardManager keyboardManager;
    private MovementStrategy movement = null;
    private ScrollingImageView boardView;
    private TextView clue;
    private boolean fitToScreen;

    private boolean showCount = false;
    private boolean showErrors = false;
    private boolean scratchMode = false;
    private long lastTap = 0;
    private int screenWidthInInches;
    private Runnable fitToScreenTask = new Runnable() {
        @Override
        public void run() {
            fitToScreen();
        }
    };

    private PlayboardRenderer getRenderer(){
        return ForkyzApplication.getInstance().getRenderer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        metrics = getResources().getDisplayMetrics();
        this.screenWidthInInches = (metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels) / Math.round(160 * metrics.density);
        LOG.info("Configuration Changed "+this.screenWidthInInches+" ");
        if(this.screenWidthInInches >= 7){
            this.handler.post(this.fitToScreenTask);
        }
    }

    DisplayMetrics metrics;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FileHandler fileHandler = getFileHandler();

        metrics = getResources().getDisplayMetrics();
        this.screenWidthInInches = (metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels) / Math.round(160 * metrics.density);

        try {
            if (!prefs.getBoolean(SHOW_TIMER, false)) {
                if (ForkyzApplication.isLandscape(metrics)) {
                    if (ForkyzApplication.isMiniTabletish(metrics)) {
                        utils.hideWindowTitle(this);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        utils.holographic(this);
        utils.finishOnHomeButton(this);

        this.showErrors = this.prefs.getBoolean("showErrors", false);
        this.scratchMode = this.prefs.getBoolean("scratchMode", false);
        setDefaultKeyMode(Activity.DEFAULT_KEYS_DISABLE);

        MovementStrategy movement = this.getMovementStrategy();

        setFullScreenMode();

        FileHandle baseFile = null;
        Puzzle puz = null;

        try {
            Uri u = this.getIntent().getData();

            if (u != null && u.getScheme().equals("file")) {
                baseFile = fileHandler.getFileHandle(u);
                puz = fileHandler.load(baseFile);
            }

            if (puz == null || puz.getBoxes() == null) {
                throw new IOException("Puzzle is null or contains no boxes.");
            }

            ForkyzApplication.getInstance().setBoard(
                    new Playboard(puz,
                                  movement,
                                  prefs.getBoolean("preserveCorrectLettersInShowErrors",
                                                   false),
                                  prefs.getBoolean("dontDeleteCrossing", true)),
                    baseFile
            );
            ForkyzApplication.getInstance().setRenderer(new PlayboardRenderer(getBoard(), metrics.densityDpi, metrics.widthPixels, !prefs.getBoolean("supressHints", false), this));

            float scale = prefs.getFloat(SCALE, 1.0F);

            if (scale > getRenderer().getDeviceMaxScale()) {
                scale = getRenderer().getDeviceMaxScale();
            } else if (scale < getRenderer().getDeviceMinScale()) {
                scale = getRenderer().getDeviceMinScale();
            } else if (Float.isNaN(scale)) {
                scale = 1F;
            }
            prefs.edit().putFloat(SCALE, scale).apply();

            getRenderer().setScale(scale);
            getBoard().setSkipCompletedLetters(this.prefs.getBoolean("skipFilled",
                    false));

            setContentView(R.layout.play);

            this.constraintLayout
                = (ConstraintLayout) this.findViewById(R.id.playConstraintLayout);

            this.clue = this.findViewById(R.id.clueLine);
            if (clue != null && clue.getVisibility() != View.GONE) {
                ConstraintSet set = new ConstraintSet();
                set.clone(constraintLayout);
                set.setVisibility(clue.getId(), ConstraintSet.GONE);
                set.applyTo(constraintLayout);

                View custom = utils.onActionBarCustom(this, R.layout.clue_line_only);
                if (custom != null) {
                    clue = custom.findViewById(R.id.clueLine);
                }
            }
            if(this.clue != null) {
                this.clue.setClickable(true);
                this.clue.setOnClickListener(new OnClickListener() {
                    public void onClick(View arg0) {
                        if (PlayActivity.this.prefs.getBoolean(SHOW_CLUES_TAB, true)) {
                            PlayActivity.this.hideClueTabs();
                            PlayActivity.this.render(true);
                        } else {
                            PlayActivity.this.showClueTabs();
                            PlayActivity.this.render(true);
                        }
                    }
                });
                this.clue.setOnLongClickListener(new OnLongClickListener() {
                    public boolean onLongClick(View arg0) {
                        PlayActivity.this.launchClueList();
                        return true;
                    }
                });
            }

            this.boardView = (ScrollingImageView) this.findViewById(R.id.board);
            this.boardView.setCurrentScale(scale);
            this.boardView.setFocusable(true);
            this.registerForContextMenu(boardView);
            boardView.setContextMenuListener(new ClickListener() {
                public void onContextMenu(final Point e) {
                    handler.post(new Runnable() {
                        public void run() {
                            try {
                                Position p = getRenderer().findBox(e);
                                Word w = getBoard().setHighlightLetter(p);
                                boolean displayScratch = prefs.getBoolean("displayScratch", false);
                                getRenderer().draw(w,
                                                   displayScratch,
                                                   displayScratch);

                                launchNotes();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }

                public void onTap(Point e) {
                    try {
                        if (prefs.getBoolean("doubleTap", false)
                                && ((System.currentTimeMillis() - lastTap) < 300)) {
                            if (fitToScreen) {
                                getRenderer().setScale(prefs.getFloat(SCALE, 1F));
                                boardView.setCurrentScale(1F);
                                getBoard().setHighlightLetter(getRenderer().findBox(e));
                            } else {
                                int w = boardView.getWidth();
                                int h = boardView.getHeight();
                                float scale = getRenderer().fitTo((w < h) ? w : h);
                                boardView.setCurrentScale(scale);
                                render(true);
                                boardView.scrollTo(0, 0);
                            }

                            fitToScreen = !fitToScreen;
                        } else {
                            Position p = getRenderer().findBox(e);
                            if (getBoard().isInWord(p)) {
                                Word previous = getBoard().setHighlightLetter(p);
                                displayKeyboard(previous);
                            }
                        }

                        lastTap = System.currentTimeMillis();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            // i know this is not needed because of onKeyUp / onKeyDown,
            // but it seems cleaner to me that we don't rely on the
            // keypress finding its way from the board to the activity
            this.boardView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_UP)
                        return PlayActivity.this.onKeyUp(keyCode, event);
                    if (event.getAction() == KeyEvent.ACTION_DOWN)
                        return PlayActivity.this.onKeyDown(keyCode, event);
                    else
                        return false;
                }
            });
            // constrain to 1:1 if clueTabs is showing
            boardView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                public void onLayoutChange(View v,
                  int left, int top, int right, int bottom,
                  int leftWas, int topWas, int rightWas, int bottomWas
                ) {
                    boolean constrainedHeight = false;

                    ConstraintSet set = new ConstraintSet();
                    set.clone(constraintLayout);
                    if (PlayActivity.this.prefs.getBoolean(SHOW_CLUES_TAB, true)) {
                        int height = bottom - top;
                        int width = right - left;

                        if (height > width) {
                            constrainedHeight = true;
                            set.constrainMaxHeight(boardView.getId(),
                                                   (int)(BOARD_DIM_RATIO * width));
                        }
                    } else {
                        set.constrainMaxHeight(boardView.getId(), 0);
                    }

                    set.applyTo(constraintLayout);

                    // if the view changed size, then rescale the view
                    // cannot change layout during a layout change, so
                    // use a predraw listener that requests a new layout
                    // (via render) and returns false to cancel the
                    // current draw
                    if (constrainedHeight ||
                        left != leftWas || right != rightWas ||
                        top != topWas || bottom != bottomWas) {
                        boardView.getViewTreeObserver()
                                 .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                            public boolean onPreDraw() {
                                PlayActivity.this.render(true);
                                PlayActivity.this
                                            .boardView
                                            .getViewTreeObserver()
                                            .removeOnPreDrawListener(this);
                                return false;
                            }
                        });
                    }
                }
            });
        } catch (IOException e) {
            System.err.println(this.getIntent().getData());
            e.printStackTrace();

            String filename = null;

            try {
                filename = fileHandler.getName(this.getBaseFile());
            } catch (Exception ee) {
                e.printStackTrace();
            }

            Toast t = Toast
                    .makeText(
                            this,
                            "Unable to read file" +
                                    (filename != null ?
                                            (" \n" + filename)
                                    : "")
                            , Toast.LENGTH_SHORT);
            t.show();
            this.finish();

            return;
        }

        this.boardView.setFocusable(true);
        this.boardView.setScaleListener(new ScaleListener() {
            TimerTask t;
            Timer renderTimer = new Timer();

            public void onScale(float newScale, final Point center) {
                //fitToScreen = false;

                if (t != null) {
                    t.cancel();
                }

                renderTimer.purge();
                t = new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(new Runnable() {
                            public void run() {
                                int w = boardView.getImageView().getWidth();
                                int h = boardView.getImageView().getHeight();
                                float scale = getRenderer().fitTo((w < h) ? w : h);
                                prefs.edit()
                                        .putFloat(SCALE,
                                                scale)
                                        .apply();
                                getBoard().setHighlightLetter(getRenderer().findBox(center));
                            }
                        });
                    }
                };
                renderTimer.schedule(t, 500);
                lastTap = System.currentTimeMillis();
            }
        });

        if (puz.isUpdatable()) {
            this.showErrors = false;
        }

        if (getBoard().isShowErrors() != this.showErrors) {
            getBoard().toggleShowErrors();
        }

        this.clueTabs = this.findViewById(R.id.playClueTab);
        this.clueTabs.setBoard(getBoard());
        this.clueTabs.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v,
              int left, int top, int right, int bottom,
              int leftWas, int topWas, int rightWas, int bottomWas
            ) {
                PlayActivity.this.clueTabs.setPage(prefs.getInt(CLUE_TABS_PAGE, 0));
            }
        });

        this.setClueSize(prefs.getInt("clueSize", 12));
        setTitle(neverNull(puz.getTitle()) + " - " + neverNull(puz.getAuthor())
             + " -	" + neverNull(puz.getCopyright()));
        this.showCount = prefs.getBoolean("showCount", false);
        if (this.prefs.getBoolean("fitToScreen", false) || (ForkyzApplication.isLandscape(metrics)) && (ForkyzApplication.isTabletish(metrics) || ForkyzApplication.isMiniTabletish(metrics))) {
            this.handler.postDelayed(new Runnable() {

                public void run() {
                    boardView.scrollTo(0, 0);

                    int v = (boardView.getWidth() < boardView.getHeight()) ? boardView
                            .getWidth() : boardView.getHeight();
                    if (v == 0) {
                        handler.postDelayed(this, 100);
                    }
                    float newScale = getRenderer().fitTo(v);
                    boardView.setCurrentScale(newScale);

                    prefs.edit().putFloat(SCALE, newScale).apply();
                    render();
                }

            }, 100);

        }

        ForkyzKeyboard keyboardView
            = (ForkyzKeyboard) this.findViewById(R.id.keyboard);
        keyboardManager
            = new KeyboardManager(this, keyboardView, boardView);

        this.render(true);
    }

    private static String neverNull(String val) {
        return val == null ? "" : val.trim();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.play_menu, menu);

        if (getRenderer() == null || getRenderer().getScale() >= getRenderer().getDeviceMaxScale())
            menu.removeItem(R.id.play_menu_zoom_in_max);

        Puzzle puz = getPuzzle();
        if (puz == null || puz.isUpdatable()) {
            menu.findItem(R.id.play_menu_show_errors).setEnabled(false);
            menu.findItem(R.id.play_menu_reveal).setEnabled(false);
        } else {
            menu.findItem(R.id.play_menu_show_errors).setChecked(this.showErrors);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (ForkyzApplication.isTabletish(metrics)) {
                    menu.findItem(R.id.play_menu_show_errors).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                    menu.findItem(R.id.play_menu_reveal).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                }
            }
        }

        menu.findItem(R.id.play_menu_scratch_mode).setChecked(this.scratchMode);
        return true;
    }

    private SpannableString createSpannableForMenu(String value){
        SpannableString s = new SpannableString(value);
        s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.textColorPrimary)), 0, s.length(), 0);
        return s;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER)
            return true;
        else
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        boolean handled = false;

        // handle back separately as it we shouldn't block a keyboard
        // hide because of it
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!keyboardManager.handleBackKey()) {
                this.finish();
            }
            handled = true;
        }

        keyboardManager.pushBlockHide();

        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
                getBoard().setMovementStrategy(MovementStrategy.MOVE_NEXT_CLUE);
                getBoard().nextWord();
                getBoard().setMovementStrategy(this.getMovementStrategy());
                handled = true;
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                getBoard().moveDown();
                handled = true;
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                getBoard().moveUp();
                handled = true;
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                getBoard().moveLeft();
                handled = true;
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                getBoard().moveRight();
                handled = true;
                break;

            case KeyEvent.KEYCODE_DPAD_CENTER:
                getBoard().toggleDirection();
                handled = true;
                break;

            case KeyEvent.KEYCODE_SPACE:
                if (prefs.getBoolean("spaceChangesDirection", true)) {
                    getBoard().toggleDirection();
                } else if (this.scratchMode) {
                    getBoard().playScratchLetter(' ');
                } else {
                    getBoard().playLetter(' ');
                }
                handled = true;
                break;

            case KeyEvent.KEYCODE_ENTER:
                if (prefs.getBoolean("enterChangesDirection", true)) {
                    getBoard().toggleDirection();
                } else {
                    getBoard().nextWord();
                }
                handled = true;
                break;

            case KeyEvent.KEYCODE_DEL:
                if (this.scratchMode) {
                    getBoard().deleteScratchLetter();
                } else {
                    getBoard().deleteLetter();
                }
                handled = true;
                break;
        }

        char c = Character.toUpperCase(event.getDisplayLabel());

        if (!handled && ALPHA.indexOf(c) != -1) {
            if (this.scratchMode) {
                getBoard().playScratchLetter(c);
            } else {
                getBoard().playLetter(c);
            }
            handled = true;
        }

        if (!handled)
            handled = super.onKeyUp(keyCode, event);

        keyboardManager.popBlockHide();

        return handled;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case R.id.play_menu_reveal_letter:
            getBoard().revealLetter();
            return true;
        case R.id.play_menu_reveal_word:
            getBoard().revealWord();
            return true;
        case R.id.play_menu_reveal_errors:
            getBoard().revealErrors();
            return true;
        case R.id.play_menu_reveal_puzzle:
            showRevealPuzzleDialog();
            return true;
        case R.id.play_menu_show_errors:
            getBoard().toggleShowErrors();
            item.setChecked(getBoard().isShowErrors());
            this.prefs.edit().putBoolean("showErrors", getBoard().isShowErrors())
                    .apply();
            return true;
        case R.id.play_menu_scratch_mode:
            this.scratchMode = !this.scratchMode;
            item.setChecked(this.scratchMode);
            this.prefs.edit().putBoolean("scratchMode", this.scratchMode).apply();
            return true;
        case R.id.play_menu_settings:
            Intent i = new Intent(this, PreferencesActivity.class);
            this.startActivity(i);

            return true;
        case R.id.play_menu_zoom_in:
            this.boardView.scrollTo(0, 0);
            {
                float newScale = getRenderer().zoomIn();
                this.prefs.edit().putFloat(SCALE, newScale).apply();
                this.fitToScreen = false;
                boardView.setCurrentScale(newScale);
            }
            this.render(true);

            return true;
        case R.id.play_menu_zoom_in_max:
            this.boardView.scrollTo(0, 0);
            {
                float newScale = getRenderer().zoomInMax();
                this.prefs.edit().putFloat(SCALE, newScale).apply();
                this.fitToScreen = false;
                boardView.setCurrentScale(newScale);
            }
            this.render(true);

            return true;
        case R.id.play_menu_zoom_out:
            this.boardView.scrollTo(0, 0);
            {
                float newScale = getRenderer().zoomOut();
                this.prefs.edit().putFloat(SCALE, newScale).apply();
                this.fitToScreen = false;
                boardView.setCurrentScale(newScale);
            }
            this.render(true);

            return true;
        case R.id.play_menu_zoom_fit:
            fitToScreen();

            return true;
        case R.id.play_menu_zoom_reset:
            float newScale = getRenderer().zoomReset();
            boardView.setCurrentScale(newScale);
            this.prefs.edit().putFloat(SCALE, newScale).apply();
            this.render(true);
            this.boardView.scrollTo(0, 0);

            return true;
        case R.id.play_menu_info:
            showInfoDialog();
            return true;
        case R.id.play_menu_clues:
            Intent clueIntent = new Intent(PlayActivity.this, ClueListActivity.class);
            PlayActivity.this.startActivityForResult(clueIntent, 0);
            return true;
        case R.id.play_menu_notes:
            launchNotes();
            return true;
        case R.id.play_menu_help:
            Intent helpIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("file:///android_asset/playscreen.html"), this,
                    HTMLActivity.class);
            this.startActivity(helpIntent);
            return true;
        case R.id.play_menu_clue_size_s:
            this.setClueSize(12);
            return true;
        case R.id.play_menu_clue_size_m:
            this.setClueSize(14);
            return true;
        case R.id.play_menu_clue_size_l:
            this.setClueSize(16);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

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

    public void onClueTabsBarSwipeDown(ClueTabs view) {
        hideClueTabs();
        render(true);
    }

    public void onClueTabsBarLongclick(ClueTabs view) {
        hideClueTabs();
        render(true);
    }

    public void onClueTabsPageChange(ClueTabs view, int pageNumber) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(CLUE_TABS_PAGE, pageNumber);
        editor.apply();
    }

    public void onPlayboardChange(Word currentWord, Word previousWord) {
        super.onPlayboardChange(currentWord, previousWord);

        // hide keyboard when moving to a new word
        Position newPos = getBoard().getHighlightLetter();
        if ((previousWord == null) ||
            !previousWord.checkInWord(newPos.across, newPos.down)) {
            keyboardManager.hideKeyboard();
        }

        render(previousWord, false);
    }

    private void fitToScreen() {
        this.boardView.scrollTo(0, 0);

        int v = (this.boardView.getWidth() < this.boardView.getHeight()) ? this.boardView
                .getWidth() : this.boardView.getHeight();
        float newScale = getRenderer().fitTo(v);
        this.prefs.edit().putFloat(SCALE, newScale).apply();
        boardView.setCurrentScale(newScale);
        this.render(true);
    }

    @Override
    protected void onTimerUpdate() {
        super.onTimerUpdate();

        Puzzle puz = getPuzzle();
        ImaginaryTimer timer = getTimer();

        if (puz != null && timer != null) {
            getWindow().setTitle(timer.time());
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        this.render(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        keyboardManager.onPause();

        Playboard board = getBoard();
        if (board != null)
            board.removeListener(this);

        if (clueTabs != null) {
            clueTabs.removeListener(this);
            clueTabs.unlistenBoard();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Playboard board = getBoard();

        if (board != null) {
            board.setSkipCompletedLetters(this.prefs.getBoolean("skipFilled", false));
            board.setMovementStrategy(this.getMovementStrategy());
            board.addListener(this);
        }

        this.showCount = prefs.getBoolean("showCount", false);
        this.onConfigurationChanged(getBaseContext().getResources()
                                                    .getConfiguration());

        if (clueTabs != null) {
            clueTabs.addListener(this);
            clueTabs.listenBoard();
            clueTabs.refresh();

            if (clueTabs.getBoardDelMeNotReallyNeeded() != board) {
                Toast t = Toast.makeText(this,
                                         "Clue tabs board not match app",
                                         Toast.LENGTH_SHORT);
                t.show();
            }

            if (prefs.getBoolean(SHOW_CLUES_TAB, false)) {
                showClueTabs();
            } else {
                hideClueTabs();
            }
        }

        render(true);

        keyboardManager.onResume();
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

    private void setClueSize(int dps) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            this.clue.setAutoSizeTextTypeUniformWithConfiguration(
                5, dps, 1, TypedValue.COMPLEX_UNIT_SP
            );
        }

        if (prefs.getInt("clueSize", 12) != dps) {
            this.prefs.edit().putInt("clueSize", dps).apply();
        }
    }

    private MovementStrategy getMovementStrategy() {
        if (movement != null) {
            return movement;
        } else {
            String stratName = this.prefs.getString("movementStrategy",
                    "MOVE_NEXT_ON_AXIS");
            switch (stratName) {
                case "MOVE_NEXT_ON_AXIS":
                    movement = MovementStrategy.MOVE_NEXT_ON_AXIS;
                    break;
                case "STOP_ON_END":
                    movement = MovementStrategy.STOP_ON_END;
                    break;
                case "MOVE_NEXT_CLUE":
                    movement = MovementStrategy.MOVE_NEXT_CLUE;
                    break;
                case "MOVE_PARALLEL_WORD":
                    movement = MovementStrategy.MOVE_PARALLEL_WORD;
                    break;
            }

            return movement;
        }
    }

    /**
     * Change keyboard display if the same word has been selected twice
     */
    private void displayKeyboard(Word previous) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Playboard board = getBoard();
        if (board != null) {
            Position newPos = board.getHighlightLetter();
            if ((previous != null) &&
                previous.checkInWord(newPos.across, newPos.down)) {
                keyboardManager.showKeyboard(boardView);
            } else {
                keyboardManager.hideKeyboard();
            }
        }
    }

    private void render() {
        render(null);
    }

    private void render(boolean rescale) {
        this.render(null, rescale);
    }

    private void render(Word previous) {
        this.render(previous, false);
    }

    private void render(Word previous, boolean rescale) {
        Clue c = getBoard().getClue();
        if (c.hint == null) {
            getBoard().toggleDirection();
            return;
        }

        boolean displayScratch = this.prefs.getBoolean("displayScratch", false);
        this.boardView.setBitmap(getRenderer().draw(previous,
                                                    displayScratch, displayScratch),
                                 rescale);
        this.boardView.requestFocus();
        /*
		 * If we jumped to a new word, ensure the first letter is visible.
		 * Otherwise, insure that the current letter is visible. Only necessary
		 * if the cursor is currently off screen.
		 */
        if (rescale && this.prefs.getBoolean("ensureVisible", true)) {
            Point topLeft;
            Point bottomRight;
            Point cursorTopLeft;
            Point cursorBottomRight;
            cursorTopLeft = getRenderer().findPointTopLeft(getBoard()
                    .getHighlightLetter());
            cursorBottomRight = getRenderer().findPointBottomRight(getBoard()
                    .getHighlightLetter());

            if ((previous != null) && previous.equals(getBoard().getCurrentWord())) {
                topLeft = cursorTopLeft;
                bottomRight = cursorBottomRight;
            } else {
                topLeft = getRenderer()
                        .findPointTopLeft(getBoard().getCurrentWordStart());
                bottomRight = getRenderer().findPointBottomRight(getBoard()
                        .getCurrentWordStart());
            }

            int tlDistance = cursorTopLeft.distance(topLeft);
            int brDistance = cursorBottomRight.distance(bottomRight);

            if (!this.boardView.isVisible(topLeft) && (tlDistance < brDistance)) {
                this.boardView.ensureVisible(topLeft);
            }

            if (!this.boardView.isVisible(bottomRight)
                    && (brDistance < tlDistance)) {
                this.boardView.ensureVisible(bottomRight);
            }

            // ensure the cursor is always on the screen.
            this.boardView.ensureVisible(cursorBottomRight);
            this.boardView.ensureVisible(cursorTopLeft);

        }

        this.clue
                .setText("("
                        + (getBoard().isAcross() ? "across" : "down")
                        + ") "
                        + c.number
                        + ". "
                        + c.hint
                        + (this.showCount ? ("  ["
                        + getBoard().getCurrentWord().length + "]") : ""));

        this.boardView.requestFocus();
    }

    private void launchNotes() {
        Intent i = new Intent(this, NotesActivity.class);
        this.startActivityForResult(i, 0);
    }

    private void launchClueList() {
        Intent i = new Intent(this, ClueListActivity.class);
        PlayActivity.this.startActivityForResult(i, 0);
    }

    /**
     * Changes the constraints on clue tabs to show.
     *
     * Call render(true) after to rescale board. Updates shared prefs.
     */
    private void showClueTabs() {
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.setVisibility(clueTabs.getId(), ConstraintSet.VISIBLE);
        set.applyTo(constraintLayout);

        clueTabs.setPage(prefs.getInt(CLUE_TABS_PAGE, 0));

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SHOW_CLUES_TAB, true);
        editor.apply();
    }

    /**
     * Changes the constraints on clue tabs to hide.
     *
     * Call render(true) after to rescale board. Updates shared prefs.
     */
    private void hideClueTabs() {
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.setVisibility(clueTabs.getId(), ConstraintSet.GONE);
        set.applyTo(constraintLayout);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SHOW_CLUES_TAB, false);
        editor.apply();
    }

    private void showInfoDialog() {
        DialogFragment dialog = new InfoDialog();
        dialog.show(getSupportFragmentManager(), "InfoDialog");
    }

    private void showRevealPuzzleDialog() {
        DialogFragment dialog = new RevealPuzzleDialog();
        dialog.show(getSupportFragmentManager(), "RevealPuzzleDialog");
    }

    public static class InfoDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder
                = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.puzzle_info_dialog, null);

            PlayActivity activity = (PlayActivity) getActivity();

            Puzzle puz = activity.getPuzzle();
            if (puz != null) {
                TextView title = view.findViewById(R.id.puzzle_info_title);
                title.setText(puz.getTitle());

                TextView author = view.findViewById(R.id.puzzle_info_author);
                author .setText(puz.getAuthor());

                TextView copyright
                    = view.findViewById(R.id.puzzle_info_copyright);
                copyright.setText(puz.getCopyright());

                TextView time = view.findViewById(R.id.puzzle_info_time);

                ImaginaryTimer timer = activity.getTimer();
                if (timer != null) {
                    timer.stop();
                    time.setText(getString(
                        R.string.elapsed_time, timer.time()
                    ));
                    timer.start();
                } else {
                    time.setText(getString(
                        R.string.elapsed_time,
                        new ImaginaryTimer(puz.getTime()).time()
                    ));
                }

                ProgressBar progress = (ProgressBar) view
                        .findViewById(R.id.puzzle_info_progress);
                progress.setProgress(puz.getPercentComplete());

                TextView filename
                    = view.findViewById(R.id.puzzle_info_filename);
                FileHandler fileHandler
                    = ForkyzApplication.getInstance().getFileHandler();
                filename.setText(
                    fileHandler.getUri(activity.getBaseFile()).toString()
                );

                addNotes(view);
            }

            builder.setView(view);

            return builder.create();
        }

        private void addNotes(View dialogView) {
            TextView view = dialogView.findViewById(R.id.puzzle_info_notes);

            Puzzle puz = ((PlayActivity) getActivity()).getPuzzle();
            if (puz == null)
                return;

            String puzNotes = puz.getNotes();
            if (puzNotes == null)
                puzNotes = "";

            final String notes = puzNotes;

            String[] split =
                notes.split("(?i:(?m:^\\s*Across:?\\s*$|^\\s*\\d))", 2);

            final String text = (split.length > 1) ? split[0].trim() : null;

            String tapShow = getString(R.string.tap_to_show_full_notes);
            if (text != null && text.length() > 0)
                view.setText(text + "\n(" + tapShow + ")");
            else
                view.setText("(" + tapShow + ")");

            view.setOnClickListener(new OnClickListener() {
                private boolean showAll = true;
                private String tapShow
                    = getString(R.string.tap_to_show_full_notes);
                private String tapHide
                    = getString(R.string.tap_to_hide_full_notes);

                public void onClick(View view) {
                    TextView tv = (TextView) view;

                    if (showAll)
                        tv.setText(notes + "\n(" + tapHide + ")");
                    else if (text == null || text.length() == 0)
                        tv.setText("(" + tapShow + ")");
                    else
                        tv.setText(text + "\n(" + tapShow + ")");

                    showAll = !showAll;
                }
            });
        }
    }

    public static class RevealPuzzleDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder
                = new AlertDialog.Builder(getActivity());

            builder.setTitle(getString(R.string.reveal_puzzle))
                .setMessage(getString(R.string.are_you_sure))
                .setPositiveButton(
                    R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            PlayActivity activity
                                = (PlayActivity) getActivity();
                            activity.getBoard().revealPuzzle();
                        }
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }
                );

            return builder.create();
        }
    }

    @SuppressWarnings("deprecation")
    private void setFullScreenMode() {
        if (prefs.getBoolean("fullScreen", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController insetsController
                    = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars());
                }
            } else {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }
}
