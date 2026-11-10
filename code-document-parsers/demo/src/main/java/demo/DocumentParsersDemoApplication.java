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
import casciian.TExceptionDialog;
import casciian.TFileOpenBox;
import casciian.TTextAnsiWindow;
import casciian.bits.RichText;
import casciian.document.parsers.DocumentParser;
import casciian.document.parsers.DocumentParserRegistry;
import casciian.event.TMenuEvent;
import casciian.menu.TMenu;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Demo TUI application showcasing the Casciian Document Parsers add-on as a
 * simple Markdown viewer.
 *
 * <p>The add-on registers its
 * {@link casciian.documentparsers.markdown.MarkdownDocumentParser} as a
 * {@link java.util.ServiceLoader} provider, which {@link TApplication}'s
 * constructor auto-discovers through
 * {@link DocumentParserRegistry#loadParsers()}. The demo shows the three ways
 * of using a parser:</p>
 * <ul>
 * <li>by file extension: the bundled sample and {@code File > Open
 * document...} use {@link DocumentParserRegistry#parse(Path)}, and relative
 * images are resolved against the document's folder;</li>
 * <li>by MIME type: {@code Help > About this demo} parses a classpath
 * resource through
 * {@link DocumentParserRegistry#parse(InputStream, String)};</li>
 * <li>directly: any {@link DocumentParser} can be invoked with an
 * {@link InputStream}.</li>
 * </ul>
 */
public final class DocumentParsersDemoApplication extends TApplication {

    /**
     * Menu item: open a document.
     */
    private static final int MID_OPEN_DOCUMENT = 3000;

    /**
     * Menu item: reopen the bundled sample document.
     */
    private static final int MID_OPEN_SAMPLE = 3001;

    /**
     * Menu item: about this demo.
     */
    private static final int MID_ABOUT_DEMO = 3002;

    /**
     * Classpath folder holding the bundled sample document.
     */
    private static final String SAMPLE_RESOURCE_DIR = "/demo/";

    /**
     * Files of the bundled sample document.
     */
    private static final List<String> SAMPLE_FILES = List.of("README.md",
            "logo.png", "chart.png", "images/xyz.png");

    /**
     * Folder the sample document is extracted to, created lazily.
     */
    private Path sampleDirectory;

    /**
     * Public constructor.
     *
     * @param backendType the desired backend type
     * @param document the document to open at startup, or null to open the
     * bundled sample
     * @throws UnsupportedEncodingException on backend errors
     */
    public DocumentParsersDemoApplication(final BackendType backendType,
            final Path document) throws UnsupportedEncodingException {

        super(backendType);

        TMenu fileMenu = addMenu("&File");
        fileMenu.addItem(MID_OPEN_DOCUMENT, "&Open document...");
        fileMenu.addItem(MID_OPEN_SAMPLE, "Open &sample");
        fileMenu.addSeparator();
        fileMenu.addDefaultItem(TMenu.MID_EXIT);
        addWindowMenu();
        TMenu helpMenu = addHelpMenu();
        helpMenu.addSeparator();
        helpMenu.addItem(MID_ABOUT_DEMO, "About this &demo");

        getBackend().setTitle("Casciian Document Parsers Add-on Demo");

        if (document != null) {
            openDocument(document);
        } else {
            openSample();
        }
    }

    @Override
    public boolean onMenu(final TMenuEvent menu) {
        switch (menu.getId()) {
        case MID_OPEN_DOCUMENT:
            chooseDocument();
            return true;
        case MID_OPEN_SAMPLE:
            openSample();
            return true;
        case MID_ABOUT_DEMO:
            showAbout();
            return true;
        default:
            return super.onMenu(menu);
        }
    }

    /**
     * Ask the user for a document, offering every registered parser's
     * extension filter.
     */
    private void chooseDocument() {
        try {
            List<String> filters = DocumentParserRegistry.getInstance()
                    .getFileExtensionPatterns();
            String filename = fileOpenBox(".", TFileOpenBox.Type.OPEN,
                    filters);
            if (filename != null) {
                openDocument(Path.of(filename));
            }
        } catch (IOException e) {
            new TExceptionDialog(this, e);
        }
    }

    /**
     * Extract the bundled sample (Markdown and images) to a temporary folder
     * and open it, so its relative image links resolve like on-disk files.
     */
    private void openSample() {
        try {
            if (sampleDirectory == null) {
                sampleDirectory = extractSample();
            }
            openDocument(sampleDirectory.resolve(SAMPLE_FILES.get(0)));
        } catch (IOException e) {
            new TExceptionDialog(this, e);
        }
    }

    /**
     * Parse a document chosen by its file extension and display it.
     *
     * @param path the document
     */
    private void openDocument(final Path path) {
        try {
            RichText text = DocumentParserRegistry.getInstance().parse(path);
            Path fileName = path.getFileName();
            showRichText(fileName == null ? path.toString()
                    : fileName.toString(), text);
        } catch (IOException | IllegalArgumentException e) {
            messageBox("Open document", "Unable to open " + path + ": "
                    + e.getMessage());
        }
    }

    /**
     * Build the about document from a classpath resource plus the list of
     * registered parsers, and parse it by MIME type from a stream.
     */
    private void showAbout() {
        try (InputStream in = getClass().getResourceAsStream(
                SAMPLE_RESOURCE_DIR + "about.md")) {
            if (in == null) {
                throw new IOException("Missing resource about.md");
            }
            StringBuilder markdown = new StringBuilder(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8));
            for (DocumentParser parser : DocumentParserRegistry.getInstance()
                    .getParsers()) {
                markdown.append("- **").append(parser.getFormatDescription())
                        .append("**: `")
                        .append(String.join("`, `",
                                parser.getSupportedMimeTypes()))
                        .append("`\n");
            }
            RichText text = DocumentParserRegistry.getInstance().parse(
                    new ByteArrayInputStream(markdown.toString()
                            .getBytes(StandardCharsets.UTF_8)),
                    "text/markdown; charset=UTF-8");
            showRichText("About this demo", text);
        } catch (IOException | IllegalArgumentException e) {
            new TExceptionDialog(this, new IOException(e));
        }
    }

    /**
     * Display rich text in a new window sized to the desktop.
     *
     * @param title the window title
     * @param text the text to display
     */
    private void showRichText(final String title, final RichText text) {
        int width = Math.max(30, Math.min(84, getScreen().getWidth() - 2));
        int height = Math.max(10, getDesktopBottom() - getDesktopTop() - 2);
        TTextAnsiWindow window = new TTextAnsiWindow(this, title, "", 1, 1,
                width, height);
        window.getTextAnsiField().setRichText(text);
    }

    /**
     * Copy the bundled sample files to a new temporary folder.
     *
     * @return the folder
     * @throws IOException if a resource is missing or cannot be copied
     */
    private Path extractSample() throws IOException {
        Path dir = Files.createTempDirectory("casciian-document-parsers-demo");
        dir.toFile().deleteOnExit();
        for (String name : SAMPLE_FILES) {
            try (InputStream in = getClass().getResourceAsStream(
                    SAMPLE_RESOURCE_DIR + name)) {
                if (in == null) {
                    throw new IOException("Missing resource " + name);
                }
                Path target = dir.resolve(name);
                Files.createDirectories(target.getParent());
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                target.toFile().deleteOnExit();
            }
        }
        return dir;
    }
}
