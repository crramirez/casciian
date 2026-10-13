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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import casciian.backend.HeadlessBackend;
import casciian.backend.SystemProperties;
import casciian.event.TMenuEvent;
import casciian.menu.TMenuItem;

import static org.assertj.core.api.Assertions.assertThat;
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
        assertSame(gradients.getParent(),
            application.getMenuItem(10028).getParent());
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
        assertTrue(application.getMenuItem(10028).isChecked());
        assertTrue(gradients.isChecked());
        assertTrue(translucence.isChecked());
        assertTrue(SystemProperties.isTranslucence());
    }

    @ParameterizedTest
    @CsvSource({
        "false, false, 10028",
        "true, false, 10026",
        "false, true, 10027",
        "true, true, 10027"
    })
    void colorMenuChecksReflectSettingsSetBeforeStartup(
        final boolean paletteColor, final boolean rgbColor,
        final int selectedId) {

        SystemProperties.setPaletteColor(paletteColor);
        SystemProperties.setRgbColor(rgbColor);
        application = new DemoApplication(new HeadlessBackend());

        assertColorMode(selectedId);
    }

    @ParameterizedTest
    @CsvSource({
        "10026, 10026", "10026, 10027", "10026, 10028",
        "10027, 10026", "10027, 10027", "10027, 10028",
        "10028, 10026", "10028, 10027", "10028, 10028"
    })
    void colorModeSelectionsAreExclusiveEvenWhenReselected(
        final int initialId, final int selectedId) {

        selectMenuItem(initialId);
        assertColorMode(initialId);
        int clearCount = backend.getPhysicalClearCount();
        selectMenuItem(selectedId);

        assertColorMode(selectedId);
        assertThat(backend.getPhysicalClearCount()).isGreaterThan(clearCount);
    }

    @ParameterizedTest
    @CsvSource({
        "10026, 10005", "10027, 10005", "10028, 10005",
        "10026, 10006", "10027, 10006", "10028, 10006"
    })
    void rebuildingMenusPreservesExclusiveColorMode(
        final int selectedId, final int languageId) {

        selectMenuItem(selectedId);
        TMenuItem oldItem = application.getMenuItem(selectedId);
        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), languageId)));

        assertThat(application.getMenuItem(selectedId)).isNotSameAs(oldItem);
        assertColorMode(selectedId);
        for (int menuId: new int[] {10026, 10027, 10028}) {
            selectMenuItem(menuId);
            assertColorMode(menuId);
        }
        selectMenuItem(10026);
        assertTrue(application.onMenu(new TMenuEvent(
            application.getBackend(), 10004)));
        assertColorMode(10028);
    }

    private void selectMenuItem(final int menuId) {
        TMenuItem item = application.getMenuItem(menuId);
        // Checkable menu items toggle before the application handles the event.
        item.setChecked(!item.isChecked());
        assertThat(application.onMenu(new TMenuEvent(
            application.getBackend(), menuId))).isTrue();
    }

    private void assertColorMode(final int selectedId) {
        for (int menuId: new int[] {10026, 10027, 10028}) {
            assertThat(application.getMenuItem(menuId).isChecked())
                .as("checked state of color mode %s", menuId)
                .isEqualTo(menuId == selectedId);
        }
        assertThat(SystemProperties.isPaletteColor())
            .isEqualTo(selectedId == 10026);
        assertThat(SystemProperties.isRgbColor())
            .isEqualTo(selectedId == 10027);
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
        assertTrue(application.getMenuItem(10028).isChecked());
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
