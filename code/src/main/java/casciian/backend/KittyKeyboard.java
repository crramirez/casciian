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
package casciian.backend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import casciian.TKeypress;

import static casciian.TKeypress.kbBackspaceDel;

/**
 * Support for the Kitty keyboard protocol, also known as "disambiguated
 * keys" or the <code>CSI u</code> format.
 *
 * <p>The protocol is a progressive enhancement: the application pushes a
 * flags entry onto the terminal's keyboard mode stack with
 * {@link #ENABLE}, and pops it again with {@link #DISABLE}.  Terminals that
 * do not implement the protocol ignore both sequences and keep emitting
 * legacy VT/ASCII sequences, so the legacy parser must stay in place.</p>
 *
 * <p>While enabled, keystrokes that legacy encodings cannot express
 * unambiguously arrive as:</p>
 *
 * <pre>
 *     CSI keycode [:shifted[:base]] [; modifiers[:event-type]] [; text] u
 * </pre>
 *
 * <p>where <code>keycode</code> is the Unicode code point of the key on the
 * unshifted layout (or a functional-key identifier from the Unicode private
 * use area), and <code>modifiers</code> is <code>1 + bitmask</code>.  So
 * Ctrl-I is <code>ESC [ 105 ; 5 u</code> and stays distinct from Tab
 * (<code>0x09</code>), and Shift-Enter is <code>ESC [ 13 ; 2 u</code>.</p>
 *
 * <p>This class is deliberately free of any terminal or widget state so
 * that the wire format can be unit-tested on its own.</p>
 */
public final class KittyKeyboard {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Push the "disambiguate escape codes" flag (1) onto the terminal's
     * keyboard mode stack.  Emitted at terminal initialization.
     */
    public static final String ENABLE = "\033[>1u";

    /**
     * Pop the flags entry pushed by {@link #ENABLE}, returning the terminal
     * to whatever keyboard mode it was in before.  Emitted at teardown.
     */
    public static final String DISABLE = "\033[<u";

    /**
     * Ask the terminal to report its current keyboard mode flags.  A
     * terminal that implements the protocol replies with
     * <code>CSI ? flags u</code>; a terminal that does not implement it, or
     * has it turned off (several terminals, including WezTerm, ship the
     * protocol disabled by default and require an explicit opt-in), simply
     * ignores this and sends nothing back.  Since silence is the expected
     * response from most terminals in practice, detecting support requires
     * pairing this with a sentinel request that is guaranteed to draw a
     * reply; see {@code ECMA48Terminal.enableKittyKeyboard()}.
     */
    public static final String QUERY = "\033[?u";

    /**
     * Whether a terminal has been observed to honor the Kitty keyboard
     * protocol.  Applications can use this to adjust their UI, for example
     * hiding a Ctrl+I accelerator hint on a terminal that will only ever
     * deliver plain Tab for that key combination.
     */
    public enum SupportState {
        /**
         * No response has arrived yet, and the sentinel that would prove a
         * lack of support has not arrived either.  This is the state for a
         * brief window right after connecting.
         */
        UNKNOWN,

        /**
         * The terminal replied to {@link #QUERY} with its current flags,
         * proving it actively honors the protocol right now.
         */
        SUPPORTED,

        /**
         * A sentinel response that every terminal is expected to send
         * arrived without a prior reply to {@link #QUERY}.  Either the
         * terminal does not implement the protocol, or it implements it but
         * has it turned off; both look identical from the wire, and both
         * mean keystrokes will not be disambiguated right now.
         */
        UNSUPPORTED,
    }

    /**
     * Modifier bit for Shift.
     */
    public static final int MOD_SHIFT = 1;

    /**
     * Modifier bit for Alt (Option).
     */
    public static final int MOD_ALT = 2;

    /**
     * Modifier bit for Ctrl.
     */
    public static final int MOD_CTRL = 4;

    /**
     * Modifier bit for Super (Windows/Command).
     */
    public static final int MOD_SUPER = 8;

    /**
     * Modifier bit for Hyper.
     */
    public static final int MOD_HYPER = 16;

    /**
     * Modifier bit for Meta.  Casciian folds this into ALT, matching the
     * metaSendsEscape behavior of the legacy parser.
     */
    public static final int MOD_META = 32;

    /**
     * Modifier bit for Caps Lock being active.
     */
    public static final int MOD_CAPS_LOCK = 64;

    /**
     * Modifier bit for Num Lock being active.
     */
    public static final int MOD_NUM_LOCK = 128;

    /**
     * First code point of the private use area block that the protocol uses
     * for functional keys.
     */
    private static final int PUA_FIRST = 57344;

    /**
     * Last code point of the private use area block that the protocol uses
     * for functional keys.
     */
    private static final int PUA_LAST = 57454;

    /**
     * Functional key identifier for KP_0.  KP_1 through KP_9 follow it.
     */
    private static final int KP_0 = 57399;

    // ------------------------------------------------------------------------
    // KittyKeyboard ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The kind of key event reported by the <code>:event-type</code>
     * sub-parameter.
     */
    public enum EventType {
        /**
         * Key was pressed (<code>:1</code>, and the default when the
         * sub-parameter is absent).
         */
        PRESS,

        /**
         * Key auto-repeated (<code>:2</code>).
         */
        REPEAT,

        /**
         * Key was released (<code>:3</code>).
         */
        RELEASE,
    }

    /**
     * One decoded <code>CSI u</code> keystroke.
     *
     * @param key the keystroke, with Casciian's alt/ctrl/shift flags set
     * @param type press, repeat, or release
     * @param modifiers the raw modifier bitmask (already decremented by 1),
     * which retains Super/Hyper/lock state that {@link TKeypress} cannot
     * represent
     */
    public record KeyEvent(TKeypress key, EventType type, int modifiers) {

        /**
         * Whether this is a key release event.  Casciian has no release
         * events, so callers normally drop these.
         *
         * @return true if the key was released
         */
        public boolean isRelease() {
            return type == EventType.RELEASE;
        }

        /**
         * Whether the Super (Windows/Command) modifier was held.
         *
         * @return true if Super was down
         */
        public boolean isSuper() {
            return (modifiers & MOD_SUPER) != 0;
        }

        /**
         * Whether the Hyper modifier was held.
         *
         * @return true if Hyper was down
         */
        public boolean isHyper() {
            return (modifiers & MOD_HYPER) != 0;
        }
    }

    /**
     * Private constructor: this is a utility class.
     */
    private KittyKeyboard() {
    }

    /**
     * Decode a complete <code>CSI u</code> sequence.
     *
     * @param sequence the sequence, with or without the leading CSI, for
     * example "\033[105;5u", "105;5u", or "105;5u"
     * @return the decoded keystroke, or null if the sequence is malformed or
     * names a key Casciian has no representation for
     */
    public static KeyEvent parse(final String sequence) {
        if (sequence == null) {
            return null;
        }
        String body = sequence;
        if (body.startsWith("\033[")) {
            body = body.substring(2);
        } else if (body.startsWith("")) {
            body = body.substring(1);
        }
        if (!body.endsWith("u")) {
            return null;
        }
        body = body.substring(0, body.length() - 1);
        if (body.isEmpty()) {
            return null;
        }
        return parse(new ArrayList<String>(Arrays.asList(body.split(";",
            -1))));
    }

    /**
     * Decode the parameters of a <code>CSI u</code> sequence.
     *
     * @param params the semicolon-separated parameters, colon sub-parameters
     * left intact, for example ["105", "5"] or ["13", "2:1"]
     * @return the decoded keystroke, or null if the parameters are malformed
     * or name a key Casciian has no representation for
     */
    public static KeyEvent parse(final List<String> params) {
        if ((params == null) || params.isEmpty()) {
            return null;
        }

        int[] keyField = subParams(params.getFirst());
        if ((keyField.length == 0) || (keyField[0] < 0)) {
            return null;
        }
        int keyCode = keyField[0];
        int shiftedCode = (keyField.length > 1 ? keyField[1] : -1);

        int modifiers = 0;
        EventType type = EventType.PRESS;
        if (params.size() > 1) {
            int[] modifierField = subParams(params.get(1));
            if ((modifierField.length > 0) && (modifierField[0] > 0)) {
                modifiers = modifierField[0] - 1;
            }
            if ((modifierField.length > 1) && (modifierField[1] >= 0)) {
                type = eventType(modifierField[1]);
                if (type == null) {
                    // Unknown event type, ignore the whole sequence rather
                    // than guess.
                    return null;
                }
            }
        }

        TKeypress key = toKeypress(keyCode, shiftedCode, modifiers);
        if (key == null) {
            return null;
        }
        return new KeyEvent(key, type, modifiers);
    }

    /**
     * Whether the Shift modifier is set in a decoded bitmask.
     *
     * @param modifiers the bitmask, already decremented by 1
     * @return true if Shift was down
     */
    public static boolean isShift(final int modifiers) {
        return (modifiers & MOD_SHIFT) != 0;
    }

    /**
     * Whether the Alt modifier is set in a decoded bitmask.  Meta is folded
     * into Alt to match the rest of the ECMA-48 input path.
     *
     * @param modifiers the bitmask, already decremented by 1
     * @return true if Alt or Meta was down
     */
    public static boolean isAlt(final int modifiers) {
        return (modifiers & (MOD_ALT | MOD_META)) != 0;
    }

    /**
     * Whether the Ctrl modifier is set in a decoded bitmask.
     *
     * @param modifiers the bitmask, already decremented by 1
     * @return true if Ctrl was down
     */
    public static boolean isCtrl(final int modifiers) {
        return (modifiers & MOD_CTRL) != 0;
    }

    /**
     * Map an event-type sub-parameter to its enum value.
     *
     * @param value the sub-parameter value
     * @return the event type, or null if the value is not one of 1, 2, or 3
     */
    private static EventType eventType(final int value) {
        return switch (value) {
            case 1 -> EventType.PRESS;
            case 2 -> EventType.REPEAT;
            case 3 -> EventType.RELEASE;
            default -> null;
        };
    }

    /**
     * Split a parameter into its colon-separated sub-parameters.  Empty
     * sub-parameters (as in "97::65") and non-numeric junk both come back as
     * -1 so that callers can fall back to a default.
     *
     * @param param the parameter text
     * @return the sub-parameter values, never null but possibly empty
     */
    private static int[] subParams(final String param) {
        if ((param == null) || param.isEmpty()) {
            return new int[0];
        }
        String[] fields = param.split(":", -1);
        int[] values = new int[fields.length];
        for (int i = 0; i < fields.length; i++) {
            try {
                values[i] = Integer.parseInt(fields[i]);
            } catch (NumberFormatException e) {
                values[i] = -1;
            }
        }
        return values;
    }

    /**
     * Turn a decoded key code and modifier bitmask into a TKeypress.
     *
     * <p>The raw key identity is preserved: code point 105 with Ctrl becomes
     * Ctrl-I, and does <em>not</em> collapse into Tab the way the legacy
     * 0x09 encoding does.</p>
     *
     * @param keyCode the Unicode code point or functional key identifier
     * @param shiftedCode the shifted code point sub-parameter, or -1
     * @param modifiers the modifier bitmask, already decremented by 1
     * @return the keystroke, or null if Casciian has no representation for it
     */
    private static TKeypress toKeypress(final int keyCode,
        final int shiftedCode, final int modifiers) {

        boolean shift = isShift(modifiers);
        boolean alt = isAlt(modifiers);
        boolean ctrl = isCtrl(modifiers);

        switch (keyCode) {
        case 9:
            return new TKeypress(true, TKeypress.TAB, ' ', alt, ctrl, shift);
        case 13:
            return new TKeypress(true, TKeypress.ENTER, ' ', alt, ctrl,
                shift);
        case 27:
            return new TKeypress(true, TKeypress.ESC, ' ', alt, ctrl, shift);
        case 8:
        case 127:
            if (!alt && !ctrl && !shift) {
                // Plain backspace keeps its legacy ^? identity so that
                // existing widgets continue to match on it.
                return kbBackspaceDel.dup();
            }
            return new TKeypress(true, TKeypress.BACKSPACE, ' ', alt, ctrl,
                shift);
        default:
            break;
        }

        if ((keyCode >= PUA_FIRST) && (keyCode <= PUA_LAST)) {
            return functionalKey(keyCode, alt, ctrl, shift);
        }

        if ((keyCode < 32) || (keyCode > Character.MAX_CODE_POINT)) {
            // C0 controls never appear as CSI u key codes, and anything
            // beyond Unicode is nonsense.
            return null;
        }

        int ch = keyCode;
        if (shift && (shiftedCode > 0)) {
            // The terminal told us what the shifted layout produces.
            ch = shiftedCode;
        } else if ((shift || ctrl) && (ch >= 'a') && (ch <= 'z')) {
            // Casciian spells Ctrl-I and Shift-Z with the uppercase letter.
            ch -= 32;
        }
        return new TKeypress(false, 0, ch, alt, ctrl, shift);
    }

    /**
     * Map a private use area functional key identifier to a TKeypress.
     *
     * @param keyCode the functional key identifier
     * @param alt true if Alt was down
     * @param ctrl true if Ctrl was down
     * @param shift true if Shift was down
     * @return the keystroke, or null for keys Casciian does not model (F13
     * and above, lock keys, media keys, and the modifier keys themselves)
     */
    private static TKeypress functionalKey(final int keyCode,
        final boolean alt, final boolean ctrl, final boolean shift) {

        if ((keyCode >= KP_0) && (keyCode <= KP_0 + 9)) {
            // Keypad digits report as their digit.
            return new TKeypress(false, 0, '0' + (keyCode - KP_0), alt, ctrl,
                shift);
        }

        return switch (keyCode) {
            // Keypad operators.
            case 57409 -> new TKeypress(false, 0, '.', alt, ctrl, shift);
            case 57410 -> new TKeypress(false, 0, '/', alt, ctrl, shift);
            case 57411 -> new TKeypress(false, 0, '*', alt, ctrl, shift);
            case 57412 -> new TKeypress(false, 0, '-', alt, ctrl, shift);
            case 57413 -> new TKeypress(false, 0, '+', alt, ctrl, shift);
            case 57414 -> new TKeypress(true, TKeypress.ENTER, ' ', alt, ctrl,
                shift);
            case 57415 -> new TKeypress(false, 0, '=', alt, ctrl, shift);
            case 57416 -> new TKeypress(false, 0, ',', alt, ctrl, shift);
            // Keypad navigation (Num Lock off).
            case 57417 -> new TKeypress(true, TKeypress.LEFT, ' ', alt, ctrl,
                shift);
            case 57418 -> new TKeypress(true, TKeypress.RIGHT, ' ', alt, ctrl,
                shift);
            case 57419 -> new TKeypress(true, TKeypress.UP, ' ', alt, ctrl,
                shift);
            case 57420 -> new TKeypress(true, TKeypress.DOWN, ' ', alt, ctrl,
                shift);
            case 57421 -> new TKeypress(true, TKeypress.PGUP, ' ', alt, ctrl,
                shift);
            case 57422 -> new TKeypress(true, TKeypress.PGDN, ' ', alt, ctrl,
                shift);
            case 57423 -> new TKeypress(true, TKeypress.HOME, ' ', alt, ctrl,
                shift);
            case 57424 -> new TKeypress(true, TKeypress.END, ' ', alt, ctrl,
                shift);
            case 57425 -> new TKeypress(true, TKeypress.INS, ' ', alt, ctrl,
                shift);
            case 57427 -> new TKeypress(true, TKeypress.DEL, ' ', alt, ctrl,
                shift);
            default -> null;
        };
    }

}
