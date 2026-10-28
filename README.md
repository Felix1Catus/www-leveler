# WWW Leveler

A plugin for **[Universal Gcode Sender](https://universalgcodesender.com/) (UGS Platform)**
that generates a serpentine (boustrophedon) **facing / leveling** toolpath and loads it
straight into UGS — all from a dockable panel inside the application.

It is a native-Java port of the `leveling_generator.py` script (included in this repo for
reference). The generated G-code is byte-for-byte equivalent to the script for the same
inputs.

---

## What it does

Produces a serpentine raster that:

- covers the full `stock_x × stock_y` rectangle, edge to edge;
- steps down in Z each full pass (no retract between passes);
- reverses row direction each pass so it never needs a rapid move between rows;
- retracts to a safe height only at the very start and very end.

### Parameters (identical to the script)

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

All values persist between sessions. Output is always millimetres (`G21`), as in the script.

Buttons:
- **Generate** — build the program into a temp file and load it into UGS.
- **Export…** — save the program to a file you choose, then load it into UGS.

Open it from the UGS menu: **Window → Plugins → WWW Leveler**.

---

## Install (prebuilt plugin)

A prebuilt plugin file is in [`dist/`](dist/) (`WWWLeveler-<version>.nbm`). Installing it is
the same on **Windows** and **Linux** — it's done through the UGS GUI:

1. Start **UGS Platform**.
2. **Tools → Plugins**.
3. Open the **Downloaded** tab → **Add Plugins…**
4. Select `dist/WWWLeveler-<version>.nbm` → **Install** → accept the prompts → **restart UGS**.
5. Open the panel from **Window → Plugins → WWW Leveler**.

> ✅ **Compatibility.** The prebuilt `.nbm` is built against UGS **v2.1.27** and its UGS Lib
> dependency is relaxed to `> 2.0` (it does not pin a build timestamp), so it installs on the
> official downloadable **2.1.27** release and newer. It does require the NetBeans platform that
> ships with 2.1.27 (RELEASE290) — so install it on **UGS 2.1.27 or later**. On an older UGS,
> build it yourself against that version (see below) — just change the `TAG` in the build script.

---

## Build from source

The plugin depends on UGS internal modules that aren't published to Maven Central, so it is
built inside a checkout of the UGS source. The scripts below do that automatically: they
clone UGS at the right tag, drop this module in, register it, and build.

**Requirements (both OSes):** **JDK 25** and `git`. (UGS 2.1.x targets Java 25.)
Get a JDK 25 from [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=25).

### Linux / macOS

```bash
# ensure JDK 25 is active, e.g.:
export JAVA_HOME=/path/to/jdk-25
export PATH="$JAVA_HOME/bin:$PATH"

./scripts/build.sh
# -> dist/WWWLeveler-<version>.nbm
```

### Windows (PowerShell)

```powershell
# ensure JDK 25 is active, e.g.:
$env:JAVA_HOME = "C:\path\to\jdk-25"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
# -> dist\WWWLeveler-<version>.nbm
```

Then install the resulting `.nbm` using the **Install** steps above.

---

## Repository layout

```
www-leveler/
├─ plugin/                     the UGS Platform NetBeans module (source)
│  ├─ pom.xml
│  └─ src/main/...
├─ dist/                       prebuilt WWWLeveler-<version>.nbm
├─ scripts/
│  ├─ build.sh                 Linux/macOS build (clones UGS, injects module, builds)
│  └─ build.ps1                Windows build
├─ leveling_generator.py       original Python script (reference)
├─ LICENSE                     GPLv3 (inherited from UGS)
└─ README.md
```

## License

GPLv3. This plugin links against and is derived from Universal Gcode Sender, which is
licensed under the GNU GPL v3; see [`LICENSE`](LICENSE).
