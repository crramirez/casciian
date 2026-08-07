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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link THyperLink}.
 *
 * <p>Note: link activation (e.g. Ctrl+Click) is handled entirely by the
 * terminal via OSC 8 and is intentionally not tested here.
 * </p>
 */
class THyperLinkTest {

    @Test
    void testConstructionAndAccessors() {
        THyperLink link = new THyperLink(null, "Casciian",
            "https://example.com", 0, 0);

        assertEquals("Casciian", link.getLabel());
        assertEquals("https://example.com", link.getUri());

        link.setUri("https://other.example.com");
        assertEquals("https://other.example.com", link.getUri());
   }

    @Test
    void testWidthMatchesVisibleText() {
        THyperLink link = new THyperLink(null, "GitHub",
            "https://github.com", 0, 0);
        assertEquals("GitHub".length(), link.getWidth());
    }
}
