package io.freemytv;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.view.KeyEvent;

/**
 * The Free My TV screensaver (enabled in Free My TV mode via `settings put secure screensaver_components`). The system
 * still decides when it starts and when the TV turns off (e.g. after 10 min idle, then 2 h later), as before: the app
 * doesn't change those timings. Any key exits; with Watch from the screensaver on (Home Settings), OK first opens the
 * title on screen in its app.
 */
public class Screensaver extends DreamService {
    private DreamView view;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Watchdog.install(this);
        Ui.init(this);
        setInteractive(Prefs.saverWatch());   // interactive: the keys come here instead of waking the TV
        setFullscreen(true);
        setScreenBright(true);
        view = new DreamView(this);
        setContentView(view);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (!isInteractive()) return super.dispatchKeyEvent(event);
        int code = event.getKeyCode();
        boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
        // OK acts when it's released, so the app it opens doesn't get the release; any other key closes at once.
        if (ok ? event.getAction() == KeyEvent.ACTION_UP : event.getAction() == KeyEvent.ACTION_DOWN) {
            if (ok && view != null) view.watch();
            finish();
        }
        return true;
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        if (view != null) view.start();
    }

    @Override public void onDreamingStopped() {
        if (view != null) view.stop();
        super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        if (view != null) view.stop();
        super.onDetachedFromWindow();
    }

    /** True when the TV's screensaver is ours (or none is set): then the preview shows the same thing. */
    static boolean systemIsOurs(Context c) {
        String s = Settings.Secure.getString(c.getContentResolver(), "screensaver_components");
        return s == null || s.trim().isEmpty() || s.contains(c.getPackageName() + "/");
    }

    /**
     * Starts the screensaver the TV has set, as its idle timer would: ours, or the TV's own with screensaver=false
     * (Google's Ambient Mode, which launcherx opened with Back on its home screen). SystemUI's Somnambulator is the
     * exported way in; false when the TV has none.
     */
    static boolean startSystem(Context c) {
        try {
            c.startActivity(new Intent(Intent.ACTION_MAIN)
                    .setClassName("com.android.systemui", "com.android.systemui.Somnambulator")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (RuntimeException e) {
            AppLog.w("screensaver: can't start the system's: " + e);
            return false;
        }
    }
}
