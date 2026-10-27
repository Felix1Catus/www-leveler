#!/usr/bin/env python3
"""
CNC Leveling (Facing) Toolpath Generator
-----------------------------------------
Edit the parameters in the block below, then run:
    python3 leveling_generator.py

Produces a serpentine raster that:
  - covers the full stock_x x stock_y rectangle, edge to edge
  - steps down in Z each full pass (no retract between passes -
    it drives straight from the last row of one pass into the Z change,
    then into the first row of the next pass)
  - reverses row direction each pass so it never needs a rapid move
  - only retracts to a safe height at the very start and very end
"""

# ==================== USER PARAMETERS ====================
stock_x   = 590.0   # stock width  (X axis), mm
stock_y   = 140.0   # stock length (Y axis), mm
stepover  = 10.0    # row spacing in Y, mm
stepdown  = 1    # depth added per pass, mm
final_thickness = 10   # desired final board thickness above the spoilboard (Z0), mm
num_passes      = 1      # how many passes to take
# ===========================================================

# --- advanced / rarely-changed settings ---
feed          = 3000     # cutting feed, mm/min
safe_z        = 2.0      # retract height above the surface (Z0) at start/end, mm
spindle_speed = 30000
dwell_seconds = 2
output_file   = "leveling.nc"
# -------------------------------------------


def build_rows(length, step):
    """Row positions from 0 to length, evenly stepped, with a final
    row forced onto the far edge if the step doesn't divide evenly."""
    rows = [0.0]
    y = 0.0
    while y < length:
        y += step
        rows.append(min(y, length))
    if len(rows) > 1 and abs(rows[-1] - rows[-2]) < 1e-9:
        rows.pop()
    return rows


def build_depths(final_thickness, step, num_passes):
    """Pass depths counting down to final_thickness, step mm apart.
    First pass is the highest (roughest), last pass lands exactly
    on final_thickness."""
    return [final_thickness + (num_passes - 1 - i) * step
            for i in range(num_passes)]


def generate(stock_x, stock_y, stepover, stepdown, final_thickness, num_passes,
             feed, safe_z, spindle_speed, dwell_seconds, output_file):

    rows = build_rows(stock_y, stepover)
    depths = build_depths(final_thickness, stepdown, num_passes)
    

    lines = []
    lines.append("G21")
    lines.append("G90")
    lines.append(f"G0 X0.00 Y0.00 Z{safe_z:.2f}")
    lines.append(f"S{spindle_speed}")
    lines.append("M3")
    lines.append(f"G4 P{dwell_seconds}")

    going_to_far_x = True   # next X move targets stock_x; flips each move
    first_move = True       # only the very first G1 gets an explicit F

    for pass_num, z in enumerate(depths):
        lines.append(f"; pass {pass_num + 1} of {len(depths)}  Z={z:.4f}")

        # plunge/step to this pass's depth (rapid straight down on the
        # very first pass since it's a shallow skim; feed-controlled sink
        # on every later pass since the tool is already in the material)
        if pass_num == 0:
            lines.append(f"G1 Z{z:.4f} F{feed}")
        else:
            lines.append(f"G1 Z{z:.4f}")

        row_sequence = rows if pass_num % 2 == 0 else list(reversed(rows))

        for i, y in enumerate(row_sequence):
            if i > 0:
                lines.append(f"G1 Y{y:.2f}")
            target_x = stock_x if going_to_far_x else 0.0
            if first_move:
                lines.append(f"G1 X{target_x:.2f} F{feed}")
                first_move = False
            else:
                lines.append(f"G1 X{target_x:.2f}")
            going_to_far_x = not going_to_far_x

    lines.append(f"G0 Z{safe_z:.2f}")
    lines.append("M5")
    lines.append("G4 P3")
    lines.append("M2")

    with open(output_file, "w") as f:
        f.write("\n".join(lines) + "\n")

    print(f"Wrote {output_file}")
    print(f"{len(rows)} rows/pass, {len(depths)} passes")
    print(f"row Y values: {rows}")
    print(f"pass Z values: {[round(d, 4) for d in depths]}")


safe_z = final_thickness + 2.0

if __name__ == "__main__":
    generate(stock_x, stock_y, stepover, stepdown, final_thickness, num_passes,
              feed, safe_z, spindle_speed, dwell_seconds, output_file)
