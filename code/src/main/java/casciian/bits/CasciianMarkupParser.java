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
package casciian.bits;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * CasciianMarkupParser parses a lightweight, human-readable markup language
 * into a {@link RichText} model, so styled text can be authored without
 * hand-writing ANSI escape sequences.
 *
 * <p>Supported syntax:</p>
 *
 * <pre>
 * Normal text
 *
 * [bold]bold text[/]
 *
 * [bold italic fg=#ffcc00]
 * Multiple attributes
 * [/]
 *
 * [fg=#ff0000]RGB foreground[/]
 * [bg=#202020]RGB background[/]
 * [fg=palette:214 bg=palette:17]256-color palette[/]
 * [u=curly]Curly underline[/]
 * [link="https://github.com/crramirez/casciian"]Casciian repository[/]
 * </pre>
 *
 * <p>Tags nest, and the shorthand {@code [/]} closes the most recently opened
 * scope.  An explicit closing tag such as {@code [/bold]} is also accepted and
 * closes the most recently opened scope (the name is not validated).</p>
 *
 * <p>To write a literal {@code [}, double it: {@code [[} renders as
 * {@code [}.</p>
 *
 * <p>Supported style tokens (whitespace-separated inside a tag):</p>
 * <ul>
 *   <li>{@code bold}, {@code faint} (alias {@code dim}), {@code italic},
 *       {@code blink}, {@code reverse}, {@code hidden},
 *       {@code strike} (alias {@code strikethrough})</li>
 *   <li>{@code underline} (alias {@code u}) for a single underline, or
 *       {@code u=<style>} / {@code underline=<style>} where {@code <style>} is
 *       one of {@code none}, {@code single}, {@code double}, {@code curly},
 *       {@code dotted}, {@code dashed}</li>
 *   <li>{@code fg=<color>} and {@code bg=<color>} where {@code <color>} is
 *       {@code #rrggbb}, {@code palette:<0-255>}, {@code default}, or a named
 *       color ({@code black}, {@code red}, {@code green}, {@code yellow},
 *       {@code blue}, {@code magenta}, {@code cyan}, {@code white}, their
 *       {@code bright*} variants, and {@code gray} / {@code grey})</li>
 *   <li>{@code link="<uri>"} (quotes optional) to set an OSC 8 hyperlink</li>
 * </ul>
 */
public final class CasciianMarkupParser {

    /**
     * Private constructor - utility class.
     */
    private CasciianMarkupParser() {
    }

    /**
     * Parse a markup string into a {@link RichText}.
     *
     * @param markup the markup text (may be null)
     * @return the parsed RichText (never null; empty when markup is null/empty)
     */
    public static RichText parse(final String markup) {
        RichText.Builder builder = RichText.builder();
        if (markup == null || markup.isEmpty()) {
            return builder.build();
        }

        // Scope stack.  The bottom entry is the default style.
        Deque<CellAttributes> scopes = new ArrayDeque<>();
        scopes.push(RichText.defaultAttributes());

        StringBuilder run = new StringBuilder();
        int i = 0;
        int len = markup.length();
        while (i < len) {
            char c = markup.charAt(i);
            if (c == '[') {
                // Escaped literal "[["
                if (i + 1 < len && markup.charAt(i + 1) == '[') {
                    run.append('[');
                    i += 2;
                    continue;
                }
                int close = findClose(markup, i + 1);
                if (close < 0) {
                    // No closing bracket: treat the remainder as literal text.
                    run.append(markup.substring(i));
                    break;
                }
                String body = markup.substring(i + 1, close).trim();
                // Flush the buffered run using the current (pre-change) style.
                flush(builder, run, scopes.peek());
                if (body.startsWith("/")) {
                    if (scopes.size() > 1) {
                        scopes.pop();
                    }
                } else {
                    CellAttributes attr = parseTag(body, scopes.peek());
                    scopes.push(attr);
                }
                i = close + 1;
            } else {
                run.append(c);
                i++;
            }
        }
        flush(builder, run, scopes.peek());
        return builder.build();
    }

    // ------------------------------------------------------------------------
    // Private helpers --------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Flush the buffered run text into the builder with the given attributes,
     * then clear the buffer.
     *
     * @param builder the target builder
     * @param run the run text buffer (cleared afterwards)
     * @param attr the attributes for the buffered text
     */
    private static void flush(final RichText.Builder builder,
        final StringBuilder run, final CellAttributes attr) {

        if (run.length() > 0) {
            builder.append(run.toString(), attr);
            run.setLength(0);
        }
    }

    /**
     * Find the index of the {@code ]} that closes a tag started at
     * {@code start}, ignoring {@code ]} characters that appear inside a
     * double-quoted value.
     *
     * @param s the source string
     * @param start the index just after the opening {@code [}
     * @return the index of the closing {@code ]}, or -1 if none
     */
    private static int findClose(final String s, final int start) {
        boolean inQuote = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ']' && !inQuote) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Parse a tag body into a new {@link CellAttributes}, starting from a copy
     * of the enclosing scope's attributes so that nested tags inherit outer
     * styling.
     *
     * @param body the tag body (already trimmed, without the brackets)
     * @param base the enclosing scope's attributes
     * @return a new CellAttributes with the tag's tokens applied
     */
    private static CellAttributes parseTag(final String body,
        final CellAttributes base) {

        CellAttributes attr = new CellAttributes(base);
        for (String token : tokenize(body)) {
            applyToken(token, attr);
        }
        return attr;
    }

    /**
     * Split a tag body into tokens on whitespace, keeping quoted values (which
     * may contain whitespace) intact.
     *
     * @param body the tag body
     * @return the list of tokens
     */
    private static List<String> tokenize(final String body) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                token.append(c);
            } else if (Character.isWhitespace(c) && !inQuote) {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
            } else {
                token.append(c);
            }
        }
        if (token.length() > 0) {
            tokens.add(token.toString());
        }
        return tokens;
    }

    /**
     * Apply a single style token to the given attributes.  Unknown tokens are
     * ignored.
     *
     * @param token the token (e.g. "bold", "fg=#ff0000", "u=curly")
     * @param attr the attributes to modify
     */
    private static void applyToken(final String token,
        final CellAttributes attr) {

        int eq = token.indexOf('=');
        String key = (eq < 0 ? token : token.substring(0, eq))
            .toLowerCase(Locale.ROOT);
        String value = (eq < 0) ? null : unquote(token.substring(eq + 1));

        switch (key) {
        case "bold":
            attr.setBold(true);
            return;
        case "faint", "dim":
            attr.setFaint(true);
            return;
        case "italic":
            attr.setItalic(true);
            return;
        case "blink":
            attr.setBlink(true);
            return;
        case "reverse":
            attr.setReverse(true);
            return;
        case "hidden":
            attr.setHidden(true);
            return;
        case "strike", "strikethrough":
            attr.setStrikethrough(true);
            return;
        case "underline", "u":
            attr.setUnderlineStyle(value == null
                ? CellAttributes.UNDERLINE_STYLE_SINGLE
                : underlineStyle(value));
            return;
        case "fg", "foreground":
            if (value != null) {
                applyColor(value, true, attr);
            }
            return;
        case "bg", "background":
            if (value != null) {
                applyColor(value, false, attr);
            }
            return;
        case "link":
            if (value != null) {
                attr.setHyperlink(value.isEmpty() ? null : value);
                if (attr.getUnderlineStyle()
                        == CellAttributes.UNDERLINE_STYLE_NONE) {
                    attr.setUnderlineStyle(
                        CellAttributes.UNDERLINE_STYLE_SINGLE);
                }
            }
            return;
        default:
            // Unknown token: ignore.
        }
    }

    /**
     * Map an underline style name to a {@link CellAttributes} underline style
     * constant.
     *
     * @param value the style name
     * @return the underline style constant (single for unknown values)
     */
    private static int underlineStyle(final String value) {
        switch (value.toLowerCase(Locale.ROOT)) {
        case "none", "off", "0":
            return CellAttributes.UNDERLINE_STYLE_NONE;
        case "single", "1":
            return CellAttributes.UNDERLINE_STYLE_SINGLE;
        case "double", "2":
            return CellAttributes.UNDERLINE_STYLE_DOUBLE;
        case "curly", "3":
            return CellAttributes.UNDERLINE_STYLE_CURLY;
        case "dotted", "4":
            return CellAttributes.UNDERLINE_STYLE_DOTTED;
        case "dashed", "5":
            return CellAttributes.UNDERLINE_STYLE_DASHED;
        default:
            return CellAttributes.UNDERLINE_STYLE_SINGLE;
        }
    }

    /**
     * Apply a color value to either the foreground or background of the given
     * attributes.  Recognizes {@code #rrggbb}, {@code palette:N}, the keyword
     * {@code default}, and named colors.  Unrecognized values are ignored.
     *
     * @param value the color value
     * @param foreground true for foreground, false for background
     * @param attr the attributes to modify
     */
    private static void applyColor(final String value,
        final boolean foreground, final CellAttributes attr) {

        String v = value.toLowerCase(Locale.ROOT);

        if (v.equals("default")) {
            attr.setDefaultColor(foreground, true);
            return;
        }

        if (v.startsWith("#")) {
            Integer rgb = parseHex(v.substring(1));
            if (rgb != null) {
                if (foreground) {
                    attr.setForeColorRGB(rgb);
                } else {
                    attr.setBackColorRGB(rgb);
                }
                attr.setDefaultColor(foreground, false);
            }
            return;
        }

        if (v.startsWith("palette:")) {
            Integer idx = parseInt(v.substring("palette:".length()));
            if (idx != null && idx >= 0 && idx <= 255) {
                if (foreground) {
                    attr.setForeColorPalette(idx);
                } else {
                    attr.setBackColorPalette(idx);
                }
                attr.setDefaultColor(foreground, false);
            }
            return;
        }

        Color named = namedColor(v);
        if (named != null) {
            if (foreground) {
                attr.setForeColor(named);
            } else {
                attr.setBackColor(named);
            }
            attr.setDefaultColor(foreground, false);
        }
    }

    /**
     * Map a color name to a {@link Color} constant.
     *
     * @param name the lower-case color name
     * @return the color, or null if the name is not recognized
     */
    private static Color namedColor(final String name) {
        switch (name) {
        case "black":
            return Color.BLACK;
        case "red":
            return Color.RED;
        case "green":
            return Color.GREEN;
        case "yellow":
            return Color.YELLOW;
        case "blue":
            return Color.BLUE;
        case "magenta":
            return Color.MAGENTA;
        case "cyan":
            return Color.CYAN;
        case "white":
            return Color.WHITE;
        case "gray", "grey", "brightblack":
            return Color.BRIGHT_BLACK;
        case "brightred":
            return Color.BRIGHT_RED;
        case "brightgreen":
            return Color.BRIGHT_GREEN;
        case "brightyellow":
            return Color.BRIGHT_YELLOW;
        case "brightblue":
            return Color.BRIGHT_BLUE;
        case "brightmagenta":
            return Color.BRIGHT_MAGENTA;
        case "brightcyan":
            return Color.BRIGHT_CYAN;
        case "brightwhite":
            return Color.BRIGHT_WHITE;
        default:
            return null;
        }
    }

    /**
     * Remove a single pair of surrounding double quotes from a value.
     *
     * @param value the raw value
     * @return the value without surrounding quotes
     */
    private static String unquote(final String value) {
        if (value.length() >= 2 && value.charAt(0) == '"'
                && value.charAt(value.length() - 1) == '"') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Parse a 6-digit hex color.  Returns null when the string is not exactly
     * six hexadecimal digits.
     *
     * @param hex the hex digits (without leading '#')
     * @return the RGB value, or null if invalid
     */
    private static Integer parseHex(final String hex) {
        if (hex.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse a base-10 integer, returning null on failure.
     *
     * @param s the string
     * @return the integer, or null if invalid
     */
    private static Integer parseInt(final String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
