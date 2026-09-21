/*
 * Casciian - Java Text User Interface
 *
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package casciian;

/**
 * MouseAutoRepeat repeats an action while a mouse button is held down.
 *
 * <p>The action fires once immediately, then pauses for
 * {@link #INITIAL_DELAY_MILLIS} before repeating every
 * {@link #REPEAT_INTERVAL_MILLIS}.  This matches Swing's BasicScrollBarUI
 * (300ms initial delay, 60ms repeat) and Turbo Vision's evMouseAuto.</p>
 *
 * <p>Terminals do not resend mouse-down events while a button is held, so the
 * repeat has to be driven by a timer rather than by incoming events.</p>
 */
class MouseAutoRepeat {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Milliseconds to wait after the first action before repeating.
     */
    public static final long INITIAL_DELAY_MILLIS = 300;

    /**
     * Milliseconds between repeats once repeating has started.
     */
    public static final long REPEAT_INTERVAL_MILLIS = 60;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The widget the repeat is running on behalf of.
     */
    private TWidget owner;

    /**
     * The running timer, or null if not repeating.
     */
    private TTimer timer;

    // ------------------------------------------------------------------------
    // MouseAutoRepeat --------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Perform an action now, and keep performing it until stopped.
     *
     * <p>If the owner is not attached to a TApplication the action still fires
     * once, but no timer is scheduled.</p>
     *
     * @param owner the widget requesting the repeat
     * @param step the action to perform on each repeat
     */
    public void start(final TWidget owner, final TAction step) {
        stop();

        if ((owner == null) || (step == null)) {
            return;
        }

        // Resolve this before the first step, which is allowed to call stop()
        // and clear the owner out from under us.
        TApplication application = owner.getApplication();
        this.owner = owner;
        step.DO();

        if ((application == null) || (this.owner == null)) {
            // Either nothing to schedule on, or the step already asked to
            // stop because it had nowhere left to scroll.
            this.owner = null;
            return;
        }

        timer = application.addTimer(INITIAL_DELAY_MILLIS, true, new TAction() {
            @Override
            public void DO() {
                if (!isOwnerLive()) {
                    stop();
                    return;
                }
                step.DO();
                if (timer != null) {
                    timer.setDuration(REPEAT_INTERVAL_MILLIS);
                }
            }
        });
    }

    /**
     * Stop repeating.  Safe to call when not repeating, and safe to call from
     * inside the repeating action itself.
     */
    public void stop() {
        if (timer != null) {
            // Clearing recurring is what actually cancels the timer:
            // TApplication.doIdle() re-reads the flag after the action runs and
            // would otherwise re-add a timer that removed itself.
            timer.setRecurring(false);
            TApplication application = (owner == null ? null
                : owner.getApplication());
            if (application != null) {
                application.removeTimer(timer);
            }
            timer = null;
        }
        owner = null;
    }

    /**
     * Returns true if a repeat timer is currently running.
     *
     * @return true if repeating
     */
    public boolean isActive() {
        return timer != null;
    }

    /**
     * Returns true if the owner is still able to act on a repeat.  Guards
     * against a timer outliving the widget or its window.
     *
     * @return true if the owner can still be stepped
     */
    private boolean isOwnerLive() {
        return (owner != null)
            && (owner.getApplication() != null)
            && (owner.getWindow() != null)
            && owner.isEnabled()
            && owner.isVisible();
    }

}
