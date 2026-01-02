/*
 * Casciian - Java Text User Interface
 *
 * Unit tests for MnemonicString
 */
package casciian.bits;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MnemonicString - validates mnemonic string parsing functionality.
 */
@DisplayName("MnemonicString Tests")
class MnemonicStringTest {

    @Test
    @DisplayName("Simple mnemonic at start: &File")
    void testSimpleMnemonicAtStart() {
        MnemonicString ms = new MnemonicString("&File");
        
        assertEquals("File", ms.getRawLabel());
        assertEquals('F', ms.getShortcut());
        assertEquals(0, ms.getShortcutIdx());
        assertEquals(0, ms.getScreenShortcutIdx());
    }

    @Test
    @DisplayName("Mnemonic in middle: Fi&le")
    void testMnemonicInMiddle() {
        MnemonicString ms = new MnemonicString("Fi&le");
        
        assertEquals("File", ms.getRawLabel());
        assertEquals('l', ms.getShortcut());
        assertEquals(2, ms.getShortcutIdx());
        assertEquals(2, ms.getScreenShortcutIdx());
    }

    @Test
    @DisplayName("Mnemonic at end: Fil&e")
    void testMnemonicAtEnd() {
        MnemonicString ms = new MnemonicString("Fil&e");
        
        assertEquals("File", ms.getRawLabel());
        assertEquals('e', ms.getShortcut());
        assertEquals(3, ms.getShortcutIdx());
        assertEquals(3, ms.getScreenShortcutIdx());
    }

    @Test
    @DisplayName("Escaped ampersand: File && Stuff")
    void testEscapedAmpersand() {
        MnemonicString ms = new MnemonicString("File && Stuff");
        
        assertEquals("File & Stuff", ms.getRawLabel());
        assertEquals(0, ms.getShortcut());  // No shortcut
        assertEquals(-1, ms.getShortcutIdx());
    }

    @Test
    @DisplayName("Mnemonic with escaped ampersand: &File && Stuff")
    void testMnemonicWithEscapedAmpersand() {
        MnemonicString ms = new MnemonicString("&File && Stuff");
        
        assertEquals("File & Stuff", ms.getRawLabel());
        assertEquals('F', ms.getShortcut());
        assertEquals(0, ms.getShortcutIdx());
        assertEquals(0, ms.getScreenShortcutIdx());
    }

    @Test
    @DisplayName("Only first mnemonic is used: &File &Edit")
    void testOnlyFirstMnemonicUsed() {
        MnemonicString ms = new MnemonicString("&File &Edit");
        
        // The raw label should have both ampersands removed for the shortcut characters
        // but only the first 'F' should be the shortcut
        assertEquals("File Edit", ms.getRawLabel());
        assertEquals('F', ms.getShortcut());
        assertEquals(0, ms.getShortcutIdx());
        assertEquals(0, ms.getScreenShortcutIdx());
    }

    @Test
    @DisplayName("Second mnemonic is ignored but character is kept: O&pen &Save")
    void testSecondMnemonicIgnoredButCharacterKept() {
        MnemonicString ms = new MnemonicString("O&pen &Save");
        
        // The raw label should be "Open Save" with both & removed
        assertEquals("Open Save", ms.getRawLabel());
        assertEquals('p', ms.getShortcut());
        assertEquals(1, ms.getShortcutIdx());
        assertEquals(1, ms.getScreenShortcutIdx());
    }

    @Test
    @DisplayName("No mnemonic: File")
    void testNoMnemonic() {
        MnemonicString ms = new MnemonicString("File");
        
        assertEquals("File", ms.getRawLabel());
        assertEquals(0, ms.getShortcut());
        assertEquals(-1, ms.getShortcutIdx());
    }

    @Test
    @DisplayName("Empty string")
    void testEmptyString() {
        MnemonicString ms = new MnemonicString("");
        
        assertEquals("", ms.getRawLabel());
        assertEquals(0, ms.getShortcut());
        assertEquals(-1, ms.getShortcutIdx());
    }

    @Test
    @DisplayName("Just ampersand: &")
    void testJustAmpersand() {
        MnemonicString ms = new MnemonicString("&");
        
        // Trailing ampersand with no following character
        assertEquals("", ms.getRawLabel());
        assertEquals(0, ms.getShortcut());
        assertEquals(-1, ms.getShortcutIdx());
    }

    @Test
    @DisplayName("Wide character before mnemonic")
    void testWideCharacterBeforeMnemonic() {
        // Japanese character 日 (takes 2 screen cells)
        MnemonicString ms = new MnemonicString("日本&語");
        
        assertEquals("日本語", ms.getRawLabel());
        assertEquals('語', ms.getShortcut());
        assertEquals(2, ms.getShortcutIdx());
        // Screen position should account for wide chars: 日 (2) + 本 (2) = 4
        assertEquals(4, ms.getScreenShortcutIdx());
    }

    @Test
    @DisplayName("Unicode mnemonic character")
    void testUnicodeMnemonicCharacter() {
        // Mnemonic on a Unicode character
        MnemonicString ms = new MnemonicString("&日本語");
        
        assertEquals("日本語", ms.getRawLabel());
        assertEquals('日', ms.getShortcut());
        assertEquals(0, ms.getShortcutIdx());
        assertEquals(0, ms.getScreenShortcutIdx());
    }

    @Test
    @DisplayName("Multiple escaped ampersands: A && B && C")
    void testMultipleEscapedAmpersands() {
        MnemonicString ms = new MnemonicString("A && B && C");
        
        assertEquals("A & B & C", ms.getRawLabel());
        assertEquals(0, ms.getShortcut());
        assertEquals(-1, ms.getShortcutIdx());
    }

    @Test
    @DisplayName("Mnemonic after escaped ampersand: A && &B")
    void testMnemonicAfterEscapedAmpersand() {
        MnemonicString ms = new MnemonicString("A && &B");
        
        assertEquals("A & B", ms.getRawLabel());
        assertEquals('B', ms.getShortcut());
        assertEquals(4, ms.getShortcutIdx());
        assertEquals(4, ms.getScreenShortcutIdx());
    }
}
