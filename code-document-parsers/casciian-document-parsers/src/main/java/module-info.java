/*
 * Casciian Document Parsers add-on
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

/**
 * Casciian Document Parsers add-on.
 *
 * <p>Optional add-on that provides
 * {@link casciian.document.parsers.DocumentParser} implementations converting
 * documents into the Casciian {@link casciian.bits.RichText} model. It
 * currently provides:</p>
 *
 * <ul>
 *   <li>{@link casciian.documentparsers.markdown.MarkdownDocumentParser} —
 *       CommonMark Markdown (plus GFM tables, strikethrough and task lists),
 *       including images.</li>
 * </ul>
 */
module casciian.document.parsers {
    requires transitive casciian;
    requires org.commonmark;
    requires org.commonmark.ext.gfm.tables;
    requires org.commonmark.ext.gfm.strikethrough;
    requires org.commonmark.ext.task.list.items;

    exports casciian.documentparsers.markdown;

    provides casciian.document.parsers.DocumentParser
        with casciian.documentparsers.markdown.MarkdownDocumentParser;
}
