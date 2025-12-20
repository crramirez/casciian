package casciian.backend;

/**
 * System properties used by Casciian.
 */
public class SystemProperties {

    /**
     * System property key for shadow opacity.
     * Valid values: 0-100 (percentage)
     * Default: 60
     */
    public static final String CASCIIAN_SHADOW_OPACITY = "casciian.shadowOpacity";

    private SystemProperties() {
    }

    /**
     * Get the shadow opacity value from system properties.
     *
     * @return shadow opacity value between 0 and 100, or default value of 60
     */
    public static int getShadowOpacity() {
        final int defaultShadowOpacity = 60;
        final int shadowOpacityValue = Integer.getInteger(CASCIIAN_SHADOW_OPACITY, defaultShadowOpacity);

        return shadowOpacityValue >= 0 && shadowOpacityValue <= 100 ? shadowOpacityValue : defaultShadowOpacity;
    }

    /**
     * Set the shadow opacity value in system properties.
     * Values outside the valid range (0-100) will be clamped to the nearest valid value.
     *
     * @param value shadow opacity value (will be clamped to 0-100)
     */
    public static void setShadowOpacity(int value) {
        int clampedValue = value;
        if (clampedValue < 0) {
            clampedValue = 0;
        } else if (clampedValue > 100) {
            clampedValue = 100;
        }
        System.setProperty(CASCIIAN_SHADOW_OPACITY, String.valueOf(clampedValue));
    }
}
