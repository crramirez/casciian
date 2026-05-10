/*
 * Casciian Java Desktop add-on
 *
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package casciian.javadesktop.image;

import casciian.bits.ArrayImageRGB;
import casciian.bits.ImageRGB;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Black-box tests for {@link BufferedImageRGB}. Exercises the API contract
 * inherited from {@link ImageRGB} without inspecting implementation details.
 */
class BufferedImageRGBTest {

    private static final int RED   = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE  = 0xFF0000FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    @Test
    void constructorRejectsNonPositiveDimensions() {
        assertThatThrownBy(() -> new BufferedImageRGB(0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BufferedImageRGB(10, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsNullBufferedImage() {
        assertThatThrownBy(() -> new BufferedImageRGB((BufferedImage) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsNullImageRGB() {
        assertThatThrownBy(() -> new BufferedImageRGB((ImageRGB) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void newImageHasGivenDimensions() {
        BufferedImageRGB image = new BufferedImageRGB(7, 3);

        assertThat(image.getWidth()).isEqualTo(7);
        assertThat(image.getHeight()).isEqualTo(3);
        assertThat(image.getBufferedImage()).isNotNull();
        assertThat(image.getBufferedImage().getWidth()).isEqualTo(7);
        assertThat(image.getBufferedImage().getHeight()).isEqualTo(3);
    }

    @Test
    void wrapsExistingBufferedImageWithoutCopying() {
        BufferedImage backing = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        backing.setRGB(0, 0, RED);
        BufferedImageRGB image = new BufferedImageRGB(backing);

        assertThat(image.getBufferedImage()).isSameAs(backing);
        assertThat(image.getRGB(0, 0)).isEqualTo(RED);

        // Mutations on the wrapper are visible on the backing image.
        image.setRGB(1, 1, BLUE);
        assertThat(backing.getRGB(1, 1)).isEqualTo(BLUE);
    }

    @Test
    void getAndSetSinglePixelRoundTrip() {
        BufferedImageRGB image = new BufferedImageRGB(2, 2);
        image.setRGB(0, 0, RED);
        image.setRGB(1, 0, GREEN);
        image.setRGB(0, 1, BLUE);
        image.setRGB(1, 1, WHITE);

        assertThat(image.getRGB(0, 0)).isEqualTo(RED);
        assertThat(image.getRGB(1, 0)).isEqualTo(GREEN);
        assertThat(image.getRGB(0, 1)).isEqualTo(BLUE);
        assertThat(image.getRGB(1, 1)).isEqualTo(WHITE);
    }

    @Test
    void bulkGetAndSetRGBRoundTrip() {
        int[] pixels = {RED, GREEN, BLUE, WHITE};
        BufferedImageRGB image = new BufferedImageRGB(2, 2);
        image.setRGB(0, 0, 2, 2, pixels, 0, 2);

        int[] readBack = image.getRGB(0, 0, 2, 2, null, 0, 2);
        assertThat(readBack).containsExactly(RED, GREEN, BLUE, WHITE);
    }

    @Test
    void bulkGetRGBRespectsOffsetAndScansize() {
        int[] pixels = {RED, GREEN, BLUE, WHITE};
        BufferedImageRGB image = new BufferedImageRGB(2, 2);
        image.setRGB(0, 0, 2, 2, pixels, 0, 2);

        int[] target = new int[10];
        image.getRGB(0, 0, 2, 2, target, 3, 3);

        // First three entries are untouched.
        assertThat(target[0]).isEqualTo(0);
        assertThat(target[1]).isEqualTo(0);
        assertThat(target[2]).isEqualTo(0);
        // Row 0 starts at offset, scansize 3 means next row starts at offset+3.
        assertThat(target[3]).isEqualTo(RED);
        assertThat(target[4]).isEqualTo(GREEN);
        assertThat(target[6]).isEqualTo(BLUE);
        assertThat(target[7]).isEqualTo(WHITE);
    }

    @Test
    void bulkAccessorsRejectInvalidRegions() {
        BufferedImageRGB image = new BufferedImageRGB(4, 4);

        assertThatThrownBy(() -> image.getRGB(-1, 0, 1, 1, null, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> image.getRGB(0, 0, 5, 1, null, 0, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> image.setRGB(0, 0, 0, 1, new int[1], 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> image.setRGB(0, 0, 1, 5, new int[5], 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fillRectPaintsTheGivenColor() {
        BufferedImageRGB image = new BufferedImageRGB(4, 4);
        image.fillRect(1, 1, 2, 2, RED);

        assertThat(image.getRGB(0, 0)).isNotEqualTo(RED);
        assertThat(image.getRGB(1, 1)).isEqualTo(RED);
        assertThat(image.getRGB(2, 1)).isEqualTo(RED);
        assertThat(image.getRGB(1, 2)).isEqualTo(RED);
        assertThat(image.getRGB(2, 2)).isEqualTo(RED);
        // Pixels outside the rectangle are unaffected.
        assertThat(image.getRGB(3, 3)).isNotEqualTo(RED);
    }

    @Test
    void fillRectOverwritesExistingPixels() {
        BufferedImageRGB image = new BufferedImageRGB(2, 2);
        image.setRGB(0, 0, 2, 2, new int[]{RED, GREEN, BLUE, WHITE}, 0, 2);
        image.fillRect(0, 0, 2, 2, BLACK);

        assertThat(image.getRGB(0, 0, 2, 2, null, 0, 2))
                .containsOnly(BLACK);
    }

    @Test
    void fillRectRejectsInvalidRegions() {
        BufferedImageRGB image = new BufferedImageRGB(4, 4);

        assertThatThrownBy(() -> image.fillRect(-1, 0, 1, 1, RED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> image.fillRect(0, 0, 0, 1, RED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> image.fillRect(2, 2, 5, 5, RED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void alphaBlendOverWithZeroAlphaLeavesTargetUnchanged() {
        BufferedImageRGB target = solid(2, 2, RED);
        BufferedImageRGB overlay = solid(2, 2, BLUE);

        target.alphaBlendOver(overlay, 0.0);

        assertThat(target.getRGB(0, 0, 2, 2, null, 0, 2)).containsOnly(RED);
    }

    @Test
    void alphaBlendOverWithFullAlphaReplacesTarget() {
        BufferedImageRGB target = solid(2, 2, RED);
        BufferedImageRGB overlay = solid(2, 2, BLUE);

        target.alphaBlendOver(overlay, 1.0);

        assertThat(target.getRGB(0, 0, 2, 2, null, 0, 2)).containsOnly(BLUE);
    }

    @Test
    void alphaBlendOverWithHalfAlphaProducesMidtone() {
        BufferedImageRGB target = solid(2, 2, RED);
        BufferedImageRGB overlay = solid(2, 2, BLUE);

        target.alphaBlendOver(overlay, 0.5);

        // Each blended pixel should have ~half red and ~half blue, fully opaque.
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                int rgb = target.getRGB(x, y);
                int alpha = (rgb >>> 24) & 0xFF;
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
                assertThat(alpha).isEqualTo(0xFF);
                assertThat(red).isBetween(120, 135);
                assertThat(green).isEqualTo(0);
                assertThat(blue).isBetween(120, 135);
            }
        }
    }

    @Test
    void alphaBlendOverWorksWithForeignImageRGB() {
        BufferedImageRGB target = solid(2, 2, RED);
        // Use a non-BufferedImageRGB implementation to exercise the
        // generic fallback path.
        ImageRGB overlay = new ArrayImageRGB(2, 2);
        overlay.fillRect(0, 0, 2, 2, BLUE);

        target.alphaBlendOver(overlay, 1.0);

        assertThat(target.getRGB(0, 0, 2, 2, null, 0, 2)).containsOnly(BLUE);
    }

    @Test
    void alphaBlendOverRejectsInvalidArguments() {
        BufferedImageRGB target = new BufferedImageRGB(2, 2);
        BufferedImageRGB mismatched = new BufferedImageRGB(3, 3);

        assertThatThrownBy(() -> target.alphaBlendOver(null, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> target.alphaBlendOver(mismatched, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getSubimageReturnsIndependentCopy() {
        BufferedImageRGB image = new BufferedImageRGB(3, 3);
        image.setRGB(0, 0, 3, 3, new int[]{
                RED,   GREEN, BLUE,
                WHITE, BLACK, RED,
                GREEN, BLUE,  WHITE,
        }, 0, 3);

        ImageRGB sub = image.getSubimage(1, 1, 2, 2);

        assertThat(sub.getWidth()).isEqualTo(2);
        assertThat(sub.getHeight()).isEqualTo(2);
        assertThat(sub.getRGB(0, 0)).isEqualTo(BLACK);
        assertThat(sub.getRGB(1, 0)).isEqualTo(RED);
        assertThat(sub.getRGB(0, 1)).isEqualTo(BLUE);
        assertThat(sub.getRGB(1, 1)).isEqualTo(WHITE);

        // Mutating the original must not affect the subimage.
        image.setRGB(1, 1, GREEN);
        assertThat(sub.getRGB(0, 0)).isEqualTo(BLACK);
    }

    @Test
    void getSubimageRejectsInvalidArguments() {
        BufferedImageRGB image = new BufferedImageRGB(4, 4);

        assertThatThrownBy(() -> image.getSubimage(-1, 0, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> image.getSubimage(0, 0, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scaleProducesImageWithTargetDimensions() {
        BufferedImageRGB image = solid(4, 4, RED);

        ImageRGB scaled = image.scale(8, 2);

        assertThat(scaled.getWidth()).isEqualTo(8);
        assertThat(scaled.getHeight()).isEqualTo(2);
        // A solid-colour input should remain solid (pixels in the interior
        // away from any edge filtering effects).
        assertThat(scaled.getRGB(4, 1)).isEqualTo(RED);
    }

    @Test
    void scaleRejectsNonPositiveDimensions() {
        BufferedImageRGB image = new BufferedImageRGB(4, 4);

        assertThatThrownBy(() -> image.scale(0, 4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> image.scale(4, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rotateZeroReturnsEqualCopy() {
        BufferedImageRGB image = new BufferedImageRGB(2, 3);
        image.setRGB(0, 0, RED);
        image.setRGB(1, 2, BLUE);

        ImageRGB rotated = image.rotate(0);

        assertThat(rotated).isNotSameAs(image);
        assertThat(rotated.getWidth()).isEqualTo(2);
        assertThat(rotated.getHeight()).isEqualTo(3);
        assertThat(rotated.getRGB(0, 0)).isEqualTo(RED);
        assertThat(rotated.getRGB(1, 2)).isEqualTo(BLUE);
    }

    @Test
    void rotateNinetyClockwiseSwapsDimensionsAndPositions() {
        // 2x3 image:
        // R . .
        // . . B
        BufferedImageRGB image = new BufferedImageRGB(3, 2);
        image.fillRect(0, 0, 3, 2, BLACK);
        image.setRGB(0, 0, RED);
        image.setRGB(2, 1, BLUE);

        ImageRGB rotated = image.rotate(1);

        // Result must be 2x3 (height, width swapped).
        assertThat(rotated.getWidth()).isEqualTo(2);
        assertThat(rotated.getHeight()).isEqualTo(3);
        // (0,0) of source maps to (height-1, 0) = (1, 0) when rotated 90° CW.
        assertThat(rotated.getRGB(1, 0)).isEqualTo(RED);
        // (2,1) of source maps to (0, 2).
        assertThat(rotated.getRGB(0, 2)).isEqualTo(BLUE);
    }

    @Test
    void rotateOneEightyFlipsBothAxes() {
        BufferedImageRGB image = new BufferedImageRGB(3, 2);
        image.fillRect(0, 0, 3, 2, BLACK);
        image.setRGB(0, 0, RED);
        image.setRGB(2, 1, BLUE);

        ImageRGB rotated = image.rotate(2);

        assertThat(rotated.getWidth()).isEqualTo(3);
        assertThat(rotated.getHeight()).isEqualTo(2);
        assertThat(rotated.getRGB(2, 1)).isEqualTo(RED);
        assertThat(rotated.getRGB(0, 0)).isEqualTo(BLUE);
    }

    @Test
    void rotateTwoSeventyClockwiseEqualsNinetyCounterClockwise() {
        BufferedImageRGB image = new BufferedImageRGB(3, 2);
        image.fillRect(0, 0, 3, 2, BLACK);
        image.setRGB(0, 0, RED);
        image.setRGB(2, 1, BLUE);

        ImageRGB rotated = image.rotate(3);

        assertThat(rotated.getWidth()).isEqualTo(2);
        assertThat(rotated.getHeight()).isEqualTo(3);
        // (0,0) rotated 270° CW lands at (0, width-1) = (0, 2).
        assertThat(rotated.getRGB(0, 2)).isEqualTo(RED);
        // (2,1) lands at (1, 0).
        assertThat(rotated.getRGB(1, 0)).isEqualTo(BLUE);
    }

    @Test
    void rotateNormalisesNegativeAndLargeAngles() {
        BufferedImageRGB image = new BufferedImageRGB(3, 2);
        image.fillRect(0, 0, 3, 2, BLACK);
        image.setRGB(0, 0, RED);

        // -3 turns CW == 1 turn CW
        ImageRGB rotatedNegative = image.rotate(-3);
        // 5 turns CW == 1 turn CW
        ImageRGB rotatedLarge = image.rotate(5);

        assertThat(rotatedNegative.getWidth()).isEqualTo(2);
        assertThat(rotatedNegative.getHeight()).isEqualTo(3);
        assertThat(rotatedLarge.getWidth()).isEqualTo(2);
        assertThat(rotatedLarge.getHeight()).isEqualTo(3);
        assertThat(rotatedNegative.getRGB(1, 0)).isEqualTo(RED);
        assertThat(rotatedLarge.getRGB(1, 0)).isEqualTo(RED);
    }

    @Test
    void resizeCanvasGrowingFillsExtraSpaceWithBackground() {
        BufferedImageRGB image = solid(2, 2, RED);

        ImageRGB resized = image.resizeCanvas(4, 4, BLUE);

        assertThat(resized.getWidth()).isEqualTo(4);
        assertThat(resized.getHeight()).isEqualTo(4);
        // Original 2x2 pixels preserved.
        assertThat(resized.getRGB(0, 0)).isEqualTo(RED);
        assertThat(resized.getRGB(1, 1)).isEqualTo(RED);
        // New pixels filled with the background colour.
        assertThat(resized.getRGB(2, 0)).isEqualTo(BLUE);
        assertThat(resized.getRGB(0, 2)).isEqualTo(BLUE);
        assertThat(resized.getRGB(3, 3)).isEqualTo(BLUE);
    }

    @Test
    void resizeCanvasShrinkingCropsImage() {
        BufferedImageRGB image = new BufferedImageRGB(3, 3);
        image.setRGB(0, 0, 3, 3, new int[]{
                RED,   GREEN, BLUE,
                WHITE, BLACK, RED,
                GREEN, BLUE,  WHITE,
        }, 0, 3);

        ImageRGB resized = image.resizeCanvas(2, 2, BLUE);

        assertThat(resized.getWidth()).isEqualTo(2);
        assertThat(resized.getHeight()).isEqualTo(2);
        assertThat(resized.getRGB(0, 0)).isEqualTo(RED);
        assertThat(resized.getRGB(1, 0)).isEqualTo(GREEN);
        assertThat(resized.getRGB(0, 1)).isEqualTo(WHITE);
        assertThat(resized.getRGB(1, 1)).isEqualTo(BLACK);
    }

    @Test
    void resizeCanvasRejectsNonPositiveDimensions() {
        BufferedImageRGB image = new BufferedImageRGB(2, 2);

        assertThatThrownBy(() -> image.resizeCanvas(0, 4, BLACK))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> image.resizeCanvas(4, -1, BLACK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copyConstructorProducesIndependentImage() {
        BufferedImageRGB original = solid(2, 2, RED);

        BufferedImageRGB copy = new BufferedImageRGB((ImageRGB) original);
        original.fillRect(0, 0, 2, 2, BLUE);

        assertThat(copy.getRGB(0, 0, 2, 2, null, 0, 2)).containsOnly(RED);
    }

    @Test
    void copyConstructorWorksWithForeignImageRGB() {
        ImageRGB source = new ArrayImageRGB(2, 2);
        source.setRGB(0, 0, 2, 2, new int[]{RED, GREEN, BLUE, WHITE}, 0, 2);

        BufferedImageRGB copy = new BufferedImageRGB(source);

        assertThat(copy.getWidth()).isEqualTo(2);
        assertThat(copy.getHeight()).isEqualTo(2);
        assertThat(copy.getRGB(0, 0, 2, 2, null, 0, 2))
                .containsExactly(RED, GREEN, BLUE, WHITE);
    }

    /**
     * Creates a {@code BufferedImageRGB} of the given dimensions filled with
     * a single solid colour. Helper used to keep tests concise.
     */
    private static BufferedImageRGB solid(int w, int h, int color) {
        BufferedImageRGB image = new BufferedImageRGB(w, h);
        image.fillRect(0, 0, w, h, color);
        return image;
    }
}
