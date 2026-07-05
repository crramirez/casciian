/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
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
package casciian.bits;

import casciian.backend.Backend;
import casciian.backend.SystemProperties;

/**
 * The attributes used by a Cell: color, bold, blink, etc.
 */
public class CellAttributes {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Bold attribute.
     */
    private static final int BOLD       = 0x01;

    /**
     * Blink attribute.
     */
    private static final int BLINK      = 0x02;

    /**
     * Reverse attribute.
     */
    private static final int REVERSE    = 0x04;

    /**
     * Underline attribute.
     */
    private static final int UNDERLINE  = 0x08;

    /**
     * Protected attribute.
     */
    private static final int PROTECT    = 0x10;

    /**
     * Default foreground color.
     */
    private static final int DEFAULT_FORECOLOR    = 0x20;

    /**
     * Default background color.
     */
    private static final int DEFAULT_BACKCOLOR    = 0x40;

    /**
     * Bold-transparent attribute.  When set, the bold attribute of this cell
     * must never be reinterpreted as a bright (high-intensity) color,
     * regardless of the {@code casciian.treatBoldAsBright} system property.
     * This is set on cells produced by terminal emulators (which parse an
     * incoming SGR stream) so that a received bold attribute is reproduced
     * faithfully.
     */
    private static final int BOLD_TRANSPARENT     = 0x80;

    /**
     * Animation bits for time-dependent transforms.
     */
    private static final int ANIMATION_MASK       = 0xFFFFF000;

    /**
     * Animation: pulse color.  8 bits: rrrgggbb.  Fades and pulses will go
     * between this color and the foregound RGB color.
     */
    private static final int ANIMATION_COLOR_MASK = 0xFF000000;

    /**
     * Animation: timer seed value.  Currently supporting 6 bits.
     */
    private static final int ANIMATION_TIME_MASK  = 0x00FC0000;

    /**
     * Animation: pulse slowly.
     */
    private static final int ANIMATION_PULSE      = 0x00004000;

    /**
     * Animation: pulse quickly.
     */
    private static final int ANIMATION_PULSE_FAST = 0x00008000;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Boolean flags.
     */
    private int flags = 0;

    /**
     * Foreground color.  Color.WHITE, Color.RED, etc.
     */
    private Color foreColor = Color.WHITE;

    /**
     * Background color.  Color.WHITE, Color.RED, etc.
     */
    private Color backColor = Color.BLACK;

    /**
     * Foreground color as 24-bit RGB value.  Negative value means not set.
     */
    private int foreColorRGB = -1;

    /**
     * Background color as 24-bit RGB value.  Negative value means not set.
     */
    private int backColorRGB = -1;

    /**
     * Foreground color as a 256-color palette index (0-255).  Negative value
     * means not set.
     *
     * @see Palette256
     */
    private int foreColorPalette = -1;

    /**
     * Background color as a 256-color palette index (0-255).  Negative value
     * means not set.
     *
     * @see Palette256
     */
    private int backColorPalette = -1;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get a new Builder instance.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Public constructor sets default values of the cell to white-on-black,
     * no bold/blink/reverse/underline/protect.
     *
     * @see #reset()
     */
    public CellAttributes() {
        // NOP
    }

    /**
     * Public constructor makes a copy from another instance.
     *
     * @param that another CellAttributes instance
     * @see #reset()
     */
    @SuppressWarnings("this-escape")
    public CellAttributes(final CellAttributes that) {
        setTo(that);
    }

    // ------------------------------------------------------------------------
    // CellAttributes ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Getter for bold.
     *
     * @return bold value
     */
    public final boolean isBold() {
        return ((flags & BOLD) != 0);
    }

    /**
     * Setter for bold.
     *
     * @param bold new bold value
     */
    public final void setBold(final boolean bold) {
        if (bold) {
            flags |= BOLD;
        } else {
            flags &= ~BOLD;
        }
    }

    /**
     * Getter for bold-transparent.
     *
     * @return bold-transparent value
     * @see #BOLD_TRANSPARENT
     */
    public final boolean isBoldTransparent() {
        return ((flags & BOLD_TRANSPARENT) != 0);
    }

    /**
     * Setter for bold-transparent.  When set, the bold attribute of this cell
     * is never reinterpreted as a bright color, regardless of the
     * {@code casciian.treatBoldAsBright} system property.
     *
     * @param boldTransparent new bold-transparent value
     * @see #BOLD_TRANSPARENT
     */
    public final void setBoldTransparent(final boolean boldTransparent) {
        if (boldTransparent) {
            flags |= BOLD_TRANSPARENT;
        } else {
            flags &= ~BOLD_TRANSPARENT;
        }
    }

    /**
     * Determine whether the bold attribute of this cell should be rendered as
     * a bright (high-intensity) color.
     *
     * <p>
     * Historically Casciian rendered bold text using the bright color
     * palette.  Since 1.6.0 the bold attribute is transparent by default: it
     * is emitted as a real SGR bold and the terminal decides how to display
     * it.  Bold selects the bright palette only when the
     * {@code casciian.treatBoldAsBright} system property is enabled and this
     * cell is not marked {@link #isBoldTransparent() bold-transparent}.
     * </p>
     *
     * @return true if this cell's bold attribute should map to a bright color
     */
    public final boolean isBoldAsBright() {
        return isBold()
            && !isBoldTransparent()
            && SystemProperties.isTreatBoldAsBright();
    }

    /**
     * Getter for blink.
     *
     * @return blink value
     */
    public final boolean isBlink() {
        return ((flags & BLINK) != 0);
    }

    /**
     * Setter for blink.
     *
     * @param blink new blink value
     */
    public final void setBlink(final boolean blink) {
        if (blink) {
            flags |= BLINK;
        } else {
            flags &= ~BLINK;
        }
    }

    /**
     * Getter for reverse.
     *
     * @return reverse value
     */
    public final boolean isReverse() {
        return ((flags & REVERSE) != 0);
    }

    /**
     * Setter for reverse.
     *
     * @param reverse new reverse value
     */
    public final void setReverse(final boolean reverse) {
        if (reverse) {
            flags |= REVERSE;
        } else {
            flags &= ~REVERSE;
        }
    }

    /**
     * Getter for underline.
     *
     * @return underline value
     */
    public final boolean isUnderline() {
        return ((flags & UNDERLINE) != 0);
    }

    /**
     * Setter for underline.
     *
     * @param underline new underline value
     */
    public final void setUnderline(final boolean underline) {
        if (underline) {
            flags |= UNDERLINE;
        } else {
            flags &= ~UNDERLINE;
        }
    }

    /**
     * Getter for protect.
     *
     * @return protect value
     */
    public final boolean isProtect() {
        return ((flags & PROTECT) != 0);
    }

    /**
     * Setter for protect.
     *
     * @param protect new protect value
     */
    public final void setProtect(final boolean protect) {
        if (protect) {
            flags |= PROTECT;
        } else {
            flags &= ~PROTECT;
        }
    }

    /**
     * Setter for default color.
     *
     * @param foreground if true, set the default for the foreground color
     * @param defaultColor new default value
     */
    public final void setDefaultColor(final boolean foreground,
        final boolean defaultColor) {

        if (foreground) {
            if (defaultColor) {
                flags |= DEFAULT_FORECOLOR;
            } else {
                flags &= ~DEFAULT_FORECOLOR;
            }
        } else {
            if (defaultColor) {
                flags |= DEFAULT_BACKCOLOR;
            } else {
                flags &= ~DEFAULT_BACKCOLOR;
            }
        }
    }

    /**
     * Getter for default color.
     *
     * @param foreground if true, get the default for the foreground color
     * @return true if the default color has been set
     */
    public final boolean isDefaultColor(final boolean foreground) {
        if (foreground) {
            return ((flags & DEFAULT_FORECOLOR) != 0);
        }
        return ((flags & DEFAULT_BACKCOLOR) != 0);
    }

    /**
     * Getter for animation flags.
     *
     * @return animation flags.
     */
    public final int getAnimations() {
        return (flags & ANIMATION_MASK);
    }

    /**
     * Setter for animations.
     *
     * @param animationFlags new animation flags
     */
    public final void setAnimations(final int animationFlags) {
        flags &= ~ANIMATION_MASK;
        flags |= animationFlags;
    }

    /**
     * Getter for foreColor.
     *
     * @return foreColor value
     */
    public final Color getForeColor() {
        return foreColor;
    }

    /**
     * Setter for foreColor.
     *
     * @param foreColor new foreColor value
     */
    public final void setForeColor(final Color foreColor) {
        this.foreColor = foreColor;
        this.foreColorRGB = -1;
        this.foreColorPalette = -1;
    }

    /**
     * Getter for backColor.
     *
     * @return backColor value
     */
    public final Color getBackColor() {
        return backColor;
    }

    /**
     * Setter for backColor.
     *
     * @param backColor new backColor value
     */
    public final void setBackColor(final Color backColor) {
        this.backColor = backColor;
        this.backColorRGB = -1;
        this.backColorPalette = -1;
    }

    /**
     * Get the current pulsed RGB value for foreColor.
     *
     * @param backend the backend that can obtain the correct ANSI
     * (palette-based) foreground color
     * @param pulseTimeMillis the time to compute the pulse color for in
     * millis
     * @return foreColor RGB value at this time slice.
     */
    public final int getForeColorPulseRGB(final Backend backend,
        final long pulseTimeMillis) {

        if ((flags & (ANIMATION_PULSE | ANIMATION_PULSE_FAST)) == 0) {
            // Not pulsed.
            if (foreColorRGB != -1) {
                return foreColorRGB;
            }
            return backend.attrToForegroundColor(this) & 0xFFFFFF;
        }

        int offsetSlice = (flags & ANIMATION_TIME_MASK) >>> 16;
        int slice;
        int sliceN = 16;
        int sliceI;
        if ((flags & ANIMATION_PULSE_FAST) == 0) {
            // This is a fast pulse: 32 steps / second
            sliceN = 32;
            slice = (int) (pulseTimeMillis * sliceN / 1000);
            sliceI = (slice + offsetSlice) & 0x1F;
        } else {
            // Slow pulse: 16 steps / second
            slice = (int) (pulseTimeMillis * (sliceN * 2) / 1000);
            sliceI = (slice + offsetSlice) & 0x0F;
        }
        if (sliceI >= (sliceN / 2)) {
            sliceI = sliceN - sliceI;
        }
        int pulseColorRGB = getPulseColorRGB();
        int fgRGB = backend.attrToForegroundColor(this);
        double fraction = sliceI * 2.0 / (sliceN - 1);
        return ImageUtils.rgbMove(fgRGB, pulseColorRGB, fraction);
    }

    /**
     * Getter for foreColor RGB.  Note that this is always a RGB value,
     * i.e. alpha is 0.
     *
     * @return foreColor value.  Negative means unset.
     */
    public final int getForeColorRGB() {
        return foreColorRGB;
    }

    /**
     * Setter for foreColor RGB.
     *
     * @param foreColorRGB new foreColor RGB value
     */
    public final void setForeColorRGB(final int foreColorRGB) {
        this.foreColorRGB = foreColorRGB & 0xFFFFFF;
        this.foreColor = Color.WHITE;
        this.foreColorPalette = -1;
    }

    /**
     * Getter for backColor RGB.  Note that this is always a RGB value,
     * i.e. alpha is 0.
     *
     * @return backColor value.  Negative means unset.
     */
    public final int getBackColorRGB() {
        return backColorRGB;
    }

    /**
     * Setter for backColor RGB.
     *
     * @param backColorRGB new backColor RGB value
     */
    public final void setBackColorRGB(final int backColorRGB) {
        this.backColorRGB = backColorRGB & 0xFFFFFF;
        this.backColor = Color.BLACK;
        this.backColorPalette = -1;
    }

    /**
     * Getter for foreColor 256-color palette index.
     *
     * @return foreColor palette index (0-255).  Negative means unset.
     * @see Palette256
     */
    public final int getForeColorPalette() {
        return foreColorPalette;
    }

    /**
     * Setter for foreColor 256-color palette index.  This is mutually
     * exclusive with the RGB foreground color: setting a palette index clears
     * any RGB foreground color.
     *
     * @param foreColorPalette new foreColor palette index (0-255)
     * @see Palette256
     */
    public final void setForeColorPalette(final int foreColorPalette) {
        this.foreColorPalette = foreColorPalette & 0xFF;
        this.foreColorRGB = -1;
        this.foreColor = Color.WHITE;
    }

    /**
     * Getter for backColor 256-color palette index.
     *
     * @return backColor palette index (0-255).  Negative means unset.
     * @see Palette256
     */
    public final int getBackColorPalette() {
        return backColorPalette;
    }

    /**
     * Setter for backColor 256-color palette index.  This is mutually
     * exclusive with the RGB background color: setting a palette index clears
     * any RGB background color.
     *
     * @param backColorPalette new backColor palette index (0-255)
     * @see Palette256
     */
    public final void setBackColorPalette(final int backColorPalette) {
        this.backColorPalette = backColorPalette & 0xFF;
        this.backColorRGB = -1;
        this.backColor = Color.BLACK;
    }

    /**
     * See if this cell uses RGB or ANSI colors.
     *
     * @return true if this cell has a RGB color
     */
    public final boolean isRGB() {
        return (foreColorRGB >= 0) || (backColorRGB >= 0);
    }

    /**
     * See if this cell uses a 256-color palette index for either its
     * foreground or background color.
     *
     * @return true if this cell has a 256-color palette color
     * @see Palette256
     */
    public final boolean isPalette() {
        return (foreColorPalette >= 0) || (backColorPalette >= 0);
    }

    /**
     * Set to default: white foreground on black background, no
     * bold/underline/blink/rever/protect.
     */
    public void reset() {
        flags           = 0;
        foreColor       = Color.WHITE;
        backColor       = Color.BLACK;
        foreColorRGB    = -1;
        backColorRGB    = -1;
        foreColorPalette = -1;
        backColorPalette = -1;
    }

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another CellAttributes instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof CellAttributes that)) {
            return false;
        }

        return ((flags == that.flags)
            && (foreColor == that.foreColor)
            && (backColor == that.backColor)
            && (foreColorRGB == that.foreColorRGB)
            && (backColorRGB == that.backColorRGB)
            && (foreColorPalette == that.foreColorPalette)
            && (backColorPalette == that.backColorPalette));
    }

    /**
     * Hashcode uses all fields in equals().
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        int a = 13;
        int b = 23;
        int hash = a;
        hash = (b * hash) + flags;
        hash = (b * hash) + foreColor.hashCode();
        hash = (b * hash) + backColor.hashCode();
        hash = (b * hash) + foreColorRGB;
        hash = (b * hash) + backColorRGB;
        hash = (b * hash) + foreColorPalette;
        hash = (b * hash) + backColorPalette;
        return hash;
    }

    /**
     * Set my field values to rhs's field.
     *
     * @param rhs another CellAttributes instance
     */
    public void setTo(final Object rhs) {
        CellAttributes that = (CellAttributes) rhs;

        this.flags              = that.flags;
        this.foreColor          = that.foreColor;
        this.backColor          = that.backColor;
        this.foreColorRGB       = that.foreColorRGB;
        this.backColorRGB       = that.backColorRGB;
        this.foreColorPalette   = that.foreColorPalette;
        this.backColorPalette   = that.backColorPalette;
    }

    /**
     * Make human-readable description of this CellAttributes.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        if ((foreColorPalette >= 0) || (backColorPalette >= 0)) {
            StringBuilder sb = new StringBuilder("Palette: ");

            if (foreColorPalette < 0) {
                sb.append(foreColor.toString());
            } else {
                sb.append(foreColorPalette);
            }
            sb.append(" on ");
            if (backColorPalette < 0) {
                sb.append(backColor.toString());
            } else {
                sb.append(backColorPalette);
            }
            return sb.toString();
        }
        if ((foreColorRGB >= 0) || (backColorRGB >= 0)) {
            StringBuilder sb = new StringBuilder("RGB: ");

            if (foreColorRGB < 0) {
                sb.append(foreColor.toRgbString());
            } else {
                sb.append(String.format("#%06x",
                        (foreColorRGB & 0xFFFFFF)));
            }
            sb.append(" on ");
            if (backColorRGB < 0) {
                sb.append(backColor.toRgbString());
            } else {
                sb.append(String.format("#%06x",
                        (backColorRGB & 0xFFFFFF)));
            }
            return sb.toString();
        }
        return String.format("%s%s%s on %s", (isBold() ? "bold " : ""),
            (isBlink() ? "blink " : ""), foreColor, backColor);
    }

    /**
     * Convert these cell attributes into the style attributes of an HTML
     * &lt;font&gt; tag.
     *
     * @return the HTML string
     */
    public String toHtml() {
        // The bold attribute maps to a bright color only when
        // treatBoldAsBright is enabled; otherwise it is rendered as a real
        // bold font weight so the appearance is left to the reader.
        boolean boldBright = isBoldAsBright();
        String fontWeight = (isBold() && !boldBright) ? "bold" : "normal";
        String textDecoration = "none";
        String fgText;
        String bgText;

        if (isBlink() && isUnderline()) {
            textDecoration = "blink, underline";
        } else if (isUnderline()) {
            textDecoration = "underline";
        } else if (isBlink()) {
            textDecoration = "blink";
        }
        if (isReverse()) {
            fgText = backColor.toRgbString(false);
            if (boldBright) {
                bgText = foreColor.toRgbString(true);
            } else {
                bgText = foreColor.toRgbString(false);
            }
        } else {
            bgText = backColor.toRgbString(false);
            if (boldBright) {
                fgText = foreColor.toRgbString(true);
            } else {
                fgText = foreColor.toRgbString(false);
            }
        }

        return String.format("style=\"color: %s; background-color: %s; " +
            "text-decoration: %s; font-weight: %s\"",
            fgText, bgText, textDecoration, fontWeight);
    }

    /**
     * Getter for animation pulse.
     *
     * @return pulse value
     */
    public final boolean isPulse() {
        return (flags & (ANIMATION_PULSE | ANIMATION_PULSE_FAST)) != 0;
    }

    /**
     * Setter for animation pulse.
     *
     * @param pulse new pulse value
     * @param fast if true, fast pulse
     * @param offset number of frames (up to 16 for slow, up to 32 for
     * fast) to offset the pulse animation
     */
    public final void setPulse(final boolean pulse, final boolean fast,
        int offset) {

        flags &= ~(ANIMATION_PULSE | ANIMATION_PULSE_FAST);
        if (!pulse && !fast) {
            return;
        }
        if (fast) {
            flags |= ANIMATION_PULSE_FAST;
            offset &= 0x1F;
        } else {
            flags |= ANIMATION_PULSE;
            offset &= 0x0F;
        }
        flags &= ~ANIMATION_TIME_MASK;
        flags |= (offset & 0x3F) << 16;
    }

    /**
     * Getter for pulse color RGB.
     *
     * @return pulse color RGB value
     */
    public final int getPulseColorRGB() {
        int pulseColor = (flags >>> 24) & 0xFF;
        return ((pulseColor & 0xE0) << 16)
                | ((pulseColor & 0x1C) << 11)
                | ((pulseColor & 0x03) << 6);
    }

    /**
     * Setter for pulse color RGB.
     *
     * @param pulseColorRGB new pulse color RGB value
     */
    public final void setPulseColorRGB(final int pulseColorRGB) {
        int color = ((pulseColorRGB & 0xE00000) >>> 16)
                | ((pulseColorRGB & 0xE000) >>> 11)
                | ((pulseColorRGB & 0xC0) >>> 6);

        flags &= ~ANIMATION_COLOR_MASK;
        flags |= color << 24;

    }

    /**
     * Set foreground color to match background color.
     */
    public final void setInvisibleForeColor() {
        foreColorRGB = backColorRGB;
        foreColor = backColor;
    }

    /**
     * Dim the foreground color towards background color.
     *
     * @param backend the backend that can obtain the correct ANSI
     * (palette-based) foreground color
     * @param percent a number between 0 and 100. 0 means the foreground color
     * is unchanged; 100 means the foreground color matches the background
     * color.
     */
    public final void setDimmedForeColor(final Backend backend,
        final int percent) {

        if (percent <= 0) {
            return;
        }
        if (percent >= 100) {
            setInvisibleForeColor();
        }

        int backRGB = backColorRGB;
        if (backRGB < 0) {
            backRGB = backend.attrToBackgroundColor(this) & 0xFFFFFF;
        }

        if (foreColorRGB < 0) {
            foreColorRGB = backend.attrToForegroundColor(this) & 0xFFFFFF;
        }

        foreColorRGB = ImageUtils.rgbMove(foreColorRGB, backRGB,
            percent / 100.0);
    }

    /**
     * Builder for CellAttributes.
     */
    public static class Builder {

        private final CellAttributes attributes = new CellAttributes();

        /**
         * Public constructor.
         */
        public Builder() {
            // NOP
        }

        /**
         * Set the bold flag.
         *
         * @param bold new bold flag
         * @return this builder
         */
        public Builder bold(final boolean bold) {
            attributes.setBold(bold);
            return this;
        }

        /**
         * Set the bold-transparent flag.
         *
         * @param boldTransparent new bold-transparent flag
         * @return this builder
         * @see CellAttributes#setBoldTransparent(boolean)
         */
        public Builder boldTransparent(final boolean boldTransparent) {
            attributes.setBoldTransparent(boldTransparent);
            return this;
        }

        /**
         * Set the blink flag.
         *
         * @param blink new blink flag
         * @return this builder
         */
        public Builder blink(final boolean blink) {
            attributes.setBlink(blink);
            return this;
        }

        /**
         * Set the reverse flag.
         *
         * @param reverse new reverse flag
         * @return this builder
         */
        public Builder reverse(final boolean reverse) {
            attributes.setReverse(reverse);
            return this;
        }

        /**
         * Set the underline flag.
         *
         * @param underline new underline flag
         * @return this builder
         */
        public Builder underline(final boolean underline) {
            attributes.setUnderline(underline);
            return this;
        }

        /**
         * Set the protect flag.
         *
         * @param protect new protect flag
         * @return this builder
         */
        public Builder protect(final boolean protect) {
            attributes.setProtect(protect);
            return this;
        }

        /**
         * Set the foreground color.
         *
         * @param foreColor new foreground color
         * @return this builder
         */
        public Builder foreColor(final Color foreColor) {
            attributes.setForeColor(foreColor);
            return this;
        }

        /**
         * Set the background color.
         *
         * @param backColor new background color
         * @return this builder
         */
        public Builder backColor(final Color backColor) {
            attributes.setBackColor(backColor);
            return this;
        }

        /**
         * Set the foreground RGB color.
         *
         * @param foreColorRGB new foreground RGB color
         * @return this builder
         */
        public Builder foreColorRGB(final int foreColorRGB) {
            attributes.setForeColorRGB(foreColorRGB);
            return this;
        }

        /**
         * Set the background RGB color.
         *
         * @param backColorRGB new background RGB color
         * @return this builder
         */
        public Builder backColorRGB(final int backColorRGB) {
            attributes.setBackColorRGB(backColorRGB);
            return this;
        }

        /**
         * Set the foreground 256-color palette index.
         *
         * @param foreColorPalette new foreground palette index (0-255)
         * @return this builder
         * @see Palette256
         */
        public Builder foreColorPalette(final int foreColorPalette) {
            attributes.setForeColorPalette(foreColorPalette);
            return this;
        }

        /**
         * Set the background 256-color palette index.
         *
         * @param backColorPalette new background palette index (0-255)
         * @return this builder
         * @see Palette256
         */
        public Builder backColorPalette(final int backColorPalette) {
            attributes.setBackColorPalette(backColorPalette);
            return this;
        }

        /**
         * Set whether the foreground or background uses its default color.
         *
         * @param foreground if true, update the foreground default color flag;
         * otherwise update the background default color flag
         * @param defaultColor new default color flag
         * @return this builder
         */
        public Builder defaultColor(final boolean foreground,
            final boolean defaultColor) {
            attributes.setDefaultColor(foreground, defaultColor);
            return this;
        }

        /**
         * Set the animation flags.
         *
         * @param animationFlags new animation flags
         * @return this builder
         */
        public Builder animations(final int animationFlags) {
            attributes.setAnimations(animationFlags);
            return this;
        }

        /**
         * Set the pulse animation state.
         *
         * @param pulse new pulse flag
         * @param fast new fast pulse flag
         * @param offset new pulse offset
         * @return this builder
         */
        public Builder pulse(final boolean pulse, final boolean fast,
            final int offset) {
            attributes.setPulse(pulse, fast, offset);
            return this;
        }

        /**
         * Set the pulse color RGB value.
         *
         * @param pulseColorRGB new pulse color RGB value
         * @return this builder
         */
        public Builder pulseColorRGB(final int pulseColorRGB) {
            attributes.setPulseColorRGB(pulseColorRGB);
            return this;
        }

        /**
         * Build a new CellAttributes instance.
         *
         * @return new CellAttributes instance
         */
        public CellAttributes build() {
            return new CellAttributes(attributes);
        }
    }


}
