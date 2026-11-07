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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import casciian.bits.ArrayImageRGB;
import casciian.bits.Cell;
import casciian.bits.ImageRGB;

/**
 * Tracks the kitty graphics images and placements that are live on a
 * terminal, so that each horizontal image run is transmitted once and later
 * redraws only send a short placement command.
 *
 * <p>Adjacent cells become one kitty image. A single cell's
 * {@link Cell#getImageId()} is used as its kitty image id when possible;
 * runs use a combined source key, with collisions distinguished by pixels. Cells
 * without an id, and inverted images, use single-use ids.</p>
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
     * 320 MB; staying well below it reduces the risk of terminal eviction.
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
    private final LinkedHashMap<Integer, CachedImage> transmitted =
        new LinkedHashMap<>(256, 0.75f, true);

    private static final class CachedImage {
        private final int sourceId;
        private final int width;
        private final int height;
        private final int columns;
        private final int[] pixels;
        private final List<TileSnapshot> tiles;
        private final long cost;

        CachedImage(final int sourceId, final ImageRGB image,
            final long cost, final int columns, final List<Cell> cells) {
            this.sourceId = sourceId;
            this.columns = columns;
            width = image.getWidth();
            height = image.getHeight();
            pixels = image.getRGB(0, 0, width, height, null, 0, width);
            tiles = cells.stream().map(TileSnapshot::new).toList();
            this.cost = cost;
        }

        boolean matchesTiles(final List<Cell> cells) {
            if (columns != cells.size() || tiles.size() != cells.size()) {
                return false;
            }
            for (int i = 0; i < cells.size(); i++) {
                if (!tiles.get(i).matches(cells.get(i))) {
                    return false;
                }
            }
            return true;
        }

        boolean matches(final int id, final ImageRGB image,
            final int columns) {
            if (sourceId != id || this.columns != columns
                || width != image.getWidth()
                || height != image.getHeight()) {
                return false;
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (pixels[y * width + x] != image.getRGB(x, y)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private static final class TileSnapshot {
        private final int imageId;
        private final int width;
        private final int height;
        private final int[] pixels;

        TileSnapshot(final Cell cell) {
            imageId = persistentImageId(cell);
            ImageRGB image = cell.getImage();
            width = image.getWidth();
            height = image.getHeight();
            pixels = image.getRGB(0, 0, width, height, null, 0, width);
        }

        boolean matches(final Cell cell) {
            if (imageId != persistentImageId(cell)) {
                return false;
            }
            ImageRGB image = cell.getImage();
            if (width != image.getWidth() || height != image.getHeight()) {
                return false;
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (pixels[y * width + x] != image.getRGB(x, y)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private final Map<Integer, List<Integer>> sourceImages =
        new HashMap<>();

    private int cellWidth = 0;
    private int cellHeight = 0;

    /**
     * Number of on-screen placements of each transmitted image.
     */
    private final Map<Integer, Integer> placementCounts = new HashMap<>();

    /**
     * Sum of the storage cost of all transmitted images.
     */
    private long storageUsed = 0;

    /**
     * Shared placement covering each text cell, or null. Indexed [x][y].
     */
    private Placement[][] placed = new Placement[0][0];

    private record Placement(int imageId, int x, int y, int columns) {
    }

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
        return placed[x][y] == null ? 0 : placed[x][y].imageId();
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
        sourceImages.clear();
        placementCounts.clear();
        storageUsed = 0;
        width = Math.max(0, newWidth);
        height = Math.max(0, newHeight);
        placed = new Placement[width][height];
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
        Placement placement = placed[x][y];
        for (int column = placement.x();
             column < placement.x() + placement.columns(); column++) {
            placed[column][y] = null;
        }
        int count = placementCounts.merge(imageId, -1, Integer::sum);
        if (count <= 0) {
            placementCounts.remove(imageId);
        }
        CachedImage cached = transmitted.get(imageId);
        if ((cached != null) && (cached.sourceId == 0)) {
            transmitted.remove(imageId);
            storageUsed -= cached.cost;
            return KittyImageEncoder.deleteImage(imageId);
        }
        return KittyImageEncoder.deletePlacement(imageId,
            placementId(placement.x(), placement.y()));
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
        return show(x, y, cell, gotoXY, force, 1);
    }

    /**
     * Show a complete horizontal run, sharing one placement across its cells.
     */
    String show(final int x, final int y, final List<Cell> cells,
        final String gotoXY, final boolean force) {
        if (cells.size() == 1) {
            return show(x, y, cells.getFirst(), gotoXY, force, 1, cells);
        }
        int columns = cells.size();
        if ((x < 0) || (y < 0) || (x + columns > width) || (y >= height)) {
            return "";
        }
        boolean alreadyPlaced = matchesPlacement(x, y, cells);
        int imageId = alreadyPlaced ? getPlacedImageId(x, y) : 0;
        if ((imageId != 0) && transmitted.containsKey(imageId) && !force) {
            return "";
        }
        if (imageId == 0) {
            imageId = findImageId(runSourceId(cells), cells);
        }
        if (imageId == 0) {
            return show(x, y, compose(cells), gotoXY, force, columns, cells);
        }

        StringBuilder sb = new StringBuilder();
        if (!alreadyPlaced) {
            for (int column = x; column < x + columns; column++) {
                sb.append(remove(column, y));
            }
        }
        sb.append(gotoXY);
        int placementId = placementId(x, y);
        sb.append(KittyImageEncoder.place(imageId, placementId, columns, 1));
        Placement placement = new Placement(imageId, x, y, columns);
        for (int column = x; column < x + columns; column++) {
            placed[column][y] = placement;
        }
        if (!alreadyPlaced) {
            placementCounts.merge(imageId, 1, Integer::sum);
        }
        sb.append(evict());
        return sb.toString();
    }

    private String show(final int x, final int y, final Cell cell,
        final String gotoXY, final boolean force, final int columns) {
        return show(x, y, cell, gotoXY, force, columns, List.of(cell));
    }

    private String show(final int x, final int y, final Cell cell,
        final String gotoXY, final boolean force, final int columns,
        final List<Cell> sourceCells) {

        if ((x < 0) || (y < 0) || (x + columns > width) || (y >= height)) {
            return "";
        }
        assert (cell.isImage());

        int sourceId = persistentImageId(cell);
        int imageId = findImageId(cell, columns);
        boolean alreadyPlaced = matchesPlacement(x, y, cell, columns);
        if (alreadyPlaced && sourceId == 0) {
            imageId = getPlacedImageId(x, y);
        }
        if ((imageId != 0) && alreadyPlaced
            && transmitted.containsKey(imageId) && !force
        ) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (!alreadyPlaced) {
            for (int column = x; column < x + columns; column++) {
                sb.append(remove(column, y));
            }
        }
        if (imageId == 0) {
            imageId = (sourceId != 0 && !transmitted.containsKey(sourceId))
                ? sourceId : nextEphemeralId();
        }
        sb.append(gotoXY);
        int placementId = placementId(x, y);
        if (transmitted.get(imageId) != null) {
            sb.append(KittyImageEncoder.place(imageId, placementId, columns, 1));
        } else {
            ImageRGB image = cell.getImage();
            if (cellWidth > 0 && cellHeight > 0
                && (image.getWidth() != cellWidth * columns
                    || image.getHeight() != cellHeight)) {
                image = scale(image, cellWidth * columns, cellHeight);
            }
            sb.append(encoder.transmitAndPlace(image, imageId,
                placementId, columns, 1));
            long cost = KittyImageEncoder.storageBytes(image);
            transmitted.put(imageId, new CachedImage(sourceId,
                cell.getImage(), cost, columns, sourceCells));
            if (sourceId != 0) {
                sourceImages.computeIfAbsent(sourceId,
                    ignored -> new ArrayList<>()).add(imageId);
            }
            storageUsed += cost;
        }
        Placement placement = new Placement(imageId, x, y, columns);
        for (int column = x; column < x + columns; column++) {
            placed[column][y] = placement;
        }
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

    boolean matchesPlacement(final int x, final int y, final Cell cell) {
        return matchesPlacement(x, y, cell, 1);
    }

    boolean matchesPlacement(final int x, final int y,
        final List<Cell> cells) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            return false;
        }
        Placement placement = placed[x][y];
        if ((placement == null) || (placement.x() != x)
            || (placement.columns() != cells.size())) {
            return false;
        }
        CachedImage cached = transmitted.get(placement.imageId());
        return cached != null && cached.matchesTiles(cells);
    }

    private boolean matchesPlacement(final int x, final int y,
        final Cell cell, final int columns) {
        CachedImage cached = transmitted.get(getPlacedImageId(x, y));
        return cached != null
            && placed[x][y].x() == x && placed[x][y].columns() == columns
            && cached.matches(persistentImageId(cell), cell.getImage(), columns);
    }

    private int runSourceId(final List<Cell> cells) {
        int sourceId = 1;
        for (Cell cell : cells) {
            int imageId = persistentImageId(cell);
            if (imageId == 0) {
                return 0;
            }
            ImageRGB tile = cell.getImage();
            sourceId = 31 * sourceId + imageId;
            sourceId = 31 * sourceId + tile.getWidth();
            sourceId = 31 * sourceId + tile.getHeight();
            for (int pixel : tile.getRGB(0, 0, tile.getWidth(),
                    tile.getHeight(), null, 0, tile.getWidth())) {
                sourceId = 31 * sourceId + pixel;
            }
        }
        return (sourceId & 0x7FFFFFFF) | 1;
    }

    private Cell compose(final List<Cell> cells) {
        if (cells.size() == 1) {
            return cells.getFirst();
        }
        int tileWidth = cellWidth > 0 ? cellWidth
            : cells.getFirst().getImage().getWidth();
        int tileHeight = cellHeight > 0 ? cellHeight
            : cells.getFirst().getImage().getHeight();
        ImageRGB image = new ArrayImageRGB(tileWidth * cells.size(), tileHeight);
        int sourceId = 1;
        boolean persistent = true;
        for (int column = 0; column < cells.size(); column++) {
            Cell cell = cells.get(column);
            ImageRGB tile = cell.getImage();
            persistent &= persistentImageId(cell) != 0;
            sourceId = 31 * sourceId + persistentImageId(cell);
            sourceId = 31 * sourceId + tile.getWidth();
            sourceId = 31 * sourceId + tile.getHeight();
            // Include original pixels so changes lost during scaling still
            // invalidate the run. Cached composite pixels verify collisions.
            for (int pixel : tile.getRGB(0, 0, tile.getWidth(),
                    tile.getHeight(), null, 0, tile.getWidth())) {
                sourceId = 31 * sourceId + pixel;
            }
            if (tile.getWidth() != tileWidth || tile.getHeight() != tileHeight) {
                tile = scale(tile, tileWidth, tileHeight);
            }
            image.setRGB(column * tileWidth, 0, tileWidth, tileHeight,
                tile.getRGB(0, 0, tileWidth, tileHeight, null, 0, tileWidth),
                0, tileWidth);
        }
        Cell result = new Cell();
        if (persistent) {
            result.setImage(image, (sourceId & 0x7FFFFFFF) | 1);
        } else {
            result.setImage(image);
        }
        return result;
    }

    private ImageRGB scale(final ImageRGB image, final int newWidth,
        final int newHeight) {
        boolean rgba = encoder.getFormat() == KittyImageEncoder.Format.RGBA;
        ArrayImageRGB source = new ArrayImageRGB(image.getWidth(),
            image.getHeight());
        ArrayImageRGB alpha = rgba
            ? new ArrayImageRGB(image.getWidth(), image.getHeight()) : null;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int a = (pixel >>> 24) & 0xFF;
                int r = (pixel >>> 16) & 0xFF;
                int g = (pixel >>> 8) & 0xFF;
                int b = pixel & 0xFF;
                if (rgba) {
                    r = (r * a + 127) / 255;
                    g = (g * a + 127) / 255;
                    b = (b * a + 127) / 255;
                    alpha.setRGB(x, y, a);
                }
                source.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        ImageRGB scaled = source.scale(newWidth, newHeight);
        if (rgba) {
            ImageRGB scaledAlpha = alpha.scale(newWidth, newHeight);
            for (int y = 0; y < newHeight; y++) {
                for (int x = 0; x < newWidth; x++) {
                    int a = scaledAlpha.getRGB(x, y) & 0xFF;
                    int pixel = scaled.getRGB(x, y);
                    int r = unpremultiply((pixel >>> 16) & 0xFF, a);
                    int g = unpremultiply((pixel >>> 8) & 0xFF, a);
                    int b = unpremultiply(pixel & 0xFF, a);
                    scaled.setRGB(x, y, (a << 24) | (r << 16)
                        | (g << 8) | b);
                }
            }
        }
        return scaled;
    }

    private int unpremultiply(final int color, final int alpha) {
        return alpha == 0 ? 0
            : Math.min(255, (color * 255 + alpha / 2) / alpha);
    }

    String setCellSize(final int newWidth, final int newHeight) {
        if (cellWidth == newWidth && cellHeight == newHeight) {
            return "";
        }
        cellWidth = newWidth;
        cellHeight = newHeight;
        return reset(width, height);
    }

    private int findImageId(final Cell cell, final int columns) {
        int sourceId = persistentImageId(cell);
        if (sourceId == 0) {
            return 0;
        }
        List<Integer> ids = sourceImages.get(sourceId);
        if (ids != null) {
            for (int id : ids) {
                CachedImage image = transmitted.get(id);
                if (image != null
                    && image.matches(sourceId, cell.getImage(), columns)) {
                    return id;
                }
            }
        }
        return 0;
    }

    private int findImageId(final int sourceId, final List<Cell> cells) {
        if (sourceId == 0) {
            return 0;
        }
        List<Integer> ids = sourceImages.get(sourceId);
        if (ids != null) {
            for (int id : ids) {
                CachedImage image = transmitted.get(id);
                if (image != null && image.matchesTiles(cells)) {
                    return id;
                }
            }
        }
        return 0;
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
        Iterator<Map.Entry<Integer, CachedImage>> it =
            transmitted.entrySet().iterator();
        while ((storageUsed > storageBudget) && it.hasNext()) {
            Map.Entry<Integer, CachedImage> entry = it.next();
            if (placementCounts.containsKey(entry.getKey())) {
                continue;
            }
            CachedImage image = entry.getValue();
            storageUsed -= image.cost;
            sb.append(KittyImageEncoder.deleteImage(entry.getKey()));
            List<Integer> ids = sourceImages.get(image.sourceId);
            if (ids != null) {
                ids.remove(entry.getKey());
                if (ids.isEmpty()) {
                    sourceImages.remove(image.sourceId);
                }
            }
            it.remove();
        }
        return sb.toString();
    }
}
