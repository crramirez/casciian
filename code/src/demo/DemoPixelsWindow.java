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
package demo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import casciian.TAction;
import casciian.TApplication;
import casciian.TEditorWindow;
import casciian.TLabel;
import casciian.TTimer;
import casciian.TWidget;
import casciian.TWindow;
import casciian.bits.Animation;
import casciian.bits.ImageUtils;
import casciian.effect.GradientCellTransform;
import casciian.event.TCommandEvent;
import casciian.layout.StretchLayoutManager;
import casciian.tackboard.Bitmap;
import casciian.tackboard.MousePointer;
import casciian.tackboard.TackboardItem;
import casciian.tackboard.Text;
import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * Pixel-based operations.
 */
public class DemoPixelsWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * Timer that moves things.
     */
    private TTimer timer3;

    /**
     * Timer label is updated with timer ticks.
     */
    TLabel timerLabel;

    /**
     * Direction for the bitmaps to move.
     */
    boolean direction = true;

    /**
     * The floating text.
     */
    Text floatingText = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent the main application
     */
    @SuppressWarnings("this-escape")
    public DemoPixelsWindow(final TApplication parent) {
        // Construct a demo window.  X and Y don't matter because it will be
        // centered on screen.
        super(parent, "", 0, 0, 72, 17, CENTERED | RESIZABLE);
        i18n = ResourceBundle.getBundle(DemoPixelsWindow.class.getName(),
            getLocale());
        setTitle(i18n.getString("windowTitle"));

        setLayoutManager(new StretchLayoutManager(getWidth() - 2,
                getHeight() - 2));

        int row = 1;
        int col = 43;

        // Add some widgets
        addLabel(i18n.getString("customMouseLabel"), 1, row);
        TWidget first = addButton(i18n.getString("customMouseButton"), col, row,
            new TAction() {
                public void DO() {
                    TackboardItem mouse = getApplication().getCustomMousePointer();
                    if (mouse != null) {
                        // Turn it off.
                        getApplication().setCustomMousePointer(null);
                    } else {
                        // Turn it on.
                        try {
                            ClassLoader loader;
                            loader = Thread.currentThread().getContextClassLoader();
                            BufferedImage image;
                            image = ImageIO.read(loader.
                                getResource("demo/cute_icon.png"));
                            TApplication app = getApplication();
                            app.setCustomMousePointer(new MousePointer(0, 0, 0,
                                    image, image.getWidth() / 2,
                                    image.getHeight() / 2));
                        } catch (Exception e) {
                            new casciian.TExceptionDialog(getApplication(), e);
                        }
                    }
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("floatingTextLabel"), 1, row);
        addButton(i18n.getString("floatingTextButton"), col, row,
            new TAction() {
                public void DO() {
                    if (floatingText == null) {
                        int fontSize = 31;
                        Font fontRoot = null;
                        Font font = null;
                        try {
                            ClassLoader loader = Thread.currentThread().getContextClassLoader();
                            InputStream in = loader.getResourceAsStream("demo/5thgradecursive.ttf");
                            fontRoot = Font.createFont(Font.TRUETYPE_FONT, in);
                            font = fontRoot.deriveFont(Font.PLAIN, fontSize);
                        } catch (FontFormatException e) {
                            font = new Font(Font.SANS_SERIF, Font.PLAIN,
                                fontSize);
                        } catch (IOException e) {
                            font = new Font(Font.SANS_SERIF, Font.PLAIN,
                                fontSize);
                        }
                        floatingText = new Text(30, 21, 2,
                            i18n.getString("heatFromFire"), font, fontSize,
                            new java.awt.Color(0xF7, 0xA8, 0xB8));
                        addOverlay(floatingText);
                    } else {
                        floatingText.remove();
                        floatingText = null;
                    }
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("textField1"), 1, row);
        TWidget field = addField(col, row, 15, false, "Field text");
        try {
            ClassLoader loader;
            loader = Thread.currentThread().getContextClassLoader();
            BufferedImage image;
            image = ImageIO.read(loader.
                getResource("demo/ibeam.png"));
            TApplication app = getApplication();
            field.setCustomMousePointer(new MousePointer(0, 0, 0,
                    image, 24, 24));
        } catch (Exception e) {
            new casciian.TExceptionDialog(getApplication(), e);
        }
        row += 2;

        // TODO: more things

        // Put some floating things on the screen.
        try {
            ClassLoader loader;
            loader = Thread.currentThread().getContextClassLoader();
            BufferedImage image;
            image = ImageIO.read(loader.getResource("demo/trans_icon.png"));
            Animation animation;
            animation = ImageUtils.getAnimation(loader.getResource(
                "demo/butterfly.gif"));
            addUnderlay(new Bitmap(17, 33, 0, animation, getApplication()));
            addOverlay(new Bitmap(15, 60, 0, image));

            timer3 = getApplication().addTimer(100, true,
                new TAction() {
                    public void DO() {
                        // System.err.println("Pixels: tick");

                        List<TackboardItem> items;
                        items = new ArrayList<TackboardItem>();
                        if (underlay != null) {
                            items.addAll(underlay.getItems());
                        }
                        if (overlay != null) {
                            items.addAll(overlay.getItems());
                        }
                        int i = 0;
                        for (TackboardItem item: items) {
                            if (item instanceof Text) {
                                continue;
                            }

                            i++;
                            int x = item.getX();
                            int y = item.getY();
                            if (i % 2 == 0) {
                                if (direction) {
                                    item.setX(x + 1);
                                } else {
                                    item.setX(x - 1);
                                }
                            } else {
                                if (direction) {
                                    item.setY(y + 1);
                                } else {
                                    item.setY(y - 1);
                                }
                            }
                            if ((item.getX() < 0)
                                || (item.getX() > 300)
                                || (item.getY() < 0)
                                || (item.getY() > 200)
                            ) {
                                direction = !direction;
                            }
                        }
                    }
                }
            );
        } catch (Exception e) {
            new casciian.TExceptionDialog(getApplication(), e);
        }

        activate(first);

        statusBar = newStatusBar(i18n.getString("statusBar"));
        statusBar.addShortcutKeypress(kbF1, cmHelp,
            i18n.getString("statusBarHelp"));
        statusBar.addShortcutKeypress(kbF2, cmShell,
            i18n.getString("statusBarShell"));
        statusBar.addShortcutKeypress(kbF3, cmOpen,
            i18n.getString("statusBarOpen"));
        statusBar.addShortcutKeypress(kbF10, cmExit,
            i18n.getString("statusBarExit"));
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * We need to override onClose so that the timer will no longer be called
     * after we close the window.  TTimers currently are completely unaware
     * of the rest of the UI classes.
     */
    @Override
    public void onClose() {
        super.onClose();
        // Just in case, make sure timer stops ticking.
        if (timer3 != null) {
            timer3.setRecurring(false);
        }
        getApplication().removeTimer(timer3);
    }

    /**
     * Method that subclasses can override to handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmOpen)) {
            try {
                String filename = fileOpenBox(".");
                if (filename != null) {
                    try {
                        new TEditorWindow(getApplication(),
                            new File(filename));
                    } catch (IOException e) {
                        messageBox(i18n.getString("errorTitle"),
                            MessageFormat.format(i18n.
                                getString("errorReadingFile"), e.getMessage()));
                    }
                }
            } catch (IOException e) {
                        messageBox(i18n.getString("errorTitle"),
                            MessageFormat.format(i18n.
                                getString("errorOpeningFile"), e.getMessage()));
            }
            return;
        }

        // Didn't handle it, let children get it instead
        super.onCommand(command);
    }

    /**
     * Enable or disable a pre-defined gradient for this window's color.
     *
     * @param useGradient if true, paint this window with a gradient
     */
    public void setUseGradient(final boolean useGradient) {
        if (useGradient) {
            Color PINK = new Color(0xf7, 0xa8, 0xb8);
            Color BLUE = new Color(0x55, 0xcd, 0xfc);
            setDrawPreTransform(new GradientCellTransform(
                GradientCellTransform.Layer.BACKGROUND, BLUE, BLUE,
                PINK, PINK));
        } else {
            setDrawPreTransform(null);
        }
    }

}
