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

import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.utils.GUIHelpers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Faithful Java port of {@code leveling_generator.py}.
 *
 * <p>Produces a serpentine (boustrophedon) facing raster that:</p>
 * <ul>
 *   <li>covers the full {@code stockX x stockY} rectangle, edge to edge;</li>
 *   <li>steps down in Z each full pass with no retract between passes;</li>
 *   <li>reverses row direction each pass so it never needs a rapid move;</li>
 *   <li>only retracts to a safe height at the very start and very end.</li>
 * </ul>
 *
 * <p>The three python functions map directly onto {@link #buildRows},
 * {@link #buildDepths} and {@link #generate}. All output is in millimetres
 * (G21), exactly as the original script, independent of the UGS unit setting.</p>
 */
public class LevelingGcode {

    private LevelingPrefs prefs;
    private BackendAPI backend;
    private File file = null;

    public void init(LevelingPrefs prefs, BackendAPI backend) {
        this.prefs = Objects.requireNonNull(prefs);
        this.backend = Objects.requireNonNull(backend);
    }

    public File getFile() { return file; }

    /**
     * Port of {@code build_rows(length, step)}: row positions from 0 to length,
     * evenly stepped, with a final row forced onto the far edge if the step
     * doesn't divide evenly. A duplicated final row (when the step divides
     * evenly) is removed.
     */
    static List<Double> buildRows(double length, double step) {
        List<Double> rows = new ArrayList<>();
        rows.add(0.0);
        double y = 0.0;
        while (y < length) {
            y += step;
            rows.add(Math.min(y, length));
        }
        if (rows.size() > 1 && Math.abs(rows.get(rows.size() - 1) - rows.get(rows.size() - 2)) < 1e-9) {
            rows.remove(rows.size() - 1);
        }
        return rows;
    }

    /**
     * Port of {@code build_depths(final_thickness, step, num_passes)}: pass
     * depths counting down to final_thickness, step mm apart. The first pass is
     * the highest (roughest) and the last lands exactly on final_thickness.
     */
    static List<Double> buildDepths(double finalThickness, double step, int numPasses) {
        List<Double> depths = new ArrayList<>();
        for (int i = 0; i < numPasses; i++) {
            depths.add(finalThickness + (numPasses - 1 - i) * step);
        }
        return depths;
    }

    /**
     * Port of {@code generate(...)}. When {@code targetFile} is {@code null} a
     * temp file is created (matching the plugin's "Generate" action); otherwise
     * the given file is written (the "Export" action).
     *
     * @return a short human-readable summary of what was produced.
     */
    public String generate(File targetFile) {
        double stockX         = prefs.stockX();
        double stockY         = prefs.stockY();
        double stepover       = prefs.stepover();
        double stepdown       = prefs.stepdown();
        double finalThickness = prefs.finalThickness();
        int    numPasses      = prefs.numPasses();
        double feed           = prefs.feed();
        double safeZ          = prefs.safeZ();
        double spindleSpeed   = prefs.spindleSpeed();
        int    dwellSeconds   = prefs.dwellSeconds();

        List<Double> rows = buildRows(stockY, stepover);
        List<Double> depths = buildDepths(finalThickness, stepdown, numPasses);

        List<String> lines = new ArrayList<>();
        lines.add("G21");
        lines.add("G90");
        lines.add(String.format(Locale.US, "G0 X0.00 Y0.00 Z%.2f", safeZ));
        lines.add(String.format(Locale.US, "S%d", Math.round(spindleSpeed)));
        lines.add("M3");
        lines.add(String.format(Locale.US, "G4 P%d", dwellSeconds));

        boolean goingToFarX = true;   // next X move targets stockX; flips each move
        boolean firstMove = true;     // only the very first G1 gets an explicit F

        for (int passNum = 0; passNum < depths.size(); passNum++) {
            double z = depths.get(passNum);
            lines.add(String.format(Locale.US, "; pass %d of %d  Z=%.4f",
                    passNum + 1, depths.size(), z));

            // Shallow skim on the first pass gets a feed-controlled sink; every
            // later pass is already in the material and sinks without an F word
            // (matching the python behaviour exactly).
            if (passNum == 0) {
                lines.add(String.format(Locale.US, "G1 Z%.4f F%d", z, Math.round(feed)));
            } else {
                lines.add(String.format(Locale.US, "G1 Z%.4f", z));
            }

            List<Double> rowSequence;
            if (passNum % 2 == 0) {
                rowSequence = rows;
            } else {
                rowSequence = new ArrayList<>(rows);
                java.util.Collections.reverse(rowSequence);
            }

            for (int i = 0; i < rowSequence.size(); i++) {
                double y = rowSequence.get(i);
                if (i > 0) {
                    lines.add(String.format(Locale.US, "G1 Y%.2f", y));
                }
                double targetX = goingToFarX ? stockX : 0.0;
                if (firstMove) {
                    lines.add(String.format(Locale.US, "G1 X%.2f F%d", targetX, Math.round(feed)));
                    firstMove = false;
                } else {
                    lines.add(String.format(Locale.US, "G1 X%.2f", targetX));
                }
                goingToFarX = !goingToFarX;
            }
        }

        lines.add(String.format(Locale.US, "G0 Z%.2f", safeZ));
        lines.add("M5");
        lines.add("G4 P3");
        lines.add("M2");

        if (targetFile == null) {
            try {
                file = Files.createTempFile("leveling", ".nc").toFile();
            } catch (IOException e) {
                GUIHelpers.displayErrorDialog("Error creating temp file: " + e.getLocalizedMessage());
                return "Error creating temp file.";
            }
        } else {
            file = targetFile;
        }

        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            GUIHelpers.displayErrorDialog("Error writing file: " + e.getLocalizedMessage());
            return "Error writing file.";
        }

        return String.format(Locale.US,
                "%d rows/pass, %d passes. Row Y: 0..%.2f. Pass Z: %s",
                rows.size(), depths.size(), stockY, depths.toString());
    }
}
