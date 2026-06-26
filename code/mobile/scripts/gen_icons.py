"""Generate simple tab bar icons for uni-app mobile.

Creates 48x48 PNG icons with simple white symbols on colored circles.
8 files: tab-{home,todo,approval,mine}{,-active}.png
"""
import struct, zlib, os, math

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "src", "static")

COLORS = {
    "home":    {"active": (0x40, 0x9E, 0xFF), "inactive": (0xC0, 0xC4, 0xCC)},
    "todo":    {"active": (0x67, 0xC2, 0x3A), "inactive": (0xC0, 0xC4, 0xCC)},
    "approval":{"active": (0xE6, 0xA2, 0x3C), "inactive": (0xC0, 0xC4, 0xCC)},
    "mine":    {"active": (0x40, 0x9E, 0xFF), "inactive": (0xC0, 0xC4, 0xCC)},
}

def chunk(ctype, data):
    c = ctype + data
    crc = struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)
    return struct.pack(">I", len(data)) + c + crc

def make_png(w, h, pixels):
    """pixels: list of (R,G,B,A) rows, each row has w items"""
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter None
        for r, g, b, a in row:
            raw += struct.pack("BBBB", r, g, b, a)
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    idat = chunk(b"IDAT", zlib.compress(bytes(raw)))
    iend = chunk(b"IEND", b"")
    return sig + ihdr + idat + iend

def circle_icon(w, h, fg_color, bg_color):
    """Draw a filled circle on a transparent background."""
    cx, cy = w // 2, h // 2
    radius = min(cx, cy) - 2
    rows = []
    for y in range(h):
        row = []
        for x in range(w):
            dx, dy = x - cx, y - cy
            dist = math.sqrt(dx*dx + dy*dy)
            if dist <= radius:
                row.append(bg_color + (255,))
            else:
                row.append((0, 0, 0, 0))
        rows.append(row)
    return rows

def draw_symbol(rows, w, h, symbol, color):
    """Draw white symbol on existing pixel rows."""
    # symbol: list of (x,y) normalized 0..1
    cx, cy = w // 2, h // 2
    scale = min(w, h) * 0.3
    for nx, ny in symbol:
        px = int(cx + nx * scale)
        py = int(cy + ny * scale)
        if 0 <= px < w and 0 <= py < h:
            rows[py][px] = color + (255,)

# === Symbol definitions (normalized -1..1 coordinates) ===

def symbol_home(w, h):
    pts = []
    # Simple house: triangle roof + rectangle body
    cx, cy = w // 2, h // 2
    # Draw filled pixels forming a house shape
    for y in range(h):
        for x in range(w):
            nx = (x - cx) / (w * 0.35)
            ny = (y - cy) / (h * 0.35)
            # Roof: triangle (point at top center, base at mid)
            if -0.8 <= nx <= 0.8 and -0.8 <= ny <= 0.6:
                # Roof area (upper half)
                roof_half = 0.8
                if ny <= -0.1:
                    # Triangle: |nx| < (1 - (ny+0.8)/0.7) * 0.8
                    t = (ny + 0.8) / 0.9
                    if abs(nx) < 0.8 * (1 - t):
                        pts.append((x, y))
                elif ny > -0.1 and ny <= 0.6 and abs(nx) < 0.6:
                    pts.append((x, y))
    return pts

def symbol_check(w, h):
    pts = []
    cx, cy = w // 2, h // 2
    for y in range(h):
        for x in range(w):
            nx = (x - cx) / (w * 0.4)
            ny = (y - cy) / (h * 0.4)
            # Checkmark: two line segments forming a check
            # Left arm: from (-0.5, 0.1) to (-0.1, 0.5)
            # Right arm: from (-0.1, 0.5) to (0.6, -0.4)
            d1 = abs((nx + 0.1) * 0.5 - (ny - 0.1) * 0.5) / math.sqrt(2)
            d2 = abs((nx - 0.6) * 0.5 - (ny + 0.4) * 0.5) / math.sqrt(2)
            if d1 < 0.12 and -0.55 <= nx <= -0.05 and -0.15 <= ny <= 0.55:
                pts.append((x, y))
            elif d2 < 0.12 and -0.05 <= nx <= 0.65 and -0.45 <= ny <= 0.55:
                pts.append((x, y))
    return pts if pts else [(cx + int(w*0.1), cy) for _ in range(5)]

def symbol_doc(w, h):
    pts = set()
    cx, cy = w // 2, h // 2
    for y in range(h):
        for x in range(w):
            nx = (x - cx) / (w * 0.35)
            ny = (y - cy) / (h * 0.35)
            # Document: rectangle with lines
            if -0.7 <= nx <= 0.7 and -0.8 <= ny <= 0.8:
                pts.add((x, y))
    return list(pts)

def symbol_person(w, h):
    pts = set()
    cx, cy = w // 2, h // 2
    for y in range(h):
        for x in range(w):
            nx = (x - cx) / (w * 0.35)
            ny = (y - cy) / (h * 0.35)
            # Head: small circle at top
            head_dist = math.sqrt(nx*nx + (ny+0.4)*(ny+0.4))
            if head_dist < 0.3:
                pts.add((x, y))
            # Body: triangle/trapezoid at bottom
            if ny >= -0.1 and ny <= 0.7:
                width = 0.5 * (1 - (ny + 0.1) / 0.8)
                if abs(nx) < width:
                    pts.add((x, y))
    return list(pts)

SYMBOLS = {
    "home": symbol_home,
    "todo": symbol_check,
    "approval": symbol_doc,
    "mine": symbol_person,
}

def create_icon(name, active):
    bg = COLORS[name]["active" if active else "inactive"]
    w, h = 48, 48
    rows = circle_icon(w, h, (255,)*3, bg)
    sym_func = SYMBOLS[name]
    pts = sym_func(w, h)
    white = (255, 255, 255)
    for x, y in pts:
        if 0 <= y < h and 0 <= x < w:
            r, g, b, a = rows[y][x]
            if a > 0:  # Only draw on non-transparent pixels
                rows[y][x] = white + (255,)
    return make_png(w, h, rows)

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name in ["home", "todo", "approval", "mine"]:
        for active, suffix in [(False, ""), (True, "-active")]:
            data = create_icon(name, active)
            path = os.path.join(OUT_DIR, f"tab-{name}{suffix}.png")
            with open(path, "wb") as f:
                f.write(data)
            print(f"  Created: {path} ({len(data)} bytes)")
    print(f"\nDone! {8} icons in {OUT_DIR}")

if __name__ == "__main__":
    main()
