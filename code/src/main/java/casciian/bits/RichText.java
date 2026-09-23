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
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * RichText is a lightweight, effectively immutable representation of styled
 * text.  It is the canonical internal model for rich text in Casciian: ANSI
 * escape sequences, the {@link CasciianMarkupParser Casciian markup language},
 * and the programmatic {@link Builder} all produce a {@code RichText}, which
 * can then be laid out into {@link Cell} grids for a given width by
 * {@link AnsiParser#layout(RichText, int)}.
 *
 * <pre>
 * ANSI ──────────────┐
 * Casciian Markup ───┼──&gt; RichText ──&gt; layout/wrapping ──&gt; Cells
 * Java Builder ──────┤
 * Markdown (future) ─┘
 * </pre>
 *
 * <p>
 * A {@code RichText} is a sequence of {@link Run}s.  Each run is one
 * contiguous string sharing the same {@link CellAttributes}.  The model is
 * deliberately independent of terminal width and of ANSI: it does not convert
 * characters into cells and it does not perform wrapping.  Those concerns
 * belong to the layout stage.
 * </p>
 */
public final class RichText {

    // ------------------------------------------------------------------------
    // Inner class: Run -------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * A run is one contiguous string of text sharing a single
     * {@link CellAttributes}.  Runs are immutable; the attributes are
     * defensively copied on the way in and on the way out so callers cannot
     * mutate the shared state of a {@link RichText}.
     */
    public static final class Run {

        /**
         * The run text.  May contain newlines and other control characters.
         */
        private final String text;

        /**
         * The attributes shared by every character in this run.  This
         * instance is private to the run and is never handed out directly.
         */
        private final CellAttributes attributes;

        /**
         * Package-private constructor.  Defensively copies the attributes.
         *
         * @param text the run text
         * @param attributes the attributes for the run (copied)
         */
        Run(final String text, final CellAttributes attributes) {
            this.text = (text == null) ? "" : text;
            this.attributes = new CellAttributes(attributes);
        }

        /**
         * Get the run text.
         *
         * @return the text of this run
         */
        public String getText() {
            return text;
        }

        /**
         * Get a copy of this run's attributes.  The returned instance is a
         * defensive copy; mutating it has no effect on the run.
         *
         * @return a copy of the run attributes
         */
        public CellAttributes getAttributes() {
            return new CellAttributes(attributes);
        }

        /**
         * Package-private accessor that returns the run's own attributes
         * without copying, for internal read-only use (e.g. layout).
         *
         * @return the run attributes (do not mutate)
         */
        CellAttributes attributes() {
            return attributes;
        }

        @Override
        public boolean equals(final Object rhs) {
            if (!(rhs instanceof Run that)) {
                return false;
            }
            return text.equals(that.text)
                && attributes.equals(that.attributes);
        }

        @Override
        public int hashCode() {
            return (31 * text.hashCode()) + attributes.hashCode();
        }

        @Override
        public String toString() {
            return "Run(" + text + ", " + attributes + ")";
        }
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The runs, in order.  Unmodifiable.
     */
    private final List<Run> runs;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Package-private constructor.  Use {@link #builder()} or one of the
     * parsers to obtain a {@code RichText}.
     *
     * @param runs the runs (already merged); wrapped unmodifiable
     */
    RichText(final List<Run> runs) {
        this.runs = Collections.unmodifiableList(new ArrayList<>(runs));
    }

    // ------------------------------------------------------------------------
    // Public API -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the runs of this rich text, read-only.
     *
     * @return an unmodifiable list of runs
     */
    public List<Run> getRuns() {
        return runs;
    }

    /**
     * Return true if this rich text contains no characters.
     *
     * @return true if there are no runs (or only empty runs)
     */
    public boolean isEmpty() {
        for (Run run : runs) {
            if (!run.getText().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the plain text (all run text concatenated, without styling).
     *
     * @return the concatenated text of every run
     */
    public String getPlainText() {
        StringBuilder sb = new StringBuilder();
        for (Run run : runs) {
            sb.append(run.getText());
        }
        return sb.toString();
    }

    /**
     * Get a new {@link Builder} instance.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof RichText that)) {
            return false;
        }
        return runs.equals(that.runs);
    }

    @Override
    public int hashCode() {
        return runs.hashCode();
    }

    @Override
    public String toString() {
        return "RichText" + runs;
    }

    // ------------------------------------------------------------------------
    // Builder ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Builder provides a convenient, fluent API for assembling a
     * {@link RichText} programmatically.
     *
     * <pre>{@code
     * RichText text = RichText.builder()
     *     .append("Build: ")
     *     .append("successful",
     *         CellAttributes.builder()
     *             .bold(true)
     *             .foreColorRGB(0x32CD70)
     *             .build())
     *     .append("\nDocumentation: ")
     *     .link("Casciian repository",
     *         "https://github.com/crramirez/casciian")
     *     .build();
     * }</pre>
     *
     * <p>
     * The builder keeps a stack of active styles so temporary styles can be
     * nested with {@link #push(CellAttributes)} / {@link #pop()}; text appended
     * without explicit attributes uses the style currently on top of the
     * stack.
     * </p>
     */
    public static final class Builder {

        /**
         * The runs collected so far.  Adjacent runs with equal attributes are
         * merged as they are appended.
         */
        private final List<Run> runs = new ArrayList<>();

        /**
         * Stack of active styles.  The bottom of the stack is the default
         * style; {@link #push(CellAttributes)} adds a temporary style.
         */
        private final Deque<CellAttributes> styles = new ArrayDeque<>();

        /**
         * Public constructor.
         */
        public Builder() {
            styles.push(defaultAttributes());
        }

        /**
         * Append plain text using the current (top-of-stack) style.
         *
         * @param text the text to append
         * @return this builder
         */
        public Builder append(final String text) {
            return append(text, styles.peek());
        }

        /**
         * Append text with the given attributes.
         *
         * @param text the text to append
         * @param attributes the attributes for the text
         * @return this builder
         */
        public Builder append(final String text,
            final CellAttributes attributes) {

            if (text == null || text.isEmpty()) {
                return this;
            }
            CellAttributes attr = (attributes == null)
                ? defaultAttributes() : attributes;
            addRun(text, attr);
            return this;
        }

        /**
         * Append a hyperlink using the current style plus a single underline
         * and the given URI.
         *
         * @param text the visible link text
         * @param uri the OSC 8 hyperlink URI
         * @return this builder
         */
        public Builder link(final String text, final String uri) {
            CellAttributes attr = new CellAttributes(styles.peek());
            attr.setUnderlineStyle(CellAttributes.UNDERLINE_STYLE_SINGLE);
            attr.setHyperlink(uri);
            return append(text, attr);
        }

        /**
         * Append a hyperlink with explicit attributes and the given URI.
         *
         * @param text the visible link text
         * @param uri the OSC 8 hyperlink URI
         * @param attributes the base attributes (hyperlink is added on top)
         * @return this builder
         */
        public Builder link(final String text, final String uri,
            final CellAttributes attributes) {

            CellAttributes attr = (attributes == null)
                ? defaultAttributes() : new CellAttributes(attributes);
            attr.setHyperlink(uri);
            return append(text, attr);
        }

        /**
         * Push a temporary style scope.  Text appended with {@link
         * #append(String)} until the matching {@link #pop()} uses these
         * attributes.
         *
         * @param attributes the attributes for the new scope
         * @return this builder
         */
        public Builder push(final CellAttributes attributes) {
            styles.push((attributes == null)
                ? defaultAttributes() : new CellAttributes(attributes));
            return this;
        }

        /**
         * Pop the most recently pushed style scope.  The default (bottom)
         * scope is never popped.
         *
         * @return this builder
         */
        public Builder pop() {
            if (styles.size() > 1) {
                styles.pop();
            }
            return this;
        }

        /**
         * Build the {@link RichText}.
         *
         * @return a new, effectively immutable RichText
         */
        public RichText build() {
            return new RichText(runs);
        }

        /**
         * Add a run, merging with the previous run when the attributes are
         * equivalent.
         *
         * @param text the run text
         * @param attributes the run attributes
         */
        private void addRun(final String text,
            final CellAttributes attributes) {

            if (!runs.isEmpty()) {
                Run last = runs.get(runs.size() - 1);
                if (last.attributes().equals(attributes)) {
                    runs.set(runs.size() - 1,
                        new Run(last.getText() + text, attributes));
                    return;
                }
            }
            runs.add(new Run(text, attributes));
        }
    }

    // ------------------------------------------------------------------------
    // Helpers ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Create a default CellAttributes with both foreground and background
     * marked as the terminal/widget default color.
     *
     * @return a fresh default CellAttributes
     */
    static CellAttributes defaultAttributes() {
        CellAttributes attr = new CellAttributes();
        attr.setDefaultColor(true, true);
        attr.setDefaultColor(false, true);
        return attr;
    }
}
