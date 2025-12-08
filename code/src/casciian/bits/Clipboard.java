/*
 * Casciian - Java Text User Interface
 *
 * Written 2013-2025 by Autumn Lamonte
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package casciian.bits;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Clipboard provides convenience methods to copy text and images to and from
 * a shared clipboard.  When the system clipboard is available it is used.
 */
public class Clipboard {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The image last copied to the clipboard.
     */
    private BufferedImage image = null;

    /**
     * The text string last copied to the clipboard.
     */
    private String text = null;

    /**
     * The system clipboard, or null if it is not available.
     */
    private java.awt.datatransfer.Clipboard systemClipboard = null;

    /**
     * The image selection class.
     */
    private ImageSelection imageSelection;

    /**
     * ImageSelection is used to hold an image while on the clipboard.
     */
    private class ImageSelection implements Transferable {

        /**
         * Returns an array of DataFlavor objects indicating the flavors the
         * data can be provided in. The array should be ordered according to
         * preference for providing the data (from most richly descriptive to
         * least descriptive).
         *
         * @return an array of data flavors in which this data can be
         * transferred
         */
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        /**
         * Returns whether or not the specified data flavor is supported for
         * this object.
         *
         * @param flavor the requested flavor for the data
         * @return boolean indicating whether or not the data flavor is
         * supported
         */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        /**
         * Returns an object which represents the data to be transferred. The
         * class of the object returned is defined by the representation
         * class of the flavor.
         *
         * @param flavor the requested flavor for the data
         * @throws IOException if the data is no longer available in the
         * requested flavor.
         * @throws UnsupportedFlavorException if the requested data flavor is
         * not supported.
         */
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {

            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     */
    public Clipboard() {
        try {
            systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (java.awt.HeadlessException e) {
            // SQUASH
        }
    }

    // ------------------------------------------------------------------------
    // Clipboard --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Copy an image to the clipboard.
     *
     * @param image image to copy
     */
    public void copyImage(final BufferedImage image) {
        this.image = image;
        if (systemClipboard != null) {
            ImageSelection imageSelection = new ImageSelection();
            systemClipboard.setContents(imageSelection, null);
        }
    }

    /**
     * Copy a text string to the clipboard.
     *
     * @param text string to copy
     */
    public void copyText(final String text) {
        this.text = text;
        if (systemClipboard != null) {
            StringSelection stringSelection = new StringSelection(text);
            systemClipboard.setContents(stringSelection, null);
        }
    }

    /**
     * Obtain an image from the clipboard.
     *
     * @return image from the clipboard, or null if no image is available
     */
    public BufferedImage pasteImage() {
        if (systemClipboard != null) {
            getClipboardImage();
        }
        return image;
    }

    /**
     * Obtain a text string from the clipboard.
     *
     * @return text string from the clipboard, or null if no text is
     * available
     */
    public String pasteText() {
        if (systemClipboard != null) {
            getClipboardText();
        }
        return text;
    }

    /**
     * Returns true if the clipboard has an image.
     *
     * @return true if an image is available from the clipboard
     */
    public boolean isImage() {
        if (image == null) {
            getClipboardImage();
        }
        return (image != null);
    }

    /**
     * Returns true if the clipboard has a text string.
     *
     * @return true if a text string is available from the clipboard
     */
    public boolean isText() {
        if (text == null) {
            getClipboardText();
        }
        return (text != null);
    }

    /**
     * Returns true if the clipboard is empty.
     *
     * @return true if the clipboard is empty
     */
    public boolean isEmpty() {
        return ((isText() == false) && (isImage() == false));
    }

    /**
     * Copy image from the clipboard to this.image.
     */
    private void getClipboardImage() {
        if (systemClipboard != null) {
            Transferable contents = systemClipboard.getContents(null);
            if (contents != null) {
                if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    try {
                        Image img = (Image) contents.getTransferData(DataFlavor.imageFlavor);
                        image = new BufferedImage(img.getWidth(null),
                            img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                        image.getGraphics().drawImage(img, 0, 0, null);
                    } catch (IOException e) {
                        // SQUASH
                    } catch (UnsupportedFlavorException e) {
                        // SQUASH
                    }
                }
            }
        }
    }

    /**
     * Copy text string from the clipboard to this.text.
     */
    private void getClipboardText() {
        if (systemClipboard != null) {
            Transferable contents = systemClipboard.getContents(null);
            if (contents != null) {
                if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    try {
                        text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    } catch (IOException e) {
                        // SQUASH
                    } catch (UnsupportedFlavorException e) {
                        // SQUASH
                    }
                }
            }
        }
    }

    /**
     * Clear whatever is on the local clipboard.  Note that this will not
     * clear the system clipboard.
     */
    public void clear() {
        image = null;
        text = null;
    }

}
