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

/**
 * Casciian - Java Text User Interface library
 *
 * <p>
 * This library is a text-based windowing system loosely reminiscent of
 * Borland's <a href="http://en.wikipedia.org/wiki/Turbo_Vision">Turbo
 * Vision</a> library.  Casciian's goal is to enable people to get up and
 * running with minimum hassle and lots of polish.  A very quick "Hello
 * World" application can be created as simply as this:
 *
 * <pre>
 * {@code
 * import casciian.TApplication;
 *
 * public class MyApplication extends TApplication {
 *
 *     public MyApplication() throws Exception {
 *         super(BackendType.XTERM);
 *
 *         // Create standard menus for Tool, File, and Window.
 *         addToolMenu();
 *         addFileMenu();
 *         addWindowMenu();
 *     }
 *
 *     public static void main(String [] args) throws Exception {
 *         MyApplication app = new MyApplication();
 *         app.run();
 *     }
 * }
 * }
 * </pre>
 */
package casciian;
