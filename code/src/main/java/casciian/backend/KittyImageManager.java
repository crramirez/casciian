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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import casciian.bits.Cell;

/**
 * Tracks the kitty graphics images and placements that are live on a
 * terminal, so that each image cell is transmitted at most once and later
 * redraws only send a short placement command.
 *
 * <p>Every image cell becomes one kitty image placed over exactly one text
 * cell (the terminal does the scaling).  A cell's {@link Cell#getImageId()}
 * is used directly as its kitty image id: cells with equal ids hold equal
 * pixels, so an image that moves, is uncovered, or is repeated is re-placed
 * from the terminal's copy without resending pixel data.  Cells without an
 * id, and inverted images, get a single-use id from the upper half of the
 * 32-bit id space (which {@link Cell} ids, being positive {@code int}s,
 * never use); those images are freed as soon as they leave the screen.</p>
 *
 * <p>Transmitted images are remembered in least-recently-used order.  When
 * their total size exceeds the storage budget, images that are not on
 * screen are deleted from the terminal, keeping this cache in step with the
 * terminal's own memory.</p>
 *
 * <p>This class is not thread safe.</p>
 */
final class KittyImageManager {

    /**
     * Default terminal-side storage budget, in bytes.  kitty's own quota is
     * 320 MB; staying well below it means the terminal never evicts an
     * image this manager still believes is available.
     */
    static final long DEFAULT_STORAGE_BUDGET = 128L * 1024 * 1024;

    /**
     * First single-use image id (unsigned 0x80000000).
     */
    private static final int FIRST_EPHEMERAL_ID = 0x80000000;

    /**
     * The encoder producing escape sequences.
     */
    private final KittyImageEncoder encoder;

    /**
     * Maximum bytes of image data to keep in the terminal.
     */
    private final long storageBudget;

    /**
     * Transmitted image ids mapped to their storage cost, in access order.
     */
    private final LinkedHashMap<Integer, Long> transmitted =
        new LinkedHashMap<>(256, 0.75f, true);

    /**
     * Number of on-screen placements of each transmitted image.
     */
    private final Map<Integer, Integer> placementCounts = new HashMap<>();

    /**
     * Sum of the storage cost of all transmitted images.
     */
    private long storageUsed = 0;

    /**
     * The image id placed on each text cell, or 0.  Indexed [x][y].
     */
    private int[][] placed = new int[0][0];

    /**
     * Screen width, used to derive placement ids.
     */
    private int width = 0;

    /**
     * Screen height.
     */
    private int height = 0;

    /**
     * Next single-use image id.
     */
    private int nextEphemeralId = FIRST_EPHEMERAL_ID;

    /**
     * Public constructor.
     *
     * @param encoder the encoder to use
     * @param storageBudget maximum bytes of image data to keep in the
     *                      terminal
     */
    KittyImageManager(final KittyImageEncoder encoder,
        final long storageBudget) {

        this.encoder = encoder;
        this.storageBudget = storageBudget;
    }

    /**
     * Get the encoder.
     *
     * @return the encoder
     */
    KittyImageEncoder getEncoder() {
        return encoder;
    }

    /**
     * Get the image id currently placed on a text cell.
     *
     * @param x column
     * @param y row
     * @return the kitty image id, or 0 if no image is placed there
     */
    int getPlacedImageId(final int x, final int y) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            return 0;
        }
        return placed[x][y];
    }

    /**
     * Check whether the placement grid matches the screen size.
     *
     * @param screenWidth screen width
     * @param screenHeight screen height
     * @return true if the grid has these dimensions
     */
    boolean isSized(final int screenWidth, final int screenHeight) {
        return (width == screenWidth) && (height == screenHeight);
    }

    /**
     * Check whether an image id has been transmitted and is still cached.
     *
     * @param imageId the kitty image id
     * @return true if the terminal holds this image
     */
    boolean isTransmitted(final int imageId) {
        return transmitted.containsKey(imageId);
    }

    /**
     * Get the total storage cost of images held by the terminal.
     *
     * @return bytes
     */
    long getStorageUsed() {
        return storageUsed;
    }

    /**
     * Forget all placements and free every transmitted image.  Use this
     * when the screen has been cleared or resized, or when the protocol is
     * being turned off.
     *
     * @param newWidth screen width
     * @param newHeight screen height
     * @return the escape sequences to emit
     */
    String reset(final int newWidth, final int newHeight) {
        StringBuilder sb = new StringBuilder();
        for (Integer imageId : transmitted.keySet()) {
            sb.append(KittyImageEncoder.deleteImage(imageId));
        }
        transmitted.clear();
        placementCounts.clear();
        storageUsed = 0;
        width = Math.max(0, newWidth);
        height = Math.max(0, newHeight);
        placed = new int[width][height];
        return sb.toString();
    }

    /**
     * Remove whatever image is placed on a text cell.
     *
     * @param x column
     * @param y row
     * @return the escape sequences to emit, or "" if nothing was placed
     */
    String remove(final int x, final int y) {
        int imageId = getPlacedImageId(x, y);
        if (imageId == 0) {
            return "";
        }
        placed[x][y] = 0;
        int count = placementCounts.merge(imageId, -1, Integer::sum);
        if (count <= 0) {
            placementCounts.remove(imageId);
        }
        if (isEphemeral(imageId)) {
            Long cost = transmitted.remove(imageId);
            if (cost != null) {
                storageUsed -= cost;
            }
            return KittyImageEncoder.deleteImage(imageId);
        }
        return KittyImageEncoder.deletePlacement(imageId,
            placementId(x, y));
    }

    /**
     * Ensure an image cell is shown on a text cell.
     *
     * @param x column
     * @param y row
     * @param cell the image cell
     * @param gotoXY the escape sequence that moves the cursor to (x, y)
     * @return the escape sequences to emit, or "" if the cell is already
     * shown
     */
    String show(final int x, final int y, final Cell cell,
        final String gotoXY) {
        return show(x, y, cell, gotoXY, false);
    }

    /**
     * Ensure an image cell is shown on a text cell.
     *
     * @param x column
     * @param y row
     * @param cell the image cell
     * @param gotoXY the escape sequence that moves the cursor to (x, y)
     * @param force if true, re-place an already placed image
     * @return the escape sequences to emit, or "" if the cell is already
     * shown
     */
    String show(final int x, final int y, final Cell cell,
        final String gotoXY, final boolean force) {

        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            return "";
        }
        assert (cell.isImage());

        int imageId = persistentImageId(cell);
        if ((imageId != 0) && (placed[x][y] == imageId)
            && transmitted.containsKey(imageId) && !force
        ) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean alreadyPlaced = (imageId != 0) && (placed[x][y] == imageId);
        if (!alreadyPlaced) {
            sb.append(remove(x, y));
        }
        if (imageId == 0) {
            imageId = nextEphemeralId();
        }
        sb.append(gotoXY);
        int placementId = placementId(x, y);
        if (transmitted.get(imageId) != null) {
            sb.append(KittyImageEncoder.place(imageId, placementId, 1, 1));
        } else {
            sb.append(encoder.transmitAndPlace(cell.getImage(), imageId,
                placementId, 1, 1));
            long cost = KittyImageEncoder.storageBytes(cell.getImage());
            transmitted.put(imageId, cost);
            storageUsed += cost;
        }
        placed[x][y] = imageId;
        if (!alreadyPlaced) {
            placementCounts.merge(imageId, 1, Integer::sum);
        }
        sb.append(evict());
        return sb.toString();
    }

    /**
     * Get the kitty image id that is reused for every appearance of a
     * cell's image.
     *
     * @param cell the image cell
     * @return the cell's image id, or 0 if the image must use a single-use
     * id
     */
    static int persistentImageId(final Cell cell) {
        if (cell.isInvertedImage()) {
            return 0;
        }
        return Math.max(0, cell.getImageId());
    }

    /**
     * Check whether an id is a single-use id.
     *
     * @param imageId the kitty image id
     * @return true if the image is freed once it leaves the screen
     */
    static boolean isEphemeral(final int imageId) {
        return imageId < 0;
    }

    /**
     * Derive the placement id for a text cell.
     *
     * @param x column
     * @param y row
     * @return a non-zero placement id unique to the cell
     */
    private int placementId(final int x, final int y) {
        return (y * width) + x + 1;
    }

    /**
     * Allocate a single-use id.
     *
     * @return an id with the high bit set
     */
    private int nextEphemeralId() {
        int imageId;
        do {
            imageId = nextEphemeralId;
            nextEphemeralId++;
            if (nextEphemeralId == 0) {
                nextEphemeralId = FIRST_EPHEMERAL_ID;
            }
        } while (transmitted.containsKey(imageId));
        return imageId;
    }

    /**
     * Free least-recently-used images that are not on screen until the
     * storage budget is met.
     *
     * @return the escape sequences to emit
     */
    private String evict() {
        if (storageUsed <= storageBudget) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<Integer, Long>> it =
            transmitted.entrySet().iterator();
        while ((storageUsed > storageBudget) && it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            if (placementCounts.containsKey(entry.getKey())) {
                continue;
            }
            storageUsed -= entry.getValue();
            sb.append(KittyImageEncoder.deleteImage(entry.getKey()));
            it.remove();
        }
        return sb.toString();
    }
}
