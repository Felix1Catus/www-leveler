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

import com.willwinder.ugs.nbp.lib.Mode;
import com.willwinder.ugs.nbp.lib.services.LocalizingService;
import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.services.LookupService;
import com.willwinder.universalgcodesender.utils.GUIHelpers;
import com.willwinder.universalgcodesender.utils.SwingHelpers;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Dockable UGS panel that drives {@link LevelingGcode}, the Java port of
 * {@code leveling_generator.py}. Every parameter from the script's
 * {@code USER PARAMETERS} and advanced blocks is exposed as an editable field;
 * the values persist via {@link LevelingPrefs}. "Generate" builds the program
 * into a temp file and loads it straight into UGS; "Export" writes it to a file
 * the user chooses and then loads it.
 */
@TopComponent.Description(
        preferredID = "LevelingTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = Mode.OUTPUT, openAtStartup = false)
@ActionID(category = LocalizingService.CATEGORY_WINDOW, id = "com.willwinder.ugs.platform.leveling.LevelingTopComponent")
@ActionReference(path = LocalizingService.MENU_WINDOW_PLUGIN)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_LevelingAction",
        preferredID = "LevelingTopComponent"
)
@Messages({
    "CTL_LevelingAction=WWW Leveler",
    "CTL_LevelingTopComponent=WWW Leveler",
    "HINT_LevelingTopComponent=Serpentine facing/leveling toolpath generator"
})
public final class LevelingTopComponent extends TopComponent {

    private static final String VERSION = "v1.0";

    private final LevelingPrefs prefs = new LevelingPrefs();
    private final LevelingGcode gcode = new LevelingGcode();
    private transient final BackendAPI backend;

    // main parameters
    private JSpinner sStockX;
    private JSpinner sStockY;
    private JSpinner sStepover;
    private JSpinner sStepdown;
    private JSpinner sFinalThickness;
    private JSpinner sNumPasses;

    // advanced parameters
    private JSpinner sFeed;
    private JSpinner sSafeZ;
    private JCheckBox cbAutoSafeZ;
    private JSpinner sSpindleSpeed;
    private JSpinner sDwell;

    private JLabel lResult;

    private boolean loading = false;

    public LevelingTopComponent() {
        setName(Bundle.CTL_LevelingTopComponent());
        setToolTipText(Bundle.HINT_LevelingTopComponent());

        backend = LookupService.lookup(BackendAPI.class);
        gcode.init(prefs, backend);

        initComponents();
        loadFromPrefs();
        prefs.updateAllPrefs(); // materialise defaults on first run
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        int row = 0;

        row = addSection(form, c, row, "Stock & passes");
        sStockX         = addDoubleRow(form, c, row++, "Stock X (width), mm",   LevelingPrefs.DEFAULT_STOCK_X, 0, 100000, 1.0, LevelingPrefs.KEY_STOCK_X);
        sStockY         = addDoubleRow(form, c, row++, "Stock Y (length), mm",  LevelingPrefs.DEFAULT_STOCK_Y, 0, 100000, 1.0, LevelingPrefs.KEY_STOCK_Y);
        sStepover       = addDoubleRow(form, c, row++, "Stepover (row Y spacing), mm", LevelingPrefs.DEFAULT_STEPOVER, 0.01, 100000, 0.5, LevelingPrefs.KEY_STEPOVER);
        sStepdown       = addDoubleRow(form, c, row++, "Stepdown (depth per pass), mm", LevelingPrefs.DEFAULT_STEPDOWN, 0, 100000, 0.1, LevelingPrefs.KEY_STEPDOWN);
        sFinalThickness = addDoubleRow(form, c, row++, "Final thickness above Z0, mm", LevelingPrefs.DEFAULT_FINAL_THICKNESS, -100000, 100000, 0.5, LevelingPrefs.KEY_FINAL_THICKNESS);
        sNumPasses      = addIntRow(form, c, row++,    "Number of passes",      LevelingPrefs.DEFAULT_NUM_PASSES, 1, 100000, 1, LevelingPrefs.KEY_NUM_PASSES);

        row = addSection(form, c, row, "Advanced");
        sFeed           = addDoubleRow(form, c, row++, "Cutting feed, mm/min",  LevelingPrefs.DEFAULT_FEED, 1, 1000000, 100, LevelingPrefs.KEY_FEED);

        // Safe-Z with auto checkbox
        cbAutoSafeZ = new JCheckBox("Auto safe-Z (final thickness + 2 mm)");
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(cbAutoSafeZ, c);
        c.gridwidth = 1;
        row++;
        sSafeZ          = addDoubleRow(form, c, row++, "Safe-Z (retract height), mm", LevelingPrefs.DEFAULT_SAFE_Z, -100000, 100000, 0.5, LevelingPrefs.KEY_SAFE_Z);
        cbAutoSafeZ.addActionListener(e -> {
            if (loading) return;
            prefs.putBoolean(LevelingPrefs.KEY_AUTO_SAFE_Z, cbAutoSafeZ.isSelected());
            refreshSafeZField();
        });

        sSpindleSpeed   = addDoubleRow(form, c, row++, "Spindle speed (RPM)",   LevelingPrefs.DEFAULT_SPINDLE_SPEED, 0, 1000000, 100, LevelingPrefs.KEY_SPINDLE_SPEED);
        sDwell          = addIntRow(form, c, row++,    "Dwell after spindle on, s", LevelingPrefs.DEFAULT_DWELL_SECONDS, 0, 100000, 1, LevelingPrefs.KEY_DWELL_SECONDS);

        // When final thickness changes and auto is on, keep safe-Z in sync.
        sFinalThickness.addChangeListener(e -> { if (!loading) refreshSafeZField(); });

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        // bottom bar: version + buttons
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));

        JLabel lVersion = new JLabel(VERSION);
        lVersion.setFont(lVersion.getFont().deriveFont(Font.PLAIN, 10f));
        lVersion.setForeground(new java.awt.Color(153, 153, 153));

        lResult = new JLabel(" ");
        lResult.setFont(lResult.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel left = new JPanel(new BorderLayout());
        left.add(lVersion, BorderLayout.NORTH);
        left.add(lResult, BorderLayout.CENTER);

        JButton bGenerate = new JButton("Generate");
        bGenerate.setToolTipText("Build the program and load it into UGS");
        bGenerate.addActionListener(e -> generate());

        JButton bExport = new JButton("Export…");
        bExport.setToolTipText("Save the program to a file, then load it into UGS");
        bExport.addActionListener(e -> export());

        JPanel buttons = new JPanel();
        buttons.add(bExport);
        buttons.add(Box.createHorizontalStrut(4));
        buttons.add(bGenerate);

        bottom.add(left, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    private int addSection(JPanel form, GridBagConstraints c, int row, String title) {
        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.insets = new Insets(12, 4, 4, 4);
        form.add(header, c);
        c.insets = new Insets(3, 4, 3, 4);
        c.gridwidth = 1;
        return row + 1;
    }

    private JSpinner addDoubleRow(JPanel form, GridBagConstraints c, int row, String label,
                                  double value, double min, double max, double step, String key) {
        JLabel l = new JLabel(label);
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        form.add(l, c);

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        setSpinnerWidth(spinner);
        c.gridx = 1; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(spinner, c);
        c.fill = GridBagConstraints.NONE;

        spinner.addChangeListener(e -> {
            if (loading) return;
            prefs.putDouble(key, ((Number) spinner.getValue()).doubleValue());
        });
        return spinner;
    }

    private JSpinner addIntRow(JPanel form, GridBagConstraints c, int row, String label,
                               int value, int min, int max, int step, String key) {
        JLabel l = new JLabel(label);
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        form.add(l, c);

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        setSpinnerWidth(spinner);
        c.gridx = 1; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(spinner, c);
        c.fill = GridBagConstraints.NONE;

        spinner.addChangeListener(e -> {
            if (loading) return;
            prefs.putInt(key, ((Number) spinner.getValue()).intValue());
        });
        return spinner;
    }

    private void setSpinnerWidth(JComponent spinner) {
        spinner.setMinimumSize(new java.awt.Dimension(120, 24));
        spinner.setPreferredSize(new java.awt.Dimension(140, 24));
    }

    private void loadFromPrefs() {
        loading = true;
        sStockX.setValue(prefs.stockX());
        sStockY.setValue(prefs.stockY());
        sStepover.setValue(prefs.stepover());
        sStepdown.setValue(prefs.stepdown());
        sFinalThickness.setValue(prefs.finalThickness());
        sNumPasses.setValue(prefs.numPasses());
        sFeed.setValue(prefs.feed());
        cbAutoSafeZ.setSelected(prefs.autoSafeZ());
        sSafeZ.setValue(prefs.rawSafeZ());
        sSpindleSpeed.setValue(prefs.spindleSpeed());
        sDwell.setValue(prefs.dwellSeconds());
        loading = false;
        refreshSafeZField();
    }

    /** Reflect the auto-safe-Z state: disable the field and show the computed value when auto. */
    private void refreshSafeZField() {
        boolean auto = cbAutoSafeZ.isSelected();
        sSafeZ.setEnabled(!auto);
        if (auto) {
            double computed = ((Number) sFinalThickness.getValue()).doubleValue() + 2.0;
            loading = true;
            sSafeZ.setValue(computed);
            loading = false;
        }
    }

    private void generate() {
        String summary = gcode.generate(null);
        if (gcode.getFile() != null) {
            GUIHelpers.openGcodeFile(gcode.getFile(), backend);
        }
        lResult.setText(summary);
    }

    private void export() {
        String sourceDir = backend.getSettings().getLastOpenedFilename();
        SwingHelpers
            .createFile(sourceDir)
            .ifPresent(file -> {
                String summary = gcode.generate(file);
                if (gcode.getFile() != null) {
                    GUIHelpers.openGcodeFile(gcode.getFile(), backend);
                }
                lResult.setText(summary);
            });
    }
}
