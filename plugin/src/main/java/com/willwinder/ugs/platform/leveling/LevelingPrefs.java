/*
 * Copyright (C) 2026 UGS Leveling Generator plugin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.willwinder.ugs.platform.leveling;

import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * Persistent storage for the Leveling (facing) generator parameters.
 *
 * <p>The keys and defaults mirror the {@code USER PARAMETERS} and
 * {@code advanced / rarely-changed settings} blocks of the original
 * {@code leveling_generator.py} script, so the values a user sees here are the
 * same ones they would have edited at the top of that script.</p>
 */
public class LevelingPrefs {

    public static final Preferences PREFS = NbPreferences.forModule(LevelingTopComponent.class);

    // ---- keys (names match the python variables) ----
    public static final String KEY_STOCK_X          = "stock_x";
    public static final String KEY_STOCK_Y          = "stock_y";
    public static final String KEY_STEPOVER         = "stepover";
    public static final String KEY_STEPDOWN         = "stepdown";
    public static final String KEY_FINAL_THICKNESS  = "final_thickness";
    public static final String KEY_NUM_PASSES       = "num_passes";

    public static final String KEY_FEED             = "feed";
    public static final String KEY_SAFE_Z           = "safe_z";
    public static final String KEY_AUTO_SAFE_Z      = "auto_safe_z";
    public static final String KEY_SPINDLE_SPEED    = "spindle_speed";
    public static final String KEY_DWELL_SECONDS    = "dwell_seconds";

    // ---- defaults (identical to the python script) ----
    public static final double  DEFAULT_STOCK_X         = 590.0;
    public static final double  DEFAULT_STOCK_Y         = 140.0;
    public static final double  DEFAULT_STEPOVER        = 10.0;
    public static final double  DEFAULT_STEPDOWN        = 1.0;
    public static final double  DEFAULT_FINAL_THICKNESS = 10.0;
    public static final int     DEFAULT_NUM_PASSES      = 1;

    public static final double  DEFAULT_FEED            = 3000;
    public static final double  DEFAULT_SAFE_Z          = 2.0;
    // The script's __main__ block sets safe_z = final_thickness + 2.0, so the
    // effective default behaviour is "auto".
    public static final boolean DEFAULT_AUTO_SAFE_Z     = true;
    public static final double  DEFAULT_SPINDLE_SPEED   = 30000;
    public static final int     DEFAULT_DWELL_SECONDS   = 2;

    // ---- setters ----
    void putDouble(String key, double value)  { PREFS.putDouble(key, value); }
    void putInt(String key, int value)        { PREFS.putInt(key, value); }
    void putBoolean(String key, boolean value){ PREFS.putBoolean(key, value); }

    // ---- getters ----
    public double  stockX()         { return PREFS.getDouble(KEY_STOCK_X, DEFAULT_STOCK_X); }
    public double  stockY()         { return PREFS.getDouble(KEY_STOCK_Y, DEFAULT_STOCK_Y); }
    public double  stepover()       { return PREFS.getDouble(KEY_STEPOVER, DEFAULT_STEPOVER); }
    public double  stepdown()       { return PREFS.getDouble(KEY_STEPDOWN, DEFAULT_STEPDOWN); }
    public double  finalThickness() { return PREFS.getDouble(KEY_FINAL_THICKNESS, DEFAULT_FINAL_THICKNESS); }
    public int     numPasses()      { return PREFS.getInt(KEY_NUM_PASSES, DEFAULT_NUM_PASSES); }

    public double  feed()           { return PREFS.getDouble(KEY_FEED, DEFAULT_FEED); }
    public boolean autoSafeZ()      { return PREFS.getBoolean(KEY_AUTO_SAFE_Z, DEFAULT_AUTO_SAFE_Z); }
    public double  spindleSpeed()   { return PREFS.getDouble(KEY_SPINDLE_SPEED, DEFAULT_SPINDLE_SPEED); }
    public int     dwellSeconds()   { return PREFS.getInt(KEY_DWELL_SECONDS, DEFAULT_DWELL_SECONDS); }

    /**
     * Effective safe-Z height. When "auto" is enabled this reproduces the
     * script's {@code safe_z = final_thickness + 2.0} override; otherwise the
     * user-entered absolute value is used.
     */
    public double safeZ() {
        if (autoSafeZ()) {
            return finalThickness() + 2.0;
        }
        return PREFS.getDouble(KEY_SAFE_Z, DEFAULT_SAFE_Z);
    }

    /** Raw stored safe-Z value (ignores the auto flag) - used to populate the UI field. */
    public double rawSafeZ() { return PREFS.getDouble(KEY_SAFE_Z, DEFAULT_SAFE_Z); }

    /** Write every value back, materialising defaults on first run. */
    public void updateAllPrefs() {
        PREFS.putDouble(KEY_STOCK_X, stockX());
        PREFS.putDouble(KEY_STOCK_Y, stockY());
        PREFS.putDouble(KEY_STEPOVER, stepover());
        PREFS.putDouble(KEY_STEPDOWN, stepdown());
        PREFS.putDouble(KEY_FINAL_THICKNESS, finalThickness());
        PREFS.putInt(KEY_NUM_PASSES, numPasses());

        PREFS.putDouble(KEY_FEED, feed());
        PREFS.putDouble(KEY_SAFE_Z, rawSafeZ());
        PREFS.putBoolean(KEY_AUTO_SAFE_Z, autoSafeZ());
        PREFS.putDouble(KEY_SPINDLE_SPEED, spindleSpeed());
        PREFS.putInt(KEY_DWELL_SECONDS, dwellSeconds());
    }
}
