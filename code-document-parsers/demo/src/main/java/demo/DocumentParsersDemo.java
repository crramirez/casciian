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

package demo;

import casciian.TApplication;

import java.nio.file.Path;

/**
 * Main entry point for the Casciian Document Parsers add-on demo.
 *
 * <p>A minimal Markdown viewer TUI: documents are converted to
 * {@link casciian.bits.RichText} by the {@link java.util.ServiceLoader}
 * discovered {@link casciian.document.parsers.DocumentParser}s and shown in a
 * {@link casciian.TTextAnsiWindow}.</p>
 *
 * <p>Run via the fat JAR produced by the {@code jarDemo} task:</p>
 * <pre>
 *     java -jar build/libs/casciian-document-parsers-demo-&lt;version&gt;.jar [document.md]
 * </pre>
 */
public final class DocumentParsersDemo {

    private DocumentParsersDemo() {}

    /**
     * Launch the demo application.
     *
     * @param args optional path of a document to open; when omitted, the
     * bundled sample document is shown
     */
    public static void main(final String[] args) {
        try {
            Path document = args.length > 0 ? Path.of(args[0]) : null;
            DocumentParsersDemoApplication app =
                    new DocumentParsersDemoApplication(
                            TApplication.BackendType.XTERM, document);
            new Thread(app).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
