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

import java.util.List;

import casciian.TKeypress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static casciian.TKeypress.kbBackspaceDel;
import static casciian.TKeypress.kbCtrlEnter;
import static casciian.TKeypress.kbCtrlI;
import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbEsc;
import static casciian.TKeypress.kbLeft;
import static casciian.TKeypress.kbShiftEnter;
import static casciian.TKeypress.kbShiftTab;
import static casciian.TKeypress.kbTab;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Kitty keyboard protocol (CSI u / "disambiguated keys")
 * decoder: sequence parsing, modifier bitmask decoding, event types, and the
 * mapping onto TKeypress.
 */
@DisplayName("Kitty keyboard protocol (CSI u)")
class KittyKeyboardTest {

    /**
     * Parse a sequence and fail the test if it did not decode.
     */
    private static TKeypress key(final String sequence) {
        KittyKeyboard.KeyEvent event = KittyKeyboard.parse(sequence);
        assertNotNull(event, "expected " + sequence + " to decode");
        return event.key();
    }

    // ------------------------------------------------------------------------
    // Disambiguation ---------------------------------------------------------
    // ------------------------------------------------------------------------

    @Nested
    @DisplayName("Key identity is preserved")
    class Disambiguation {

        @Test
        @DisplayName("Ctrl+I stays Ctrl+I and does not collapse into Tab")
        void ctrlIIsNotTab() {
            TKeypress ctrlI = key("\033[105;5u");

            assertEquals(kbCtrlI, ctrlI);
            assertFalse(ctrlI.isFnKey());
            assertEquals('I', ctrlI.getChar());
            assertNotEqualsKey(kbTab, ctrlI);
        }

        @Test
        @DisplayName("Ctrl+M stays Ctrl+M and does not collapse into Enter")
        void ctrlMIsNotEnter() {
            TKeypress ctrlM = key("\033[109;5u");

            assertEquals(TKeypress.kbCtrlM, ctrlM);
            assertNotEqualsKey(kbEnter, ctrlM);
        }

        @Test
        @DisplayName("Tab itself still decodes to the Tab function key")
        void tabIsStillTab() {
            assertEquals(kbTab, key("\033[9u"));
        }

        @Test
        @DisplayName("Shift+Enter is distinct from Enter")
        void shiftEnter() {
            TKeypress shiftEnter = key("\033[13;2u");

            assertEquals(kbShiftEnter, shiftEnter);
            assertTrue(shiftEnter.isFnKey());
            assertEquals(TKeypress.ENTER, shiftEnter.getKeyCode());
            assertNotEqualsKey(kbEnter, shiftEnter);
        }

        @Test
        @DisplayName("Ctrl+Enter is distinct from Enter and Shift+Enter")
        void ctrlEnter() {
            TKeypress ctrlEnter = key("\033[13;5u");

            assertEquals(kbCtrlEnter, ctrlEnter);
            assertNotEqualsKey(kbShiftEnter, ctrlEnter);
        }

        @Test
        @DisplayName("Shift+Tab decodes to Shift+Tab")
        void shiftTab() {
            assertEquals(kbShiftTab, key("\033[9;2u"));
        }

        @Test
        @DisplayName("Ctrl+Shift+Tab is distinct from Shift+Tab")
        void ctrlShiftTab() {
            TKeypress ctrlShiftTab = key("\033[9;6u");

            assertTrue(ctrlShiftTab.isFnKey());
            assertEquals(TKeypress.TAB, ctrlShiftTab.getKeyCode());
            assertTrue(ctrlShiftTab.isCtrl());
            assertTrue(ctrlShiftTab.isShift());
            assertFalse(ctrlShiftTab.isAlt());
            assertNotEqualsKey(kbShiftTab, ctrlShiftTab);
        }

        @Test
        @DisplayName("Ctrl+Shift+Z reports the uppercase letter")
        void ctrlShiftZ() {
            TKeypress ctrlShiftZ = key("\033[122;6u");

            assertFalse(ctrlShiftZ.isFnKey());
            assertEquals('Z', ctrlShiftZ.getChar());
            assertTrue(ctrlShiftZ.isCtrl());
            assertTrue(ctrlShiftZ.isShift());
            assertNotEqualsKey(TKeypress.kbCtrlZ, ctrlShiftZ);
        }

        @Test
        @DisplayName("Escape decodes to the Esc function key")
        void escape() {
            assertEquals(kbEsc, key("\033[27u"));
        }

        @Test
        @DisplayName("A shifted alternate key code wins over case folding")
        void shiftedAlternateKeyCode() {
            // 'a' with the shifted layout reporting 'A'.
            TKeypress shiftA = key("\033[97:65;2u");

            assertEquals('A', shiftA.getChar());
            assertTrue(shiftA.isShift());
        }

        @Test
        @DisplayName("An empty alternate sub-parameter is tolerated")
        void emptyAlternateSubParameter() {
            TKeypress ctrlA = key("\033[97::97;5u");

            assertEquals('A', ctrlA.getChar());
            assertTrue(ctrlA.isCtrl());
        }
    }

    // ------------------------------------------------------------------------
    // Modifier bitmask -------------------------------------------------------
    // ------------------------------------------------------------------------

    @Nested
    @DisplayName("Modifier bitmask decoding")
    class Modifiers {

        @ParameterizedTest(name = "modifier param {0} -> shift={1} alt={2} ctrl={3}")
        @CsvSource({
            // wire value (1 + bitmask), shift, alt, ctrl
            "1,  false, false, false",  // none
            "2,  true,  false, false",  // shift
            "3,  false, true,  false",  // alt
            "4,  true,  true,  false",  // shift+alt
            "5,  false, false, true",   // ctrl
            "6,  true,  false, true",   // shift+ctrl
            "7,  false, true,  true",   // alt+ctrl
            "8,  true,  true,  true",   // shift+alt+ctrl
            "9,  false, false, false",  // super
            "10, true,  false, false",  // super+shift
            "13, false, false, true",   // super+ctrl
            "16, true,  true,  true",   // super+shift+alt+ctrl
        })
        void bitmaskMapsToFlags(final int wireValue, final boolean shift,
            final boolean alt, final boolean ctrl) {

            TKeypress keypress = key("\033[97;" + wireValue + "u");

            assertEquals(shift, keypress.isShift(), "shift");
            assertEquals(alt, keypress.isAlt(), "alt");
            assertEquals(ctrl, keypress.isCtrl(), "ctrl");
        }

        @Test
        @DisplayName("Super is retained on the event even though TKeypress cannot hold it")
        void superIsRetained() {
            KittyKeyboard.KeyEvent event = KittyKeyboard.parse("\033[97;9u");

            assertNotNull(event);
            assertTrue(event.isSuper());
            assertFalse(event.isHyper());
            assertFalse(event.key().isAlt());
            assertEquals('a', event.key().getChar());
        }

        @Test
        @DisplayName("Hyper is retained on the event")
        void hyperIsRetained() {
            KittyKeyboard.KeyEvent event = KittyKeyboard.parse("\033[97;17u");

            assertNotNull(event);
            assertTrue(event.isHyper());
        }

        @Test
        @DisplayName("Meta folds into Alt")
        void metaFoldsIntoAlt() {
            TKeypress keypress = key("\033[97;33u");

            assertTrue(keypress.isAlt());
        }

        @Test
        @DisplayName("Lock state does not become a modifier")
        void locksAreNotModifiers() {
            // Caps Lock (64) + Num Lock (128) active, no real modifier.
            TKeypress keypress = key("\033[97;193u");

            assertFalse(keypress.isShift());
            assertFalse(keypress.isAlt());
            assertFalse(keypress.isCtrl());
        }

        @Test
        @DisplayName("An absent modifier parameter means no modifiers")
        void absentModifierParameter() {
            TKeypress keypress = key("\033[97u");

            assertFalse(keypress.isShift());
            assertFalse(keypress.isAlt());
            assertFalse(keypress.isCtrl());
            assertEquals('a', keypress.getChar());
        }

        @ParameterizedTest(name = "bitmask {0} -> shift={1}")
        @CsvSource({"0, false", "1, true", "4, false", "5, true", "8, false"})
        @DisplayName("isShift reads the Shift bit out of the bitmask")
        void isShiftReadsShiftBit(final int bitmask, final boolean expected) {
            assertEquals(expected, KittyKeyboard.isShift(bitmask));
        }

        @Test
        @DisplayName("isAlt covers both the Alt and Meta bits")
        void isAltCoversMeta() {
            assertTrue(KittyKeyboard.isAlt(KittyKeyboard.MOD_ALT));
            assertTrue(KittyKeyboard.isAlt(KittyKeyboard.MOD_META));
            assertFalse(KittyKeyboard.isAlt(KittyKeyboard.MOD_SUPER));
        }

        @Test
        @DisplayName("isCtrl reads the Ctrl bit")
        void isCtrlReadsCtrlBit() {
            assertTrue(KittyKeyboard.isCtrl(KittyKeyboard.MOD_CTRL));
            assertFalse(KittyKeyboard.isCtrl(KittyKeyboard.MOD_SHIFT
                | KittyKeyboard.MOD_ALT));
        }
    }

    // ------------------------------------------------------------------------
    // Event types ------------------------------------------------------------
    // ------------------------------------------------------------------------

    @Nested
    @DisplayName("Event types")
    class EventTypes {

        @Test
        @DisplayName("An absent event type is a press")
        void defaultIsPress() {
            KittyKeyboard.KeyEvent event = KittyKeyboard.parse("\033[105;5u");

            assertNotNull(event);
            assertEquals(KittyKeyboard.EventType.PRESS, event.type());
            assertFalse(event.isRelease());
        }

        @Test
        @DisplayName(":1 is a press")
        void explicitPress() {
            KittyKeyboard.KeyEvent event = KittyKeyboard.parse("\033[105;5:1u");

            assertNotNull(event);
            assertEquals(KittyKeyboard.EventType.PRESS, event.type());
            assertEquals(kbCtrlI, event.key());
        }

        @Test
        @DisplayName(":2 is a repeat, and still carries the key")
        void repeat() {
            KittyKeyboard.KeyEvent event = KittyKeyboard.parse("\033[105;5:2u");

            assertNotNull(event);
            assertEquals(KittyKeyboard.EventType.REPEAT, event.type());
            assertFalse(event.isRelease());
            assertEquals(kbCtrlI, event.key());
        }

        @Test
        @DisplayName(":3 is flagged as a release")
        void release() {
            KittyKeyboard.KeyEvent event = KittyKeyboard.parse("\033[105;5:3u");

            assertNotNull(event);
            assertEquals(KittyKeyboard.EventType.RELEASE, event.type());
            assertTrue(event.isRelease());
        }

        @Test
        @DisplayName("An unknown event type is rejected rather than guessed")
        void unknownEventType() {
            assertNull(KittyKeyboard.parse("\033[105;5:9u"));
        }
    }

    // ------------------------------------------------------------------------
    // Functional keys --------------------------------------------------------
    // ------------------------------------------------------------------------

    @Nested
    @DisplayName("Functional key identifiers")
    class FunctionalKeys {

        @Test
        @DisplayName("Plain backspace keeps its legacy ^? identity")
        void plainBackspace() {
            assertEquals(kbBackspaceDel, key("\033[127u"));
        }

        @Test
        @DisplayName("Ctrl+Backspace is the Backspace function key")
        void ctrlBackspace() {
            assertEquals(TKeypress.kbCtrlBackspace, key("\033[127;5u"));
        }

        @Test
        @DisplayName("Keypad Enter maps to Enter")
        void keypadEnter() {
            assertEquals(kbEnter, key("\033[57414u"));
        }

        @Test
        @DisplayName("Keypad digits map to their digit character")
        void keypadDigits() {
            assertEquals('0', key("\033[57399u").getChar());
            assertEquals('5', key("\033[57404u").getChar());
            assertEquals('9', key("\033[57408u").getChar());
        }

        @Test
        @DisplayName("Keypad operators map to their character")
        void keypadOperators() {
            assertEquals('.', key("\033[57409u").getChar());
            assertEquals('/', key("\033[57410u").getChar());
            assertEquals('*', key("\033[57411u").getChar());
            assertEquals('-', key("\033[57412u").getChar());
            assertEquals('+', key("\033[57413u").getChar());
        }

        @Test
        @DisplayName("Keypad navigation maps to the arrow and paging keys")
        void keypadNavigation() {
            assertEquals(kbLeft, key("\033[57417u"));
            assertEquals(TKeypress.kbUp, key("\033[57419u"));
            assertEquals(TKeypress.kbPgDn, key("\033[57422u"));
            assertEquals(TKeypress.kbHome, key("\033[57423u"));
            assertEquals(TKeypress.kbDel, key("\033[57427u"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "\033[57358u",  // Caps Lock
            "\033[57362u",  // Pause
            "\033[57376u",  // F13, which Casciian does not model
            "\033[57441u",  // Left Shift reported as its own key
            "\033[57430u",  // a media key
        })
        @DisplayName("Keys Casciian cannot represent are dropped")
        void unrepresentableKeysAreDropped(final String sequence) {
            assertNull(KittyKeyboard.parse(sequence));
        }
    }

    // ------------------------------------------------------------------------
    // Malformed input --------------------------------------------------------
    // ------------------------------------------------------------------------

    @Nested
    @DisplayName("Malformed input")
    class Malformed {

        @ParameterizedTest
        @ValueSource(strings = {
            "",             // empty
            "\033[u",       // no key code
            "\033[105;5",   // no final byte
            "\033[105;5m",  // wrong final byte
            "\033[abc;5u",  // non-numeric key code
            "\033[0u",      // NUL is not a valid key code
            "\033[9;5D",    // not a CSI u sequence at all
        })
        @DisplayName("Bad sequences decode to null instead of throwing")
        void badSequences(final String sequence) {
            assertNull(KittyKeyboard.parse(sequence));
        }

        @Test
        @DisplayName("A null sequence decodes to null")
        void nullSequence() {
            assertNull(KittyKeyboard.parse((String) null));
        }

        @Test
        @DisplayName("Null and empty parameter lists decode to null")
        void nullParams() {
            assertNull(KittyKeyboard.parse((List<String>) null));
            assertNull(KittyKeyboard.parse(List.of()));
        }

        @Test
        @DisplayName("A non-numeric modifier is treated as no modifiers")
        void nonNumericModifier() {
            TKeypress keypress = key("\033[97;xu");

            assertFalse(keypress.isShift());
            assertFalse(keypress.isAlt());
            assertFalse(keypress.isCtrl());
        }
    }

    // ------------------------------------------------------------------------
    // Parameter list entry point ---------------------------------------------
    // ------------------------------------------------------------------------

    @Nested
    @DisplayName("Parameter list entry point")
    class ParameterList {

        @Test
        @DisplayName("Parameters split by the terminal decode the same way")
        void parametersFromTerminal() {
            KittyKeyboard.KeyEvent event = KittyKeyboard.parse(
                List.of("105", "5"));

            assertNotNull(event);
            assertEquals(kbCtrlI, event.key());
        }

        @Test
        @DisplayName("Colon sub-parameters survive the split")
        void subParametersFromTerminal() {
            KittyKeyboard.KeyEvent event = KittyKeyboard.parse(
                List.of("13", "2:3"));

            assertNotNull(event);
            assertEquals(kbShiftEnter, event.key());
            assertTrue(event.isRelease());
        }

        @Test
        @DisplayName("A trailing text parameter is ignored")
        void trailingTextParameter() {
            KittyKeyboard.KeyEvent event = KittyKeyboard.parse(
                List.of("97", "1", "97"));

            assertNotNull(event);
            assertEquals('a', event.key().getChar());
        }
    }

    /**
     * Assert that two keystrokes are different.  TKeypress.equals() compares
     * every field, so this is the check that proves disambiguation worked.
     */
    private static void assertNotEqualsKey(final TKeypress unexpected,
        final TKeypress actual) {

        assertFalse(unexpected.equals(actual),
            "expected " + actual + " to differ from " + unexpected);
    }

}
