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
package casciian.tackboard;

/**
 * Pointer is an item that has a hotspot location to represent the "tip" of a
 * mouse icon.
 */
public interface Pointer {

    /**
     * Get the hotspot X location relative to the X location of the icon.
     *
     * @return the X location
     */
    public int getHotspotX();

    /**
     * Get the hotspot Y location relative to the Y location of the icon.
     *
     * @return the Y location
     */
    public int getHotspotY();

}
