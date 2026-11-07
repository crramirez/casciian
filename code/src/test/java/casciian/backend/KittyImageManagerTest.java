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
package casciian.backend;

import java.util.List;
import java.util.Map;

import casciian.bits.ArrayImageRGB;
import casciian.bits.Cell;
import casciian.bits.ImageRGB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static casciian.backend.KittyImageEncoderTest.parse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the kitty image manager reuses Cell image ids as kitty image
 * ids, transmits each image once, and cleans up placements and data.
 */
@DisplayName("KittyImageManager")
class KittyImageManagerTest {

    private static final String GOTO = "<goto>";

    private KittyImageManager manager;

    @BeforeEach
    void setUp() {
        manager = new KittyImageManager(new KittyImageEncoder(
            KittyImageEncoder.Format.RGB, true),
            KittyImageManager.DEFAULT_STORAGE_BUDGET);
        manager.reset(10, 5);
    }

    @Test
    @DisplayName("A cell's image id is the kitty image id, transmitted once")
    void imageIdIsReused() {
        Cell cell = imageCell(1234);

        List<KittyImageEncoderTest.Command> first =
            parse(manager.show(0, 0, cell, GOTO));
        assertThat(first).hasSize(1);
        assertThat(first.getFirst().keys()).containsEntry("a", "T")
            .containsEntry("i", "1234").containsEntry("p", "1");

        // Same image elsewhere: placed from the terminal's copy.
        List<KittyImageEncoderTest.Command> second =
            parse(manager.show(3, 2, cell, GOTO));
        assertThat(second).hasSize(1);
        assertThat(second.getFirst().keys()).containsEntry("a", "p")
            .containsEntry("i", "1234").containsEntry("p", "24");
        assertThat(second.getFirst().payload()).isEmpty();

        // Unchanged cell: nothing to send.
        assertThat(manager.show(0, 0, cell, GOTO)).isEmpty();
    }

    @Test
    @DisplayName("Colliding source ids retain and reuse distinct pixel data")
    void collidingIdsDoNotSharePixels() {
        Cell first = imageCell(42);
        Cell second = imageCell(42);
        second.getImage().setRGB(0, 0, 0xFF123456);

        String firstId = parse(manager.show(0, 0, first, GOTO))
            .getFirst().keys().get("i");
        Map<String, String> secondUpload = parse(manager.show(1, 0, second, GOTO))
            .getFirst().keys();
        assertThat(secondUpload).containsEntry("a", "T");
        String secondId = secondUpload.get("i");
        assertThat(secondId).isNotEqualTo(firstId);
        assertThat(parse(manager.show(2, 0, second, GOTO)).getFirst().keys())
            .containsEntry("a", "p").containsEntry("i", secondId);
        assertThat(parse(manager.show(0, 0, second, GOTO)))
            .extracting(c -> c.keys().get("a")).containsExactly("d", "p");
        assertThat(parse(manager.show(3, 0, first, GOTO)).getFirst().keys())
            .containsEntry("a", "p").containsEntry("i", firstId);
    }

    @Test
    @DisplayName("Tiles are scaled to cell dimensions and re-uploaded on change")
    void cellSizeChangesInvalidateTiles() {
        manager.setCellSize(12, 16);
        Cell cell = imageCell(42);
        assertThat(parse(manager.show(0, 0, cell, GOTO)).getFirst().keys())
            .containsEntry("s", "12").containsEntry("v", "16");
        assertThat(parse(manager.setCellSize(8, 16)).getFirst().keys())
            .containsEntry("d", "I").containsEntry("i", "42");
        assertThat(parse(manager.show(0, 0, cell, GOTO)).getFirst().keys())
            .containsEntry("a", "T").containsEntry("s", "8")
            .containsEntry("v", "16");
        assertThat(manager.setCellSize(8, 16)).isEmpty();
    }

    @Test
    @DisplayName("The cursor move precedes the placement")
    void gotoPrecedesPlacement() {
        String output = manager.show(0, 0, imageCell(5), GOTO);

        assertThat(output).startsWith(GOTO + "\033_G");
    }

    @Test
    @DisplayName("Replacing an image deletes only the old placement")
    void replacingDeletesOldPlacement() {
        manager.show(1, 1, imageCell(10), GOTO);

        List<KittyImageEncoderTest.Command> commands =
            parse(manager.show(1, 1, imageCell(11), GOTO));

        assertThat(commands).hasSize(2);
        assertThat(commands.get(0).keys()).containsEntry("a", "d")
            .containsEntry("d", "i").containsEntry("i", "10")
            .containsEntry("p", "12");
        assertThat(commands.get(1).keys()).containsEntry("a", "T")
            .containsEntry("i", "11");
        assertThat(manager.isTransmitted(10)).isTrue();
        assertThat(manager.getPlacedImageId(1, 1)).isEqualTo(11);
    }

    @Test
    @DisplayName("Removing an image placement keeps its data for reuse")
    void removeKeepsData() {
        manager.show(2, 0, imageCell(42), GOTO);

        assertThat(parse(manager.remove(2, 0)).getFirst().keys())
            .containsEntry("d", "i");
        assertThat(manager.remove(2, 0)).isEmpty();
        assertThat(parse(manager.show(4, 4, imageCell(42), GOTO)).getFirst()
            .keys()).containsEntry("a", "p");
    }

    @Test
    @DisplayName("Cells without an image id use single-use ids that are freed")
    void cellsWithoutIdAreEphemeral() {
        Cell cell = imageCell(0);

        Map<String, String> first =
            parse(manager.show(0, 0, cell, GOTO)).getFirst().keys();
        long id = Long.parseLong(first.get("i"));
        assertThat(id).isGreaterThanOrEqualTo(0x80000000L);

        Map<String, String> removed =
            parse(manager.remove(0, 0)).getFirst().keys();
        assertThat(removed).containsEntry("d", "I")
            .containsEntry("i", first.get("i"));
        assertThat(manager.getStorageUsed()).isZero();
    }

    @Test
    @DisplayName("Inverted images never reuse the normal image's id")
    void invertedImagesAreEphemeral() {
        Cell cell = imageCell(77);
        cell.invertImage();

        Map<String, String> keys =
            parse(manager.show(0, 0, cell, GOTO)).getFirst().keys();

        assertThat(keys).containsEntry("a", "T");
        assertThat(keys.get("i")).isNotEqualTo("77");
    }

    @Test
    @DisplayName("Reset frees every transmitted image and forgets placements")
    void resetFreesEverything() {
        manager.show(0, 0, imageCell(1), GOTO);
        manager.show(1, 0, imageCell(2), GOTO);

        List<KittyImageEncoderTest.Command> commands =
            parse(manager.reset(10, 5));

        assertThat(commands).extracting(c -> c.keys().get("i"))
            .containsExactlyInAnyOrder("1", "2");
        assertThat(commands).allSatisfy(c ->
            assertThat(c.keys()).containsEntry("d", "I"));
        assertThat(manager.getPlacedImageId(0, 0)).isZero();
        assertThat(manager.getStorageUsed()).isZero();
        // After a reset the image must be transmitted again.
        assertThat(parse(manager.show(0, 0, imageCell(1), GOTO)).getFirst()
            .keys()).containsEntry("a", "T");
    }

    @Test
    @DisplayName("Over budget, off-screen least-recently-used images are freed")
    void evictsOffScreenImages() {
        // Each 4x4 image costs 64 bytes; allow two of them.
        manager = new KittyImageManager(new KittyImageEncoder(
            KittyImageEncoder.Format.RGB, false), 128);
        manager.reset(10, 5);

        manager.show(0, 0, imageCell(1), GOTO);
        manager.show(1, 0, imageCell(2), GOTO);
        manager.remove(0, 0);

        String output = manager.show(2, 0, imageCell(3), GOTO);

        List<KittyImageEncoderTest.Command> commands = parse(output);
        assertThat(commands.getLast().keys()).containsEntry("d", "I")
            .containsEntry("i", "1");
        assertThat(manager.isTransmitted(1)).isFalse();
        // Image 2 is still on screen and must survive.
        assertThat(manager.isTransmitted(2)).isTrue();
        assertThat(manager.isTransmitted(3)).isTrue();
    }

    private static Cell imageCell(final int imageId) {
        ImageRGB image = new ArrayImageRGB(4, 4);
        image.fillRect(0, 0, 4, 4, 0xFF000000 | imageId);
        Cell cell = new Cell();
        if (imageId > 0) {
            cell.setImage(image, imageId);
        } else {
            cell.setImage(image);
        }
        return cell;
    }
}
