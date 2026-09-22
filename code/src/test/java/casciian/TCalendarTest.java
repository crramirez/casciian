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

import java.util.Calendar;
import java.util.GregorianCalendar;

import casciian.backend.HeadlessBackend;
import casciian.event.TMouseEvent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests mouse interaction with the calendar widget.
 */
class TCalendarTest {

    @Test
    void doubleClickingADateCellFiresTheUpdateAction() {
        int[] activations = new int[1];
        TCalendar calendar = new TCalendar(makeWindow(), 1, 1,
            new TAction() {
                public void DO() {
                    activations[0]++;
                }
            });
        Calendar value = calendar.getValue();
        int[] point = dayPoint(value, value.get(Calendar.DAY_OF_MONTH));

        mouse(calendar, TMouseEvent.Type.MOUSE_DOWN, point[0], point[1]);
        mouse(calendar, TMouseEvent.Type.MOUSE_DOUBLE_CLICK, point[0], point[1]);

        assertEquals(1, activations[0]);
    }

    @Test
    void doubleClickingAnArrowDoesNotFireTheUpdateActionAndStillNavigates() {
        int[] activations = new int[1];
        TCalendar calendar = new TCalendar(makeWindow(), 1, 1,
            new TAction() {
                public void DO() {
                    activations[0]++;
                }
            });
        Calendar nextMonth = calendar.getValue();
        nextMonth.add(Calendar.MONTH, 1);
        int[] nextMonthDayOne = dayPoint(nextMonth, 1);

        mouse(calendar, TMouseEvent.Type.MOUSE_DOWN, calendar.getWidth() - 2, 0);
        mouse(calendar, TMouseEvent.Type.MOUSE_DOUBLE_CLICK,
            calendar.getWidth() - 2, 0);
        mouse(calendar, TMouseEvent.Type.MOUSE_DOWN,
            nextMonthDayOne[0], nextMonthDayOne[1]);

        assertEquals(0, activations[0]);
        Calendar selected = calendar.getValue();
        assertEquals(nextMonth.get(Calendar.YEAR),
            selected.get(Calendar.YEAR));
        assertEquals(nextMonth.get(Calendar.MONTH),
            selected.get(Calendar.MONTH));
        assertEquals(1, selected.get(Calendar.DAY_OF_MONTH));
    }

    private TWindow makeWindow() {
        return new TWindow(new TApplication(new HeadlessBackend()), "test",
            0, 0, 40, 10);
    }

    private int[] dayPoint(final Calendar calendar, final int dayOfMonth) {
        GregorianCalendar firstOfMonth = new GregorianCalendar();
        firstOfMonth.setTimeInMillis(calendar.getTimeInMillis());
        firstOfMonth.set(Calendar.DAY_OF_MONTH, 1);

        int dayOf1st = firstOfMonth.get(Calendar.DAY_OF_WEEK) - 1;
        if (firstOfMonth.getFirstDayOfWeek() == Calendar.MONDAY) {
            dayOf1st--;
        }

        int dayColumn = dayOf1st * 4;
        int row = 2;
        if (dayOf1st < 0) {
            dayColumn = 4 * 6;
        }
        for (int day = 1; day < dayOfMonth; day++) {
            dayColumn += 4;
            if (dayColumn == 4 * 7) {
                dayColumn = 0;
                row++;
            }
        }
        return new int[] { dayColumn + 1, row };
    }

    private void mouse(final TCalendar calendar, final TMouseEvent.Type type,
        final int x, final int y) {

        boolean mouse1 = type != TMouseEvent.Type.MOUSE_UP;
        TMouseEvent event = new TMouseEvent(null, type, x, y,
            calendar.getAbsoluteX() + x, calendar.getAbsoluteY() + y, 0, 0,
            mouse1, false, false, false, false, false, false, false);
        switch (type) {
        case MOUSE_DOWN:
            calendar.onMouseDown(event);
            break;
        case MOUSE_DOUBLE_CLICK:
            calendar.onMouseDoubleClick(event);
            break;
        default:
            calendar.onMouseUp(event);
            break;
        }
    }
}
