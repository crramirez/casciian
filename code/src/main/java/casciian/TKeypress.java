/*
 * Casciian - Java Text User Interface
 *
 * Written 2013-2025 by Autumn Lamonte
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package casciian;

/**
 * This class represents keystrokes.
 */
public class TKeypress {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // Various special keystrokes

    /**
     * "No key".
     */
    public static final int NONE        = 255;

    /**
     * Function key F1.
     */
    public static final int F1          = 1;

    /**
     * Function key F2.
     */
    public static final int F2          = 2;

    /**
     * Function key F3.
     */
    public static final int F3          = 3;

    /**
     * Function key F4.
     */
    public static final int F4          = 4;

    /**
     * Function key F5.
     */
    public static final int F5          = 5;

    /**
     * Function key F6.
     */
    public static final int F6          = 6;

    /**
     * Function key F7.
     */
    public static final int F7          = 7;

    /**
     * Function key F8.
     */
    public static final int F8          = 8;

    /**
     * Function key F9.
     */
    public static final int F9          = 9;

    /**
     * Function key F10.
     */
    public static final int F10         = 10;

    /**
     * Function key F11.
     */
    public static final int F11         = 11;

    /**
     * Function key F12.
     */
    public static final int F12         = 12;

    /**
     * Home.
     */
    public static final int HOME        = 20;

    /**
     * End.
     */
    public static final int END         = 21;

    /**
     * Page up.
     */
    public static final int PGUP        = 22;

    /**
     * Page down.
     */
    public static final int PGDN        = 23;

    /**
     * Insert.
     */
    public static final int INS         = 24;

    /**
     * Delete.
     */
    public static final int DEL         = 25;

    /**
     * Right arrow.
     */
    public static final int RIGHT       = 30;

    /**
     * Left arrow.
     */
    public static final int LEFT        = 31;

    /**
     * Up arrow.
     */
    public static final int UP          = 32;

    /**
     * Down arrow.
     */
    public static final int DOWN        = 33;

    /**
     * Tab.
     */
    public static final int TAB         = 40;

    /**
     * Back-tab (shift-tab).
     */
    public static final int BTAB        = 41;

    /**
     * Enter.
     */
    public static final int ENTER       = 42;

    /**
     * Escape.
     */
    public static final int ESC         = 43;

    /**
     * Backspace, used for control-backspace.
     */
    public static final int BACKSPACE   = 44;

    /**
     * Special "no-key" keypress, used to ignore undefined keystrokes.
     */
    public static final TKeypress kbNoKey = new TKeypress(true,
            TKeypress.NONE, ' ', false, false, false);

    /**
     * F1
     */
    public static final TKeypress kbF1 = new TKeypress(true,
            TKeypress.F1, ' ', false, false, false);
    /**
     * F2
     */
    public static final TKeypress kbF2 = new TKeypress(true,
            TKeypress.F2, ' ', false, false, false);
    /**
     * F3
     */
    public static final TKeypress kbF3 = new TKeypress(true,
            TKeypress.F3, ' ', false, false, false);
    /**
     * F4
     */
    public static final TKeypress kbF4 = new TKeypress(true,
            TKeypress.F4, ' ', false, false, false);
    /**
     * F5
     */
    public static final TKeypress kbF5 = new TKeypress(true,
            TKeypress.F5, ' ', false, false, false);
    /**
     * F6
     */
    public static final TKeypress kbF6 = new TKeypress(true,
            TKeypress.F6, ' ', false, false, false);
    /**
     * F7
     */
    public static final TKeypress kbF7 = new TKeypress(true,
            TKeypress.F7, ' ', false, false, false);
    /**
     * F8
     */
    public static final TKeypress kbF8 = new TKeypress(true,
            TKeypress.F8, ' ', false, false, false);
    /**
     * F9
     */
    public static final TKeypress kbF9 = new TKeypress(true,
            TKeypress.F9, ' ', false, false, false);
    /**
     * F10
     */
    public static final TKeypress kbF10 = new TKeypress(true,
            TKeypress.F10, ' ', false, false, false);
    /**
     * F11
     */
    public static final TKeypress kbF11 = new TKeypress(true,
            TKeypress.F11, ' ', false, false, false);
    /**
     * F12
     */
    public static final TKeypress kbF12 = new TKeypress(true,
            TKeypress.F12, ' ', false, false, false);
    /**
     * Alt-F1
     */
    public static final TKeypress kbAltF1 = new TKeypress(true,
            TKeypress.F1, ' ', true, false, false);
    /**
     * Alt-F2
     */
    public static final TKeypress kbAltF2 = new TKeypress(true,
            TKeypress.F2, ' ', true, false, false);
    /**
     * Alt-F3
     */
    public static final TKeypress kbAltF3 = new TKeypress(true,
            TKeypress.F3, ' ', true, false, false);
    /**
     * Alt-F4
     */
    public static final TKeypress kbAltF4 = new TKeypress(true,
            TKeypress.F4, ' ', true, false, false);
    /**
     * Alt-F5
     */
    public static final TKeypress kbAltF5 = new TKeypress(true,
            TKeypress.F5, ' ', true, false, false);
    /**
     * Alt-F6
     */
    public static final TKeypress kbAltF6 = new TKeypress(true,
            TKeypress.F6, ' ', true, false, false);
    /**
     * Alt-F7
     */
    public static final TKeypress kbAltF7 = new TKeypress(true,
            TKeypress.F7, ' ', true, false, false);
    /**
     * Alt-F8
     */
    public static final TKeypress kbAltF8 = new TKeypress(true,
            TKeypress.F8, ' ', true, false, false);
    /**
     * Alt-F9
     */
    public static final TKeypress kbAltF9 = new TKeypress(true,
            TKeypress.F9, ' ', true, false, false);
    /**
     * Alt-F10
     */
    public static final TKeypress kbAltF10 = new TKeypress(true,
            TKeypress.F10, ' ', true, false, false);
    /**
     * Alt-F11
     */
    public static final TKeypress kbAltF11 = new TKeypress(true,
            TKeypress.F11, ' ', true, false, false);
    /**
     * Alt-F12
     */
    public static final TKeypress kbAltF12 = new TKeypress(true,
            TKeypress.F12, ' ', true, false, false);
    /**
     * Ctrl-F1
     */
    public static final TKeypress kbCtrlF1 = new TKeypress(true,
            TKeypress.F1, ' ', false, true, false);
    /**
     * Ctrl-F2
     */
    public static final TKeypress kbCtrlF2 = new TKeypress(true,
            TKeypress.F2, ' ', false, true, false);
    /**
     * Ctrl-F3
     */
    public static final TKeypress kbCtrlF3 = new TKeypress(true,
            TKeypress.F3, ' ', false, true, false);
    /**
     * Ctrl-F4
     */
    public static final TKeypress kbCtrlF4 = new TKeypress(true,
            TKeypress.F4, ' ', false, true, false);
    /**
     * Ctrl-F5
     */
    public static final TKeypress kbCtrlF5 = new TKeypress(true,
            TKeypress.F5, ' ', false, true, false);
    /**
     * Ctrl-F6
     */
    public static final TKeypress kbCtrlF6 = new TKeypress(true,
            TKeypress.F6, ' ', false, true, false);
    /**
     * Ctrl-F7
     */
    public static final TKeypress kbCtrlF7 = new TKeypress(true,
            TKeypress.F7, ' ', false, true, false);
    /**
     * Ctrl-F8
     */
    public static final TKeypress kbCtrlF8 = new TKeypress(true,
            TKeypress.F8, ' ', false, true, false);
    /**
     * Ctrl-F9
     */
    public static final TKeypress kbCtrlF9 = new TKeypress(true,
            TKeypress.F9, ' ', false, true, false);
    /**
     * Ctrl-F10
     */
    public static final TKeypress kbCtrlF10 = new TKeypress(true,
            TKeypress.F10, ' ', false, true, false);
    /**
     * Ctrl-F11
     */
    public static final TKeypress kbCtrlF11 = new TKeypress(true,
            TKeypress.F11, ' ', false, true, false);
    /**
     * Ctrl-F12
     */
    public static final TKeypress kbCtrlF12 = new TKeypress(true,
            TKeypress.F12, ' ', false, true, false);
    /**
     * Shift-F1
     */
    public static final TKeypress kbShiftF1 = new TKeypress(true,
            TKeypress.F1, ' ', false, false, true);
    /**
     * Shift-F2
     */
    public static final TKeypress kbShiftF2 = new TKeypress(true,
            TKeypress.F2, ' ', false, false, true);
    /**
     * Shift-F3
     */
    public static final TKeypress kbShiftF3 = new TKeypress(true,
            TKeypress.F3, ' ', false, false, true);
    /**
     * Shift-F4
     */
    public static final TKeypress kbShiftF4 = new TKeypress(true,
            TKeypress.F4, ' ', false, false, true);
    /**
     * Shift-F5
     */
    public static final TKeypress kbShiftF5 = new TKeypress(true,
            TKeypress.F5, ' ', false, false, true);
    /**
     * Shift-F6
     */
    public static final TKeypress kbShiftF6 = new TKeypress(true,
            TKeypress.F6, ' ', false, false, true);
    /**
     * Shift-F7
     */
    public static final TKeypress kbShiftF7 = new TKeypress(true,
            TKeypress.F7, ' ', false, false, true);
    /**
     * Shift-F8
     */
    public static final TKeypress kbShiftF8 = new TKeypress(true,
            TKeypress.F8, ' ', false, false, true);
    /**
     * Shift-F9
     */
    public static final TKeypress kbShiftF9 = new TKeypress(true,
            TKeypress.F9, ' ', false, false, true);
    /**
     * Shift-F10
     */
    public static final TKeypress kbShiftF10 = new TKeypress(true,
            TKeypress.F10, ' ', false, false, true);
    /**
     * Shift-F11
     */
    public static final TKeypress kbShiftF11 = new TKeypress(true,
            TKeypress.F11, ' ', false, false, true);
    /**
     * Shift-F12
     */
    public static final TKeypress kbShiftF12 = new TKeypress(true,
            TKeypress.F12, ' ', false, false, true);
    /**
     * Enter
     */
    public static final TKeypress kbEnter = new TKeypress(true,
            TKeypress.ENTER, ' ', false, false, false);
    /**
     * Tab
     */
    public static final TKeypress kbTab = new TKeypress(true,
            TKeypress.TAB, ' ', false, false, false);
    /**
     * Esc
     */
    public static final TKeypress kbEsc = new TKeypress(true,
            TKeypress.ESC, ' ', false, false, false);
    /**
     * Home
     */
    public static final TKeypress kbHome = new TKeypress(true,
            TKeypress.HOME, ' ', false, false, false);
    /**
     * End
     */
    public static final TKeypress kbEnd = new TKeypress(true,
            TKeypress.END, ' ', false, false, false);
    /**
     * PgUp
     */
    public static final TKeypress kbPgUp = new TKeypress(true,
            TKeypress.PGUP, ' ', false, false, false);
    /**
     * PgDn
     */
    public static final TKeypress kbPgDn = new TKeypress(true,
            TKeypress.PGDN, ' ', false, false, false);
    /**
     * Ins
     */
    public static final TKeypress kbIns = new TKeypress(true,
            TKeypress.INS, ' ', false, false, false);
    /**
     * Del
     */
    public static final TKeypress kbDel = new TKeypress(true,
            TKeypress.DEL, ' ', false, false, false);
    /**
     * Up
     */
    public static final TKeypress kbUp = new TKeypress(true,
            TKeypress.UP, ' ', false, false, false);
    /**
     * Down
     */
    public static final TKeypress kbDown = new TKeypress(true,
            TKeypress.DOWN, ' ', false, false, false);
    /**
     * Left
     */
    public static final TKeypress kbLeft = new TKeypress(true,
            TKeypress.LEFT, ' ', false, false, false);
    /**
     * Right
     */
    public static final TKeypress kbRight = new TKeypress(true,
            TKeypress.RIGHT, ' ', false, false, false);
    /**
     * Alt-Enter
     */
    public static final TKeypress kbAltEnter = new TKeypress(true,
            TKeypress.ENTER, ' ', true, false, false);
    /**
     * Alt-Tab
     */
    public static final TKeypress kbAltTab = new TKeypress(true,
            TKeypress.TAB, ' ', true, false, false);
    /**
     * Alt-Esc
     */
    public static final TKeypress kbAltEsc = new TKeypress(true,
            TKeypress.ESC, ' ', true, false, false);
    /**
     * Alt-Home
     */
    public static final TKeypress kbAltHome = new TKeypress(true,
            TKeypress.HOME, ' ', true, false, false);
    /**
     * Alt-End
     */
    public static final TKeypress kbAltEnd = new TKeypress(true,
            TKeypress.END, ' ', true, false, false);
    /**
     * Alt-PgUp
     */
    public static final TKeypress kbAltPgUp = new TKeypress(true,
            TKeypress.PGUP, ' ', true, false, false);
    /**
     * Alt-PgDn
     */
    public static final TKeypress kbAltPgDn = new TKeypress(true,
            TKeypress.PGDN, ' ', true, false, false);
    /**
     * Alt-Ins
     */
    public static final TKeypress kbAltIns = new TKeypress(true,
            TKeypress.INS, ' ', true, false, false);
    /**
     * Alt-Del
     */
    public static final TKeypress kbAltDel = new TKeypress(true,
            TKeypress.DEL, ' ', true, false, false);
    /**
     * Alt-Up
     */
    public static final TKeypress kbAltUp = new TKeypress(true,
            TKeypress.UP, ' ', true, false, false);
    /**
     * Alt-Down
     */
    public static final TKeypress kbAltDown = new TKeypress(true,
            TKeypress.DOWN, ' ', true, false, false);
    /**
     * Alt-Left
     */
    public static final TKeypress kbAltLeft = new TKeypress(true,
            TKeypress.LEFT, ' ', true, false, false);
    /**
     * Alt-Right
     */
    public static final TKeypress kbAltRight = new TKeypress(true,
            TKeypress.RIGHT, ' ', true, false, false);
    /**
     * Ctrl-Enter
     */
    public static final TKeypress kbCtrlEnter = new TKeypress(true,
            TKeypress.ENTER, ' ', false, true, false);
    /**
     * Ctrl-Tab
     */
    public static final TKeypress kbCtrlTab = new TKeypress(true,
            TKeypress.TAB, ' ', false, true, false);
    /**
     * Ctrl-Esc
     */
    public static final TKeypress kbCtrlEsc = new TKeypress(true,
            TKeypress.ESC, ' ', false, true, false);
    /**
     * Ctrl-Home
     */
    public static final TKeypress kbCtrlHome = new TKeypress(true,
            TKeypress.HOME, ' ', false, true, false);
    /**
     * Ctrl-End
     */
    public static final TKeypress kbCtrlEnd = new TKeypress(true,
            TKeypress.END, ' ', false, true, false);
    /**
     * Ctrl-PgUp
     */
    public static final TKeypress kbCtrlPgUp = new TKeypress(true,
            TKeypress.PGUP, ' ', false, true, false);
    /**
     * Ctrl-PgDn
     */
    public static final TKeypress kbCtrlPgDn = new TKeypress(true,
            TKeypress.PGDN, ' ', false, true, false);
    /**
     * Ctrl-Ins
     */
    public static final TKeypress kbCtrlIns = new TKeypress(true,
            TKeypress.INS, ' ', false, true, false);
    /**
     * Ctrl-Del
     */
    public static final TKeypress kbCtrlDel = new TKeypress(true,
            TKeypress.DEL, ' ', false, true, false);
    /**
     * Ctrl-Up
     */
    public static final TKeypress kbCtrlUp = new TKeypress(true,
            TKeypress.UP, ' ', false, true, false);
    /**
     * Ctrl-Down
     */
    public static final TKeypress kbCtrlDown = new TKeypress(true,
            TKeypress.DOWN, ' ', false, true, false);
    /**
     * Ctrl-Left
     */
    public static final TKeypress kbCtrlLeft = new TKeypress(true,
            TKeypress.LEFT, ' ', false, true, false);
    /**
     * Ctrl-Right
     */
    public static final TKeypress kbCtrlRight = new TKeypress(true,
            TKeypress.RIGHT, ' ', false, true, false);
    /**
     * Shift-Enter
     */
    public static final TKeypress kbShiftEnter = new TKeypress(true,
            TKeypress.ENTER, ' ', false, false, true);
    /**
     * Shift-Tab
     */
    public static final TKeypress kbShiftTab = new TKeypress(true,
            TKeypress.TAB, ' ', false, false, true);
    /**
     * Shift-BackTab
     */
    public static final TKeypress kbBackTab = new TKeypress(true,
            TKeypress.BTAB, ' ', false, false, false);
    /**
     * Shift-Esc
     */
    public static final TKeypress kbShiftEsc = new TKeypress(true,
            TKeypress.ESC, ' ', false, false, true);
    /**
     * Shift-Home
     */
    public static final TKeypress kbShiftHome = new TKeypress(true,
            TKeypress.HOME, ' ', false, false, true);
    /**
     * Shift-End
     */
    public static final TKeypress kbShiftEnd = new TKeypress(true,
            TKeypress.END, ' ', false, false, true);
    /**
     * Shift-PgUp
     */
    public static final TKeypress kbShiftPgUp = new TKeypress(true,
            TKeypress.PGUP, ' ', false, false, true);
    /**
     * Shift-PgDn
     */
    public static final TKeypress kbShiftPgDn = new TKeypress(true,
            TKeypress.PGDN, ' ', false, false, true);
    /**
     * Shift-Ins
     */
    public static final TKeypress kbShiftIns = new TKeypress(true,
            TKeypress.INS, ' ', false, false, true);
    /**
     * Shift-Del
     */
    public static final TKeypress kbShiftDel = new TKeypress(true,
            TKeypress.DEL, ' ', false, false, true);
    /**
     * Shift-Up
     */
    public static final TKeypress kbShiftUp = new TKeypress(true,
            TKeypress.UP, ' ', false, false, true);
    /**
     * Shift-Down
     */
    public static final TKeypress kbShiftDown = new TKeypress(true,
            TKeypress.DOWN, ' ', false, false, true);
    /**
     * Shift-Left
     */
    public static final TKeypress kbShiftLeft = new TKeypress(true,
            TKeypress.LEFT, ' ', false, false, true);
    /**
     * Shift-Right
     */
    public static final TKeypress kbShiftRight = new TKeypress(true,
            TKeypress.RIGHT, ' ', false, false, true);
    /**
     * A
     */
    public static final TKeypress kbA = new TKeypress(false,
            0, 'a', false, false, false);
    /**
     * B
     */
    public static final TKeypress kbB = new TKeypress(false,
            0, 'b', false, false, false);
    /**
     * C
     */
    public static final TKeypress kbC = new TKeypress(false,
            0, 'c', false, false, false);
    /**
     * D
     */
    public static final TKeypress kbD = new TKeypress(false,
            0, 'd', false, false, false);
    /**
     * E
     */
    public static final TKeypress kbE = new TKeypress(false,
            0, 'e', false, false, false);
    /**
     * F
     */
    public static final TKeypress kbF = new TKeypress(false,
            0, 'f', false, false, false);
    /**
     * G
     */
    public static final TKeypress kbG = new TKeypress(false,
            0, 'g', false, false, false);
    /**
     * H
     */
    public static final TKeypress kbH = new TKeypress(false,
            0, 'h', false, false, false);
    /**
     * I
     */
    public static final TKeypress kbI = new TKeypress(false,
            0, 'i', false, false, false);
    /**
     * J
     */
    public static final TKeypress kbJ = new TKeypress(false,
            0, 'j', false, false, false);
    /**
     * K
     */
    public static final TKeypress kbK = new TKeypress(false,
            0, 'k', false, false, false);
    /**
     * L
     */
    public static final TKeypress kbL = new TKeypress(false,
            0, 'l', false, false, false);
    /**
     * M
     */
    public static final TKeypress kbM = new TKeypress(false,
            0, 'm', false, false, false);
    /**
     * N
     */
    public static final TKeypress kbN = new TKeypress(false,
            0, 'n', false, false, false);
    /**
     * O
     */
    public static final TKeypress kbO = new TKeypress(false,
            0, 'o', false, false, false);
    /**
     * P
     */
    public static final TKeypress kbP = new TKeypress(false,
            0, 'p', false, false, false);
    /**
     * Q
     */
    public static final TKeypress kbQ = new TKeypress(false,
            0, 'q', false, false, false);
    /**
     * R
     */
    public static final TKeypress kbR = new TKeypress(false,
            0, 'r', false, false, false);
    /**
     * S
     */
    public static final TKeypress kbS = new TKeypress(false,
            0, 's', false, false, false);
    /**
     * T
     */
    public static final TKeypress kbT = new TKeypress(false,
            0, 't', false, false, false);
    /**
     * U
     */
    public static final TKeypress kbU = new TKeypress(false,
            0, 'u', false, false, false);
    /**
     * V
     */
    public static final TKeypress kbV = new TKeypress(false,
            0, 'v', false, false, false);
    /**
     * W
     */
    public static final TKeypress kbW = new TKeypress(false,
            0, 'w', false, false, false);
    /**
     * X
     */
    public static final TKeypress kbX = new TKeypress(false,
            0, 'x', false, false, false);
    /**
     * Y
     */
    public static final TKeypress kbY = new TKeypress(false,
            0, 'y', false, false, false);
    /**
     * Z
     */
    public static final TKeypress kbZ = new TKeypress(false,
            0, 'z', false, false, false);
    /**
     * Space (' ')
     */
    public static final TKeypress kbSpace = new TKeypress(false,
            0, ' ', false, false, false);
    /**
     * Alt-A
     */
    public static final TKeypress kbAltA = new TKeypress(false,
            0, 'a', true, false, false);
    /**
     * Alt-B
     */
    public static final TKeypress kbAltB = new TKeypress(false,
            0, 'b', true, false, false);
    /**
     * Alt-C
     */
    public static final TKeypress kbAltC = new TKeypress(false,
            0, 'c', true, false, false);
    /**
     * Alt-D
     */
    public static final TKeypress kbAltD = new TKeypress(false,
            0, 'd', true, false, false);
    /**
     * Alt-E
     */
    public static final TKeypress kbAltE = new TKeypress(false,
            0, 'e', true, false, false);
    /**
     * Alt-F
     */
    public static final TKeypress kbAltF = new TKeypress(false,
            0, 'f', true, false, false);
    /**
     * Alt-G
     */
    public static final TKeypress kbAltG = new TKeypress(false,
            0, 'g', true, false, false);
    /**
     * Alt-H
     */
    public static final TKeypress kbAltH = new TKeypress(false,
            0, 'h', true, false, false);
    /**
     * Alt-I
     */
    public static final TKeypress kbAltI = new TKeypress(false,
            0, 'i', true, false, false);
    /**
     * Alt-J
     */
    public static final TKeypress kbAltJ = new TKeypress(false,
            0, 'j', true, false, false);
    /**
     * Alt-K
     */
    public static final TKeypress kbAltK = new TKeypress(false,
            0, 'k', true, false, false);
    /**
     * Alt-L
     */
    public static final TKeypress kbAltL = new TKeypress(false,
            0, 'l', true, false, false);
    /**
     * Alt-M
     */
    public static final TKeypress kbAltM = new TKeypress(false,
            0, 'm', true, false, false);
    /**
     * Alt-N
     */
    public static final TKeypress kbAltN = new TKeypress(false,
            0, 'n', true, false, false);
    /**
     * Alt-O
     */
    public static final TKeypress kbAltO = new TKeypress(false,
            0, 'o', true, false, false);
    /**
     * Alt-P
     */
    public static final TKeypress kbAltP = new TKeypress(false,
            0, 'p', true, false, false);
    /**
     * Alt-Q
     */
    public static final TKeypress kbAltQ = new TKeypress(false,
            0, 'q', true, false, false);
    /**
     * Alt-R
     */
    public static final TKeypress kbAltR = new TKeypress(false,
            0, 'r', true, false, false);
    /**
     * Alt-S
     */
    public static final TKeypress kbAltS = new TKeypress(false,
            0, 's', true, false, false);
    /**
     * Alt-T
     */
    public static final TKeypress kbAltT = new TKeypress(false,
            0, 't', true, false, false);
    /**
     * Alt-U
     */
    public static final TKeypress kbAltU = new TKeypress(false,
            0, 'u', true, false, false);
    /**
     * Alt-V
     */
    public static final TKeypress kbAltV = new TKeypress(false,
            0, 'v', true, false, false);
    /**
     * Alt-W
     */
    public static final TKeypress kbAltW = new TKeypress(false,
            0, 'w', true, false, false);
    /**
     * Alt-X
     */
    public static final TKeypress kbAltX = new TKeypress(false,
            0, 'x', true, false, false);
    /**
     * Alt-Y
     */
    public static final TKeypress kbAltY = new TKeypress(false,
            0, 'y', true, false, false);
    /**
     * Alt-Z
     */
    public static final TKeypress kbAltZ = new TKeypress(false,
            0, 'z', true, false, false);
    /**
     * Alt-0
     */
    public static final TKeypress kbAlt0 = new TKeypress(false,
            0, '0', true, false, false);
    /**
     * Alt-1
     */
    public static final TKeypress kbAlt1 = new TKeypress(false,
            0, '1', true, false, false);
    /**
     * Alt-2
     */
    public static final TKeypress kbAlt2 = new TKeypress(false,
            0, '2', true, false, false);
    /**
     * Alt-3
     */
    public static final TKeypress kbAlt3 = new TKeypress(false,
            0, '3', true, false, false);
    /**
     * Alt-4
     */
    public static final TKeypress kbAlt4 = new TKeypress(false,
            0, '4', true, false, false);
    /**
     * Alt-5
     */
    public static final TKeypress kbAlt5 = new TKeypress(false,
            0, '5', true, false, false);
    /**
     * Alt-6
     */
    public static final TKeypress kbAlt6 = new TKeypress(false,
            0, '6', true, false, false);
    /**
     * Alt-7
     */
    public static final TKeypress kbAlt7 = new TKeypress(false,
            0, '7', true, false, false);
    /**
     * Alt-8
     */
    public static final TKeypress kbAlt8 = new TKeypress(false,
            0, '8', true, false, false);
    /**
     * Alt-9
     */
    public static final TKeypress kbAlt9 = new TKeypress(false,
            0, '9', true, false, false);
    /**
     * Ctrl-A
     */
    public static final TKeypress kbCtrlA = new TKeypress(false,
            0, 'A', false, true, false);
    /**
     * Ctrl-B
     */
    public static final TKeypress kbCtrlB = new TKeypress(false,
            0, 'B', false, true, false);
    /**
     * Ctrl-C
     */
    public static final TKeypress kbCtrlC = new TKeypress(false,
            0, 'C', false, true, false);
    /**
     * Ctrl-D
     */
    public static final TKeypress kbCtrlD = new TKeypress(false,
            0, 'D', false, true, false);
    /**
     * Ctrl-E
     */
    public static final TKeypress kbCtrlE = new TKeypress(false,
            0, 'E', false, true, false);
    /**
     * Ctrl-F
     */
    public static final TKeypress kbCtrlF = new TKeypress(false,
            0, 'F', false, true, false);
    /**
     * Ctrl-G
     */
    public static final TKeypress kbCtrlG = new TKeypress(false,
            0, 'G', false, true, false);
    /**
     * Ctrl-H
     */
    public static final TKeypress kbCtrlH = new TKeypress(false,
            0, 'H', false, true, false);
    /**
     * Ctrl-I
     */
    public static final TKeypress kbCtrlI = new TKeypress(false,
            0, 'I', false, true, false);
    /**
     * Ctrl-J
     */
    public static final TKeypress kbCtrlJ = new TKeypress(false,
            0, 'J', false, true, false);
    /**
     * Ctrl-K
     */
    public static final TKeypress kbCtrlK = new TKeypress(false,
            0, 'K', false, true, false);
    /**
     * Ctrl-L
     */
    public static final TKeypress kbCtrlL = new TKeypress(false,
            0, 'L', false, true, false);
    /**
     * Ctrl-M
     */
    public static final TKeypress kbCtrlM = new TKeypress(false,
            0, 'M', false, true, false);
    /**
     * Ctrl-N
     */
    public static final TKeypress kbCtrlN = new TKeypress(false,
            0, 'N', false, true, false);
    /**
     * Ctrl-O
     */
    public static final TKeypress kbCtrlO = new TKeypress(false,
            0, 'O', false, true, false);
    /**
     * Ctrl-P
     */
    public static final TKeypress kbCtrlP = new TKeypress(false,
            0, 'P', false, true, false);
    /**
     * Ctrl-Q
     */
    public static final TKeypress kbCtrlQ = new TKeypress(false,
            0, 'Q', false, true, false);
    /**
     * Ctrl-R
     */
    public static final TKeypress kbCtrlR = new TKeypress(false,
            0, 'R', false, true, false);
    /**
     * Ctrl-S
     */
    public static final TKeypress kbCtrlS = new TKeypress(false,
            0, 'S', false, true, false);
    /**
     * Ctrl-T
     */
    public static final TKeypress kbCtrlT = new TKeypress(false,
            0, 'T', false, true, false);
    /**
     * Ctrl-U
     */
    public static final TKeypress kbCtrlU = new TKeypress(false,
            0, 'U', false, true, false);
    /**
     * Ctrl-V
     */
    public static final TKeypress kbCtrlV = new TKeypress(false,
            0, 'V', false, true, false);
    /**
     * Ctrl-W
     */
    public static final TKeypress kbCtrlW = new TKeypress(false,
            0, 'W', false, true, false);
    /**
     * Ctrl-X
     */
    public static final TKeypress kbCtrlX = new TKeypress(false,
            0, 'X', false, true, false);
    /**
     * Ctrl-Y
     */
    public static final TKeypress kbCtrlY = new TKeypress(false,
            0, 'Y', false, true, false);
    /**
     * Ctrl-Z
     */
    public static final TKeypress kbCtrlZ = new TKeypress(false,
            0, 'Z', false, true, false);
    /**
     * Alt-Shift-A
     */
    public static final TKeypress kbAltShiftA = new TKeypress(false,
            0, 'A', true, false, true);
    /**
     * Alt-Shift-B
     */
    public static final TKeypress kbAltShiftB = new TKeypress(false,
            0, 'B', true, false, true);
    /**
     * Alt-Shift-C
     */
    public static final TKeypress kbAltShiftC = new TKeypress(false,
            0, 'C', true, false, true);
    /**
     * Alt-Shift-D
     */
    public static final TKeypress kbAltShiftD = new TKeypress(false,
            0, 'D', true, false, true);
    /**
     * Alt-Shift-E
     */
    public static final TKeypress kbAltShiftE = new TKeypress(false,
            0, 'E', true, false, true);
    /**
     * Alt-Shift-F
     */
    public static final TKeypress kbAltShiftF = new TKeypress(false,
            0, 'F', true, false, true);
    /**
     * Alt-Shift-G
     */
    public static final TKeypress kbAltShiftG = new TKeypress(false,
            0, 'G', true, false, true);
    /**
     * Alt-Shift-H
     */
    public static final TKeypress kbAltShiftH = new TKeypress(false,
            0, 'H', true, false, true);
    /**
     * Alt-Shift-I
     */
    public static final TKeypress kbAltShiftI = new TKeypress(false,
            0, 'I', true, false, true);
    /**
     * Alt-Shift-J
     */
    public static final TKeypress kbAltShiftJ = new TKeypress(false,
            0, 'J', true, false, true);
    /**
     * Alt-Shift-K
     */
    public static final TKeypress kbAltShiftK = new TKeypress(false,
            0, 'K', true, false, true);
    /**
     * Alt-Shift-L
     */
    public static final TKeypress kbAltShiftL = new TKeypress(false,
            0, 'L', true, false, true);
    /**
     * Alt-Shift-M
     */
    public static final TKeypress kbAltShiftM = new TKeypress(false,
            0, 'M', true, false, true);
    /**
     * Alt-Shift-N
     */
    public static final TKeypress kbAltShiftN = new TKeypress(false,
            0, 'N', true, false, true);
    /**
     * Alt-Shift-O
     */
    public static final TKeypress kbAltShiftO = new TKeypress(false,
            0, 'O', true, false, true);
    /**
     * Alt-Shift-P
     */
    public static final TKeypress kbAltShiftP = new TKeypress(false,
            0, 'P', true, false, true);
    /**
     * Alt-Shift-Q
     */
    public static final TKeypress kbAltShiftQ = new TKeypress(false,
            0, 'Q', true, false, true);
    /**
     * Alt-Shift-R
     */
    public static final TKeypress kbAltShiftR = new TKeypress(false,
            0, 'R', true, false, true);
    /**
     * Alt-Shift-S
     */
    public static final TKeypress kbAltShiftS = new TKeypress(false,
            0, 'S', true, false, true);
    /**
     * Alt-Shift-T
     */
    public static final TKeypress kbAltShiftT = new TKeypress(false,
            0, 'T', true, false, true);
    /**
     * Alt-Shift-U
     */
    public static final TKeypress kbAltShiftU = new TKeypress(false,
            0, 'U', true, false, true);
    /**
     * Alt-Shift-V
     */
    public static final TKeypress kbAltShiftV = new TKeypress(false,
            0, 'V', true, false, true);
    /**
     * Alt-Shift-W
     */
    public static final TKeypress kbAltShiftW = new TKeypress(false,
            0, 'W', true, false, true);
    /**
     * Alt-Shift-X
     */
    public static final TKeypress kbAltShiftX = new TKeypress(false,
            0, 'X', true, false, true);
    /**
     * Alt-Shift-Y
     */
    public static final TKeypress kbAltShiftY = new TKeypress(false,
            0, 'Y', true, false, true);
    /**
     * Alt-Shift-Z
     */
    public static final TKeypress kbAltShiftZ = new TKeypress(false,
            0, 'Z', true, false, true);

    /**
     * Alt-Shift-Home
     */
    public static final TKeypress kbAltShiftHome = new TKeypress(true,
            TKeypress.HOME, ' ', true, false, true);
    /**
     * Alt-Shift-End
     */
    public static final TKeypress kbAltShiftEnd = new TKeypress(true,
            TKeypress.END, ' ', true, false, true);
    /**
     * Alt-Shift-PgUp
     */
    public static final TKeypress kbAltShiftPgUp = new TKeypress(true,
            TKeypress.PGUP, ' ', true, false, true);
    /**
     * Alt-Shift-PgDn
     */
    public static final TKeypress kbAltShiftPgDn = new TKeypress(true,
            TKeypress.PGDN, ' ', true, false, true);
    /**
     * Alt-Shift-Up
     */
    public static final TKeypress kbAltShiftUp = new TKeypress(true,
            TKeypress.UP, ' ', true, false, true);
    /**
     * Alt-Shift-Down
     */
    public static final TKeypress kbAltShiftDown = new TKeypress(true,
            TKeypress.DOWN, ' ', true, false, true);
    /**
     * Alt-Shift-Left
     */
    public static final TKeypress kbAltShiftLeft = new TKeypress(true,
            TKeypress.LEFT, ' ', true, false, true);
    /**
     * Alt-Shift-Right
     */
    public static final TKeypress kbAltShiftRight = new TKeypress(true,
            TKeypress.RIGHT, ' ', true, false, true);

    /**
     * Ctrl-Shift-Home
     */
    public static final TKeypress kbCtrlShiftHome = new TKeypress(true,
            TKeypress.HOME, ' ', false, true, true);
    /**
     * Ctrl-Shift-End
     */
    public static final TKeypress kbCtrlShiftEnd = new TKeypress(true,
            TKeypress.END, ' ', false, true, true);
    /**
     * Ctrl-Shift-PgUp
     */
    public static final TKeypress kbCtrlShiftPgUp = new TKeypress(true,
            TKeypress.PGUP, ' ', false, true, true);
    /**
     * Ctrl-Shift-PgDn
     */
    public static final TKeypress kbCtrlShiftPgDn = new TKeypress(true,
            TKeypress.PGDN, ' ', false, true, true);
    /**
     * Ctrl-Shift-Up
     */
    public static final TKeypress kbCtrlShiftUp = new TKeypress(true,
            TKeypress.UP, ' ', false, true, true);
    /**
     * Ctrl-Shift-Down
     */
    public static final TKeypress kbCtrlShiftDown = new TKeypress(true,
            TKeypress.DOWN, ' ', false, true, true);
    /**
     * Ctrl-Shift-Left
     */
    public static final TKeypress kbCtrlShiftLeft = new TKeypress(true,
            TKeypress.LEFT, ' ', false, true, true);
    /**
     * Ctrl-Shift-Right
     */
    public static final TKeypress kbCtrlShiftRight = new TKeypress(true,
            TKeypress.RIGHT, ' ', false, true, true);


    /**
     * Backspace as ^H.
     */
    public static final TKeypress kbBackspace = new TKeypress(false,
            0, 'H', false, true, false);

    /**
     * Control-Backspace as function key.
     */
    public static final TKeypress kbCtrlBackspace = new TKeypress(true,
            TKeypress.BACKSPACE, ' ', false, true, false);

    /**
     * Alt-Backspace as function key.
     */
    public static final TKeypress kbAltBackspace = new TKeypress(true,
            TKeypress.BACKSPACE, ' ', true, false, false);

    /**
     * Backspace as ^?.
     */
    public static final TKeypress kbBackspaceDel = new TKeypress(false,
            0, (char) 0x7F, false, false, false);

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, ch is meaningless, use keyCode instead.
     */
    private boolean isFunctionKey;

    /**
     * Will be set to F1, F2, HOME, END, etc. if isKey is true.
     */
    private int keyCode;

    /**
     * Keystroke modifier ALT.
     */
    private boolean alt;

    /**
     * Keystroke modifier CTRL.
     */
    private boolean ctrl;

    /**
     * Keystroke modifier SHIFT.
     */
    private boolean shift;

    /**
     * The character received.
     */
    private int ch;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor makes an immutable instance.
     *
     * @param isKey is true, this is a function key
     * @param fnKey the function key code (only valid if isKey is true)
     * @param ch the character (only valid if fnKey is false)
     * @param alt if true, ALT was pressed with this keystroke
     * @param ctrl if true, CTRL was pressed with this keystroke
     * @param shift if true, SHIFT was pressed with this keystroke
     */
    public TKeypress(final boolean isKey, final int fnKey, final int ch,
            final boolean alt, final boolean ctrl, final boolean shift) {

        this.isFunctionKey = isKey;
        this.keyCode       = fnKey;
        this.ch            = ch;
        this.alt           = alt;
        this.ctrl          = ctrl;
        this.shift         = shift;
    }

    // ------------------------------------------------------------------------
    // TKeypress --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Getter for isFunctionKey.
     *
     * @return if true, ch is meaningless, use keyCode instead
     */
    public boolean isFnKey() {
        return isFunctionKey;
    }

    /**
     * Getter for function key code.
     *
     * @return function key code int value (only valid is isKey is true)
     */
    public int getKeyCode() {
        return keyCode;
    }

    /**
     * Getter for ALT.
     *
     * @return alt value
     */
    public boolean isAlt() {
        return alt;
    }

    /**
     * Getter for CTRL.
     *
     * @return ctrl value
     */
    public boolean isCtrl() {
        return ctrl;
    }

    /**
     * Getter for SHIFT.
     *
     * @return shift value
     */
    public boolean isShift() {
        return shift;
    }

    /**
     * Getter for character.
     *
     * @return the character (only valid if isKey is false)
     */
    public int getChar() {
        return ch;
    }

    /**
     * Create a duplicate instance.
     *
     * @return duplicate intance
     */
    public TKeypress dup() {
        TKeypress keypress = new TKeypress(isFunctionKey, keyCode, ch,
            alt, ctrl, shift);
        return keypress;
    }

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another TKeypress instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof TKeypress)) {
            return false;
        }

        TKeypress that = (TKeypress) rhs;
        return ((isFunctionKey == that.isFunctionKey)
                && (keyCode == that.keyCode)
                && (ch == that.ch)
                && (alt == that.alt)
                && (ctrl == that.ctrl)
                && (shift == that.shift));
    }

    /**
     * Comparison check, omitting the ctrl/alt/shift flags.
     *
     * @param rhs another TKeypress instance
     * @return true if all fields (except for ctrl/alt/shift) are equal
     */
    public boolean equalsWithoutModifiers(final Object rhs) {
        if (!(rhs instanceof TKeypress)) {
            return false;
        }

        TKeypress that = (TKeypress) rhs;
        return ((isFunctionKey == that.isFunctionKey)
                && (keyCode == that.keyCode)
                && (ch == that.ch));
    }

    /**
     * Hashcode uses all fields in equals().
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        int A = 13;
        int B = 23;
        int hash = A;
        hash = (B * hash) + (isFunctionKey ? 1 : 0);
        hash = (B * hash) + keyCode;
        hash = (B * hash) + ch;
        hash = (B * hash) + (alt ? 1 : 0);
        hash = (B * hash) + (ctrl ? 1 : 0);
        hash = (B * hash) + (shift ? 1 : 0);
        return hash;
    }

    /**
     * Make human-readable description of this TKeypress.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        // Special case: Enter is "<arrow> <line> <angle>"
        if (equals(kbEnter)) {
            return "\u25C0\u2500\u2518";
        }

        // Special case: Space is "Space"
        if (equals(kbSpace)) {
            return "Space";
        }

        // Special case: Backspace is "<arrow>"
        if (equals(kbBackspace)) {
            return "\u2190";
        }

        // Special case: BackspaceDel is "<house>"
        if (equals(kbBackspaceDel)) {
            return "\u2302";
        }

        if (equals(kbShiftLeft)) {
            return "Shift+\u2190";
        }
        if (equals(kbShiftRight)) {
            return "Shift+\u2192";
        }

        if (isFunctionKey) {
            switch (keyCode) {
            case F1:
                return String.format("%s%s%sF1",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F2:
                return String.format("%s%s%sF2",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F3:
                return String.format("%s%s%sF3",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F4:
                return String.format("%s%s%sF4",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F5:
                return String.format("%s%s%sF5",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F6:
                return String.format("%s%s%sF6",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F7:
                return String.format("%s%s%sF7",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F8:
                return String.format("%s%s%sF8",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F9:
                return String.format("%s%s%sF9",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F10:
                return String.format("%s%s%sF10",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F11:
                return String.format("%s%s%sF11",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case F12:
                return String.format("%s%s%sF12",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case HOME:
                return String.format("%s%s%sHOME",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case END:
                return String.format("%s%s%sEND",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case PGUP:
                return String.format("%s%s%sPGUP",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case PGDN:
                return String.format("%s%s%sPGDN",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case INS:
                return String.format("%s%s%sINS",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case DEL:
                return String.format("%s%s%sDEL",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case RIGHT:
                return String.format("%s%s%sRIGHT",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case LEFT:
                return String.format("%s%s%sLEFT",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case UP:
                return String.format("%s%s%sUP",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case DOWN:
                return String.format("%s%s%sDOWN",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case TAB:
                return String.format("%s%s%sTAB",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case BTAB:
                return String.format("%s%s%sBTAB",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case ENTER:
                return String.format("%s%s%sENTER",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case ESC:
                return String.format("%s%s%sESC",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            case BACKSPACE:
                return String.format("%s%s%sBACKSPACE",
                        ctrl ? "Ctrl+" : "",
                                alt ? "Alt+" : "",
                                        shift ? "Shift+" : "");
            default:
                return String.format("--UNKNOWN--");
            }
        } else {
            if (alt && !shift && !ctrl) {
                // Alt-X
                return String.format("Alt+%c", Character.toUpperCase(ch));
            } else if (!alt && shift && !ctrl) {
                // Shift-X
                return String.format("%c", ch);
            } else if (!alt && !shift && ctrl) {
                // Ctrl-X
                return String.format("Ctrl+%c", ch);
            } else if (alt && shift && !ctrl) {
                // Alt-Shift-X
                return String.format("Alt+Shift+%c", ch);
            } else if (!alt && shift && ctrl) {
                // Ctrl-Shift-X
                return String.format("Ctrl+Shift+%c", ch);
            } else if (alt && !shift && ctrl) {
                // Ctrl-Alt-X
                return String.format("Ctrl+Alt+%c", Character.toUpperCase(ch));
            } else if (alt && shift && ctrl) {
                // Ctrl-Alt-Shift-X
                return String.format("Ctrl+Alt+Shift+%c",
                        Character.toUpperCase(ch));
            } else {
                // X
                return String.format("%c", ch);
            }
        }
    }

    /**
     * Convert a keypress to lowercase.  Function keys and alt/ctrl keys are
     * not converted.
     *
     * @return a new instance with the key converted
     */
    public TKeypress toLowerCase() {
        TKeypress newKey = new TKeypress(isFunctionKey, keyCode, ch, alt, ctrl,
                shift);
        if (!isFunctionKey && (ch >= 'A') && (ch <= 'Z') && !ctrl && !alt) {
            newKey.shift = false;
            newKey.ch += 32;
        }
        return newKey;
    }

    /**
     * Convert a keypress to uppercase.  Function keys and alt/ctrl keys are
     * not converted.
     *
     * @return a new instance with the key converted
     */
    public TKeypress toUpperCase() {
        TKeypress newKey = new TKeypress(isFunctionKey, keyCode, ch, alt, ctrl,
                shift);
        if (!isFunctionKey && (ch >= 'a') && (ch <= 'z') && !ctrl && !alt) {
            newKey.shift = true;
            newKey.ch -= 32;
        }
        return newKey;
    }

}
