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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.*;

/**
 * TCalendar is a date picker widget.
 */
public class TCalendar extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The calendar being displayed.
     */
    private GregorianCalendar displayCalendar = null;

    /**
     * The calendar with the selected day.
     */
    private GregorianCalendar calendar = null;

    /**
     * The days of week heading line.
     */
    private String daysOfWeek = "  S   M   T   W   T   F   S ";

    /**
     * If true, the week starts on Monday.
     */
    private boolean startOnMonday = false;

    /**
     * The action to perform when the user changes the value of the calendar.
     */
    private TAction updateAction = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param updateAction action to call when the user changes the value of
     * the calendar
     */
    @SuppressWarnings("this-escape")
    public TCalendar(final TWidget parent, final int x, final int y,
        final TAction updateAction) {

        // Set parent and window
        super(parent, x, y, 28, 8);

        this.updateAction = updateAction;
        displayCalendar = new GregorianCalendar(getLocale());
        calendar = new GregorianCalendar(getLocale());

        GregorianCalendar dayOfWeekCalendar = new GregorianCalendar(getLocale());
        if (dayOfWeekCalendar.getFirstDayOfWeek() == Calendar.MONDAY) {
            startOnMonday = true;
        }

        dayOfWeekCalendar.setWeekDate(2025, 1,
            (startOnMonday ? Calendar.MONDAY : Calendar.SUNDAY));
        daysOfWeek = "  ";
        for (int i = 0; i < 7; i++) {
            daysOfWeek += dayOfWeekCalendar.getDisplayName(Calendar.DAY_OF_WEEK,
                Calendar.LONG, getLocale()).substring(0, 1);
            if (i < 6) {
                daysOfWeek += "   ";
            } else {
                daysOfWeek += " ";
            }
            dayOfWeekCalendar.add(Calendar.DAY_OF_WEEK, 1);
        }
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the left arrow.
     *
     * @param mouse mouse event
     * @return true if the mouse is currently on the left arrow
     */
    private boolean mouseOnLeftArrow(final TMouseEvent mouse) {
        if ((mouse.getY() == 0)
            && (mouse.getX() == 1)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the mouse is currently on the right arrow.
     *
     * @param mouse mouse event
     * @return true if the mouse is currently on the right arrow
     */
    private boolean mouseOnRightArrow(final TMouseEvent mouse) {
        if ((mouse.getY() == 0)
            && (mouse.getX() == getWidth() - 2)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse down clicks.
     *
     * @param mouse mouse button down event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if ((mouseOnLeftArrow(mouse)) && (mouse.isMouse1())) {
            displayCalendar.add(Calendar.MONTH, -1);
        } else if ((mouseOnRightArrow(mouse)) && (mouse.isMouse1())) {
            displayCalendar.add(Calendar.MONTH, 1);
        } else if (mouse.isMouse1()) {
            // Find the day this might correspond to, and set it.
            int index = (mouse.getY() - 2) * 7 + (mouse.getX() / 4) + 1;
            // System.err.println("index: " + index);

            int lastDayNumber = displayCalendar.getActualMaximum(
                    Calendar.DAY_OF_MONTH);
            GregorianCalendar firstOfMonth = new GregorianCalendar();
            firstOfMonth.setTimeInMillis(displayCalendar.getTimeInMillis());
            firstOfMonth.set(Calendar.DAY_OF_MONTH, 1);
            int dayOf1st = firstOfMonth.get(Calendar.DAY_OF_WEEK) - 1;
            if (startOnMonday) {
                dayOf1st--;
            }
            // System.err.println("dayOf1st: " + dayOf1st);

            int day = index - dayOf1st;
            if (dayOf1st < 0) {
                day -= 7;
            }
            // System.err.println("day: " + day);

            if ((day < 1) || (day > lastDayNumber)) {
                return;
            }
            calendar.setTimeInMillis(displayCalendar.getTimeInMillis());
            calendar.set(Calendar.DAY_OF_MONTH, day);
        }
    }

    /**
     * Handle mouse double click.
     *
     * @param mouse mouse double click event
     */
    @Override
    public void onMouseDoubleClick(final TMouseEvent mouse) {
        if (updateAction != null) {
            updateAction.DO(this);
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        int increment = 0;

        if (keypress.equals(kbUp)) {
            increment = -7;
        } else if (keypress.equals(kbDown)) {
            increment = 7;
        } else if (keypress.equals(kbLeft)) {
            increment = -1;
        } else if (keypress.equals(kbRight)) {
            increment = 1;
        } else if (keypress.equals(kbEnter)) {
            if (updateAction != null) {
                updateAction.DO(this);
            }
            return;
        } else {
            // Pass to parent for the things we don't care about.
            super.onKeypress(keypress);
            return;
        }

        if (increment != 0) {
            calendar.add(Calendar.DAY_OF_YEAR, increment);

            if ((displayCalendar.get(Calendar.MONTH) != calendar.get(
                    Calendar.MONTH))
                || (displayCalendar.get(Calendar.YEAR) != calendar.get(
                    Calendar.YEAR))
            ) {
                if (increment < 0) {
                    displayCalendar.add(Calendar.MONTH, -1);
                } else {
                    displayCalendar.add(Calendar.MONTH, 1);
                }
            }
        }

    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the Locale used for producing user-facing strings.
     *
     * @param locale the locale
     */
    @Override
    public void setLocale(final Locale locale) {
        super.setLocale(locale);
        GregorianCalendar newDisplayCalendar = new GregorianCalendar(locale);
        newDisplayCalendar.setTime(displayCalendar.getTime());
        displayCalendar = newDisplayCalendar;

        GregorianCalendar newCalendar = new GregorianCalendar(locale);
        newCalendar.setTime(calendar.getTime());
        calendar = newCalendar;
    }

    /**
     * Draw the combobox down arrow.
     */
    @Override
    public void draw() {
        CellAttributes backgroundColor = getTheme().getColor(
                "tcalendar.background");
        CellAttributes dayColor = getTheme().getColor(
                "tcalendar.day");
        CellAttributes selectedDayColor = getTheme().getColor(
                "tcalendar.day.selected");
        CellAttributes arrowColor = getTheme().getColor(
                "tcalendar.arrow");
        CellAttributes titleColor = getTheme().getColor(
                "tcalendar.title");

        // Fill in the interior background
        for (int i = 0; i < getHeight(); i++) {
            hLineXY(0, i, getWidth(), ' ', backgroundColor);
        }

        // Draw the title
        String title = String.format(getLocale(), "%tB %tY", displayCalendar,
            displayCalendar);
        // This particular title is always single-width (see format string
        // above), but for completeness let's treat it the same as every
        // other window title string.
        int titleLeft = (getWidth() - StringUtils.width(title) - 2) / 2;
        putCharXY(titleLeft, 0, ' ', titleColor);
        putStringXY(titleLeft + 1, 0, title, titleColor);
        putCharXY(titleLeft + StringUtils.width(title) + 1, 0, ' ',
            titleColor);

        // Arrows
        putCharXY(1, 0, GraphicsChars.LEFTARROW, arrowColor);
        putCharXY(getWidth() - 2, 0, GraphicsChars.RIGHTARROW,
            arrowColor);

        /*
         * Now draw out the days.
         */
        putStringXY(0, 1, daysOfWeek, dayColor);
        int lastDayNumber = displayCalendar.getActualMaximum(
                Calendar.DAY_OF_MONTH);
        GregorianCalendar firstOfMonth = new GregorianCalendar(getLocale());
        firstOfMonth.setTimeInMillis(displayCalendar.getTimeInMillis());
        firstOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        int dayOf1st = firstOfMonth.get(Calendar.DAY_OF_WEEK) - 1;
        if (startOnMonday) {
            dayOf1st--;
        }
        int dayColumn = dayOf1st * 4;
        int row = 2;

        int dayOfMonth = 1;
        if (dayOf1st < 0) {
            dayColumn = 4 * 6;
        }
        while (dayOfMonth <= lastDayNumber) {
            if (dayColumn == 4 * 7) {
                dayColumn = 0;
                row++;
            }
            if ((dayOfMonth == calendar.get(Calendar.DAY_OF_MONTH))
                && (displayCalendar.get(Calendar.MONTH) == calendar.get(
                    Calendar.MONTH))
                && (displayCalendar.get(Calendar.YEAR) == calendar.get(
                    Calendar.YEAR))
            ) {
                putStringXY(dayColumn, row,
                    String.format(" %2d ", dayOfMonth), selectedDayColor);
            } else {
                putStringXY(dayColumn, row,
                    String.format(" %2d ", dayOfMonth), dayColor);
            }
            dayColumn += 4;
            dayOfMonth++;
        }

    }

    // ------------------------------------------------------------------------
    // TCalendar --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get calendar value.
     *
     * @return the current calendar value (clone instance)
     */
    public Calendar getValue() {
        return (Calendar) calendar.clone();
    }

    /**
     * Set calendar value.
     *
     * @param calendar the new value to use
     */
    public final void setValue(final Calendar calendar) {
        this.calendar.setTimeInMillis(calendar.getTimeInMillis());
    }

    /**
     * Set calendar value.
     *
     * @param millis the millis to set to
     */
    public final void setValue(final long millis) {
        this.calendar.setTimeInMillis(millis);
    }

}
