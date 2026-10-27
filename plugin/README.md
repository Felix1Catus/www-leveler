# UGS Leveling Generator plugin

A Universal Gcode Sender **Platform** plugin that generates a serpentine
(boustrophedon) facing / leveling toolpath. It is a faithful Java port of
`leveling_generator.py`, with every script parameter exposed in a dockable panel
and a one-click load of the generated program into UGS.

## What it does

Produces a serpentine raster that:

- covers the full `stock_x × stock_y` rectangle, edge to edge;
- steps down in Z each full pass (no retract between passes);
- reverses row direction each pass so it never needs a rapid move between rows;
- only retracts to a safe height at the very start and very end.

The G-code output is byte-for-byte equivalent to the Python script for the same
inputs (same `G21/G90`, same serpentine order, same per-pass depth ladder,
same first-move-only feed words).

## Parameters (identical to the script)

| Panel field | Script variable |
|---|---|
| Stock X (width), mm | `stock_x` |
| Stock Y (length), mm | `stock_y` |
| Stepover (row Y spacing), mm | `stepover` |
| Stepdown (depth per pass), mm | `stepdown` |
| Final thickness above Z0, mm | `final_thickness` |
| Number of passes | `num_passes` |
| Cutting feed, mm/min | `feed` |
| Safe-Z (retract height), mm | `safe_z` |
| Auto safe-Z (final thickness + 2 mm) | the script's `safe_z = final_thickness + 2.0` override |
| Spindle speed (RPM) | `spindle_speed` |
| Dwell after spindle on, s | `dwell_seconds` |

All values persist between sessions. Output is always millimetres (`G21`), as in
the script.

## Buttons

- **Generate** — build the program into a temp file and load it into UGS (shows
  in the visualizer, ready to send).
- **Export…** — save the program to a file you choose, then load it into UGS.

Open the panel from the UGS menu: **Window → Plugins → Leveling Generator**.

## Building the .nbm

Requires **JDK 25** (the UGS 2.1.x build targets Java 25) and uses the Maven
wrapper bundled with the UGS source — no separate Maven install needed.

From the UGS source root:

```bash
# build only this plugin and its dependencies (ugs-core, ugslib)
JAVA_HOME=/path/to/jdk-25 ./mvnw install -pl ugs-platform/LevelingGenerator -am -DskipTests
```

or on Windows PowerShell:

```powershell
$env:JAVA_HOME="C:\path\to\jdk-25"; .\mvnw.cmd install -pl ugs-platform/LevelingGenerator -am -DskipTests
```

The plugin is produced at:

```
ugs-platform/LevelingGenerator/target/LevelingGenerator-<version>.nbm
```

## Installing into UGS

1. In UGS Platform: **Tools → Plugins**.
2. Open the **Downloaded** tab → **Add Plugins…**
3. Select `LevelingGenerator-<version>.nbm` → **Install**, accept, and restart UGS.
4. Open it from **Window → Plugins → Leveling Generator**.

> Build the `.nbm` against the **same UGS version you run** (this one is built
> against the `v2.1.27` tag). A module built against a very different platform
> version may refuse to load.
