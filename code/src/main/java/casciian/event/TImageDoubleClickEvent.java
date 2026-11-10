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
package casciian.event;

import java.util.Objects;

/**
 * Raised when the user double-clicks an image displayed inside a rich-text
 * widget such as {@link casciian.TTextAnsi}.  It carries the full path of the
 * image file and its alternative text, so listeners can, for example, open
 * the image in a {@link casciian.TImageWindow} to zoom or inspect it.
 */
public final class TImageDoubleClickEvent {

    private final String imagePath;
    private final String altText;
    private final TMouseEvent mouse;

    /**
     * Public constructor.
     *
     * @param imagePath the full (absolute, normalized) path of the image
     * @param altText the alternative text of the image, never null
     * @param mouse the double-click mouse event, relative to the widget
     */
    public TImageDoubleClickEvent(final String imagePath, final String altText,
            final TMouseEvent mouse) {
        this.imagePath = Objects.requireNonNull(imagePath, "imagePath");
        this.altText = altText == null ? "" : altText;
        this.mouse = mouse;
    }

    /**
     * Get the full path of the double-clicked image.
     *
     * @return the absolute, normalized image path
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * Get the alternative text of the double-clicked image.
     *
     * @return the alternative text, possibly empty
     */
    public String getAltText() {
        return altText;
    }

    /**
     * Get the mouse event that triggered this event.
     *
     * @return the double-click mouse event
     */
    public TMouseEvent getMouse() {
        return mouse;
    }

    @Override
    public String toString() {
        return String.format("ImageDoubleClick: %s [%s]", imagePath, altText);
    }
}
