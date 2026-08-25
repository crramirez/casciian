package casciian.bits;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BorderStyleTest {

    @Test
    void toStringUsesStyleLabels() {
        assertEquals("none", BorderStyle.NONE.toString());
        assertEquals("single", BorderStyle.SINGLE.toString());
        assertEquals("double", BorderStyle.DOUBLE.toString());
        assertEquals("round", BorderStyle.SINGLE_ROUND.toString());
        assertEquals("singleVdoubleH", BorderStyle.SINGLE_V_DOUBLE_H.toString());
        assertEquals("singleHdoubleV", BorderStyle.SINGLE_H_DOUBLE_V.toString());
    }

    @Test
    void defaultAndUnknownResolveToSingleLabel() {
        assertEquals("single", BorderStyle.getStyle("default").toString());
        assertEquals("single", BorderStyle.getStyle("unknown-style").toString());
    }
}
