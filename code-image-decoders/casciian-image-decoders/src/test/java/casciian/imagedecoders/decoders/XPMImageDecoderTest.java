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
package casciian.imagedecoders.decoders;

import casciian.bits.ImageRGB;
import casciian.image.decoders.ImageDecoder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Black-box tests for {@link XPMImageDecoder}. Exercises the public
 * {@link ImageDecoder} API against synthetic XPM sources, covering hex and
 * named colors, without inspecting implementation details.
 */
class XPMImageDecoderTest {

    private static final String SAMPLE_XPM = String.join("\n",
        "/* XPM */",
        "static char *sample[] = {",
        "\"2 2 3 1\",",
        "\"r c #FF0000\",",
        "\"g c #00FF00\",",
        "\"b c blue\",",
        "\"rg\",",
        "\"bg\"",
        "};");

    @Test
    void decodesXpmWithHexAndNamedColors() throws IOException {
        ImageRGB image = new XPMImageDecoder()
            .decode(new ByteArrayInputStream(SAMPLE_XPM.getBytes(StandardCharsets.UTF_8)), "image/x-xpm");

        assertThat(image.getWidth()).isEqualTo(2);
        assertThat(image.getHeight()).isEqualTo(2);
        assertThat(image.getRGB(0, 0) & 0x00FFFFFF).isEqualTo(0xFF0000);
        assertThat(image.getRGB(1, 0) & 0x00FFFFFF).isEqualTo(0x00FF00);
        assertThat(image.getRGB(0, 1) & 0x00FFFFFF).isEqualTo(0x0000FF);
        assertThat(image.getRGB(1, 1) & 0x00FFFFFF).isEqualTo(0x00FF00);
    }

    @Test
    void rejectsSourceWithoutValuesLine() {
        String bad = "/* XPM */\nstatic char *broken[] = {\n};";

        assertThatThrownBy(() ->
            new XPMImageDecoder().decode(
                new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8)), null))
            .isInstanceOf(IOException.class);
    }

    @Test
    void extensionPatternMatchesXpmFilesCaseInsensitively() {
        ImageDecoder decoder = new XPMImageDecoder();
        String pattern = decoder.getFileExtensionPattern();

        assertThat("icon.xpm").matches(pattern);
        assertThat("ICON.XPM").matches(pattern);
        assertThat("icon.bmp").doesNotMatch(pattern);
        assertThat(decoder.getSupportedMimeTypes()).contains("image/x-xpm");
        assertThat(decoder.getFormatDescription()).isNotBlank();
    }
}
