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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import casciian.backend.HeadlessBackend;
import casciian.backend.SystemProperties;
import casciian.event.TMenuEvent;
import casciian.menu.TMenuItem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoApplicationRenderingMenuTest {

    private DemoApplication application;
    private CountingHeadlessBackend backend;

    @BeforeEach
    void setUp() {
        SystemProperties.setPaletteColor(false);
        SystemProperties.setRgbColor(false);
        SystemProperties.setTranslucence(false);
        backend = new CountingHeadlessBackend();
        application = new DemoApplication(backend);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(SystemProperties.CASCIIAN_ECMA48_PALETTE_COLOR);
        System.clearProperty(SystemProperties.CASCIIAN_ECMA48_RGB_COLOR);
        System.clearProperty(SystemProperties.CASCIIAN_TRANSLUCENCE);
        SystemProperties.reset();
    }

    @Test
    void renderingOptionsAreGroupedAndInitializedFromSettings() {
        TMenuItem gradients = application.getMenuItem(10010);
        TMenuItem translucence = application.getMenuItem(10015);
        TMenuItem palette = application.getMenuItem(10026);
        TMenuItem rgb = application.getMenuItem(10027);

        assertSame(gradients.getParent(), translucence.getParent());
        assertSame(gradients.getParent(), palette.getParent());
        assertSame(gradients.getParent(), rgb.getParent());
        assertNotSame(gradients.getParent(),
            application.getMenuItem(10013).getParent());
    }

    @Test
    void colorOptionsChangeSettingsAndStandardColorsPreservesOtherOptions() {
        TMenuItem palette = application.getMenuItem(10026);
        palette.setChecked(true);
        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), 10026)));
        assertTrue(SystemProperties.isPaletteColor());

        TMenuItem rgb = application.getMenuItem(10027);
        rgb.setChecked(true);
        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), 10027)));
        assertTrue(SystemProperties.isRgbColor());

        TMenuItem gradients = application.getMenuItem(10010);
        gradients.setChecked(true);
        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), 10010)));
        TMenuItem translucence = application.getMenuItem(10015);
        translucence.setChecked(true);
        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), 10015)));
        assertTrue(SystemProperties.isTranslucence());

        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), 10028)));

        assertFalse(SystemProperties.isPaletteColor());
        assertFalse(SystemProperties.isRgbColor());
        assertFalse(palette.isChecked());
        assertFalse(rgb.isChecked());
        assertTrue(gradients.isChecked());
        assertTrue(translucence.isChecked());
        assertTrue(SystemProperties.isTranslucence());
    }

    @Test
    void colorMenuChecksReflectSettingsSetBeforeStartup() {
        SystemProperties.setPaletteColor(true);
        SystemProperties.setRgbColor(true);

        application = new DemoApplication(new HeadlessBackend());

        assertTrue(application.getMenuItem(10026).isChecked());
        assertTrue(application.getMenuItem(10027).isChecked());
    }

    @Test
    void colorModeChangesClearPhysicalScreenAndDefaultsResetModes() {
        int clearCount = backend.getPhysicalClearCount();

        TMenuItem palette = application.getMenuItem(10026);
        palette.setChecked(true);
        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), 10026)));
        assertTrue(backend.getPhysicalClearCount() > clearCount);

        clearCount = backend.getPhysicalClearCount();
        TMenuItem rgb = application.getMenuItem(10027);
        rgb.setChecked(true);
        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), 10027)));
        assertTrue(backend.getPhysicalClearCount() > clearCount);

        clearCount = backend.getPhysicalClearCount();
        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), 10028)));
        assertTrue(backend.getPhysicalClearCount() > clearCount);

        palette.setChecked(true);
        application.onMenu(new TMenuEvent(application.getBackend(), 10026));
        rgb.setChecked(true);
        application.onMenu(new TMenuEvent(application.getBackend(), 10027));
        clearCount = backend.getPhysicalClearCount();

        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), 10004)));

        assertFalse(SystemProperties.isPaletteColor());
        assertFalse(SystemProperties.isRgbColor());
        assertFalse(palette.isChecked());
        assertFalse(rgb.isChecked());
        assertTrue(backend.getPhysicalClearCount() > clearCount);
    }

    private static class CountingHeadlessBackend extends HeadlessBackend {
        private int physicalClearCount;

        @Override
        public synchronized void clearPhysical() {
            physicalClearCount++;
            super.clearPhysical();
        }

        int getPhysicalClearCount() {
            return physicalClearCount;
        }
    }
}
