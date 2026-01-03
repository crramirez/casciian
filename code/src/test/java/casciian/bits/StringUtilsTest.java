package casciian.bits;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StringUtils}, covering text wrapping, alignment, and
 * related utility methods, including behavior with whitespace, newlines, and
 * Unicode or variable-width characters.
 */
class StringUtilsTest {

    @Test
    void testLeft() {
        String input = "The quick brown fox jumps over the lazy dog";
        List<String> result = StringUtils.left(input, 15);
        assertEquals(Arrays.asList("The quick brown", "fox jumps over", "the lazy dog"), result);

        // Test with newline
        input = "First paragraph\nSecond paragraph";
        result = StringUtils.left(input, 10);
        assertEquals(Arrays.asList("First", "paragraph", "Second", "paragraph"), result);

        // Test with tabs and multiple spaces; ensure the method handles such input without error
        input = "Word\twith\t\ttabs   and spaces";
        result = StringUtils.left(input, 10);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Let's test basic left justification
        assertEquals(Arrays.asList("a", "b"), StringUtils.left("a b", 1));
        assertEquals(Arrays.asList("a b"), StringUtils.left("a b", 3));

        // Word longer than n
        // Current implementation adds an empty string if the first word is longer than n
        assertEquals(Arrays.asList("", "supercalifragilistic"), StringUtils.left("supercalifragilistic", 5));
    }

    @Test
    void testRight() {
        String input = "Hello world";
        List<String> result = StringUtils.right(input, 15);
        assertEquals(1, result.size());
        assertEquals("    Hello world", result.get(0));

        result = StringUtils.right(input, 5);
        assertEquals(Arrays.asList("Hello", "world"), result);
    }

    @Test
    void testCenter() {
        String input = "Hello";
        List<String> result = StringUtils.center(input, 10);
        assertEquals(1, result.size());
        // (10 - 5) / 2 = 2. r = 10 - 5 - 2 = 3.
        assertEquals("  Hello   ", result.get(0));
    }

    @Test
    void testFull() {
        String input = "The quick brown fox jumps";
        // "The quick brown fox jumps"
        // width: 3+1+5+1+5+1+3+1+5 = 25
        List<String> result = StringUtils.full(input, 10);
        // left(input, 10) -> ["The quick", "brown fox", "jumps"]
        // line 1: "The quick" -> words ["The", "quick"]. charCount 8. spaceCount 10-8=2. q=2/1=2. r=0.
        // result: "The  quick"
        // line 2: "brown fox" -> words ["brown", "fox"]. charCount 8. spaceCount 10-8=2. q=2/1=2. r=0.
        // result: "brown  fox"
        // line 3: "jumps" (last line, no full justification)
        // result: "jumps"
        assertEquals(Arrays.asList("The  quick", "brown  fox", "jumps"), result);

        // Single word line
        assertEquals(Arrays.asList("word"), StringUtils.full("word", 10));
    }

    @Test
    void testUnescape() {
        assertEquals("\\n\\r\\t\\b\\f", StringUtils.unescape("\n\r\t\b\f"));
        assertEquals("^?", StringUtils.unescape("\u007f"));
        assertEquals(" ", StringUtils.unescape("\u0001")); // default case
        assertEquals("Hello", StringUtils.unescape("Hello"));
    }

    @Test
    void testCsv() {
        List<String> fields = Arrays.asList("a", "b", "c");
        String csv = StringUtils.toCsv(fields);
        assertEquals("a,b,c", csv);
        assertEquals(fields, StringUtils.fromCsv(csv));

        fields = Arrays.asList("a,b", "c\"d", "e");
        csv = StringUtils.toCsv(fields);
        // "a,b" -> "\"a,b\""
        // "c\"d" -> "\"c\"\"d\""
        assertEquals("\"a,b\",\"c\"\"d\",e", csv);
        assertEquals(fields, StringUtils.fromCsv(csv));

        // Unmatched quote case in fromCsv
        // StringUtils.java line 306-313: unmatched double-quote and comma.
        // It treats it as a quote terminating the field and appends a quote character.
        // "a\",b" -> ["a\"", "b"] ? Let's see.
        List<String> result = StringUtils.fromCsv("a\",b");
        assertEquals(Arrays.asList("a\"", "b"), result);
    }

    @Test
    void testWidth() {
        assertEquals(1, StringUtils.width('A'));
        assertEquals(0, StringUtils.width('\n'));
        assertEquals(2, StringUtils.width('\u4E2D')); // 中 - CJK Unified Ideograph
        
        assertEquals(5, StringUtils.width("Hello"));
        assertEquals(4, StringUtils.width("\u4E2D\u6587")); // 中文 -> 2+2=4.

        // Test Regional Indicators (requires 2 cells)
        // RI for 'U' is U+1F1FA, RI for 'S' is U+1F1F8
        List<Integer> usFlag = Arrays.asList(0x1F1FA, 0x1F1F8);
        assertEquals(2, StringUtils.width(usFlag));
        
        int[] usFlagArray = new int[]{0x1F1FA, 0x1F1F8};
        assertEquals(2, StringUtils.width(usFlagArray));

        // width(null)
        assertEquals(0, StringUtils.width((String) null));

        // Test Cell width
        Cell cell = new Cell('A');
        assertEquals(1, StringUtils.width(cell));

        ComplexCell complexCell = new ComplexCell(new int[]{0x41, 0x301}); // A + combining acute accent
        // width(ComplexCell) calls getCodePoints() and returns the max width of any codepoint.
        // Under the current Unicode width implementation:
        //   - width('A' / 0x41) = 1
        //   - width(0x301) = 0 because it is a combining character
        // Therefore, width(complexCell) = max(width('A'), width(0x301)) = max(1, 0) = 1.
        assertEquals(1, StringUtils.width(complexCell));
        
        ComplexCell cjkCell = new ComplexCell(0x4E2D); // 中
        assertEquals(2, StringUtils.width(cjkCell));

        assertEquals(2, StringUtils.width(new ComplexCell(0x1F4CB))); //Paste
        assertEquals(1, StringUtils.width(new ComplexCell(0x261D)));  //Size move
        assertEquals(1, StringUtils.width(new ComplexCell(0x1F5D0))); //Copy
        assertEquals(1, StringUtils.width(new ComplexCell(0x1F5AE)));
        assertEquals(1, StringUtils.width(new ComplexCell(0x1F5D9)));
        assertEquals(1, StringUtils.width(new ComplexCell(0x2B6F)));
        assertEquals(1, StringUtils.width(new ComplexCell(0x2B6E)));
        assertEquals(1, StringUtils.width(new ComplexCell(0x1F5F6)));
        assertEquals(1, StringUtils.width(new ComplexCell(0x1F5D7)));

    }

    @Test
    void testToCodePoints() {
        String input = "A\uD83D\uDE00B"; // A, 😀, B
        int[] expected = {0x41, 0x1F600, 0x42};
        assertArrayEquals(expected, StringUtils.toCodePoints(input));
    }

    @Test
    void testToComplexCells() {
        String input = "A\uD83D\uDE00B";
        List<ComplexCell> cells = StringUtils.toComplexCells(input);
        assertNotNull(cells);
        assertEquals(3, cells.size());
        assertEquals(0x41, cells.get(0).getChar());
        assertEquals(0x1F600, cells.get(1).getChar());
        assertEquals(0x42, cells.get(2).getChar());
    }

    @Test
    void testBase64() {
        byte[] data = {1, 2, 3, 4, 5};
        String base64 = StringUtils.toBase64(data);
        assertArrayEquals(data, StringUtils.fromBase64(base64.getBytes()));
        
        assertEquals("", StringUtils.toBase64(null));
        assertEquals("", StringUtils.toBase64(new byte[0]));
        
        // Base64.getMimeDecoder().decode() ignores non-base64 characters.
        // So "!!!" returns an empty array.
        assertArrayEquals(new byte[0], StringUtils.fromBase64("!!!".getBytes()));
    }
}
