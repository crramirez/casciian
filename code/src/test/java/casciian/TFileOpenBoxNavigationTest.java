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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import casciian.backend.HeadlessBackend;
import casciian.event.TKeypressEvent;

import static casciian.TKeypress.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TFileOpenBoxNavigationTest {

    @TempDir
    Path directory;

    static Stream<TKeypress> navigationKeys() {
        return Stream.of(kbUp, kbDown, kbLeft, kbRight, kbPgUp, kbPgDn,
            kbHome, kbEnd);
    }

    static Stream<Arguments> populatedNavigation() {
        return Stream.of(
            Arguments.of(kbUp, 0),
            Arguments.of(kbDown, 2),
            Arguments.of(kbLeft, 1),
            Arguments.of(kbRight, 1),
            Arguments.of(kbPgUp, 0),
            Arguments.of(kbPgDn, 2),
            Arguments.of(kbHome, 0),
            Arguments.of(kbEnd, 2));
    }

    @ParameterizedTest
    @MethodSource("navigationKeys")
    void emptyDirectoryNavigationIsSafe(final TKeypress key)
        throws IOException {

        TFileOpenBox dialog = openDialog(null);

        assertEmptyNavigation(dialog, key);
    }

    @ParameterizedTest
    @MethodSource("navigationKeys")
    void navigationIsSafeAfterChangingToEmptyDirectory(final TKeypress key)
        throws IOException {

        Files.createFile(directory.resolve("file.txt"));
        Path empty = Files.createDirectory(directory.resolve("empty"));
        TFileOpenBox dialog = openDialog(null);
        TDirectoryList list = fileList(dialog);
        assertThat(list.getSelected()).isNotNull();

        list.setPath(empty.toString());

        assertEmptyNavigation(dialog, key);
    }

    @ParameterizedTest
    @MethodSource("navigationKeys")
    void navigationIsSafeWhenFiltersExcludeAllFiles(final TKeypress key)
        throws IOException {

        Files.createFile(directory.resolve("file.txt"));
        TFileOpenBox dialog = openDialog(List.of(".*\\.java"));

        assertEmptyNavigation(dialog, key);
    }

    @ParameterizedTest
    @MethodSource("populatedNavigation")
    void populatedNavigationStillSelectsAndOpensFiles(final TKeypress key,
        final int expectedIndex) throws IOException {

        for (String name: List.of("a.txt", "b.txt", "c.txt")) {
            Files.createFile(directory.resolve(name));
        }
        TFileOpenBox dialog = openDialog(null);
        TDirectoryList list = fileList(dialog);
        list.setSelectedIndex(1);

        press(dialog, key);

        assertThat(list.getSelectedIndex()).isEqualTo(expectedIndex);
        Path expected = directory.resolve(
            List.of("a.txt", "b.txt", "c.txt").get(expectedIndex));
        assertThat(list.getPath()).isEqualTo(expected.toFile());

        press(dialog, kbEnter);

        assertThat(dialog.getFilename()).isEqualTo(expected.toString());
        assertThat(dialog.getApplication().getAllWindows())
            .doesNotContain(dialog);
    }

    private TFileOpenBox openDialog(final List<String> filters)
        throws IOException {

        return new TFileOpenBox(new TApplication(new HeadlessBackend()),
            directory.toString(), TFileOpenBox.Type.OPEN, filters);
    }

    private TDirectoryList fileList(final TFileOpenBox dialog) {
        return dialog.getChildren().stream()
            .filter(TDirectoryList.class::isInstance)
            .map(TDirectoryList.class::cast)
            .findFirst().orElseThrow();
    }

    private void assertEmptyNavigation(final TFileOpenBox dialog,
        final TKeypress key) {

        TDirectoryList list = fileList(dialog);
        assertThat(list.isActive()).isTrue();
        assertThat(list.getList()).isEmpty();

        assertThatCode(() -> press(dialog, key)).doesNotThrowAnyException();
        assertThat(list.getSelectedIndex()).isEqualTo(-1);
        assertThat(list.getSelected()).isNull();
        assertThat(list.isActive()).isTrue();
        assertThatCode(() -> press(dialog, kbEnter)).doesNotThrowAnyException();
        assertThat(dialog.getFilename()).isNull();
        assertThat(dialog.getApplication().getAllWindows()).contains(dialog);

        press(dialog, kbTab);
        assertThat(list.isActive()).isFalse();
        press(dialog, kbEsc);
        assertThat(dialog.getApplication().getAllWindows())
            .doesNotContain(dialog);
    }

    private void press(final TFileOpenBox dialog, final TKeypress key) {
        dialog.handleEvent(new TKeypressEvent(null, key));
    }
}
