"""
Generates a 512x512 Play Store icon PNG for SwipeDel.
Requires: pip install Pillow
Run: python generate_icon.py
Output: playstore-icon.png
"""
import math
from PIL import Image, ImageDraw

SIZE = 512
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Background
draw.rectangle([0, 0, SIZE, SIZE], fill="#1A1A1A")

# Scale: viewport 24x24 -> 512x512, with 15% padding
PADDING = SIZE * 0.18
scale = (SIZE - 2 * PADDING) / 24

def vx(x): return PADDING + x * scale
def vy(y): return PADDING + y * scale

stroke = scale * 1.8
WHITE = "#FFFFFF"

# Circle centered at (16, 12) radius 4
cx, cy, r = vx(16), vy(12), 4 * scale
draw.ellipse([cx - r, cy - r, cx + r, cy + r],
             outline=WHITE, width=int(stroke))

# Arrow shaft: (12,12) -> (4,12)
draw.line([vx(12), vy(12), vx(4), vy(12)],
          fill=WHITE, width=int(stroke))

# Arrow head
pts = [(vx(7), vy(15)), (vx(4), vy(12)), (vx(7), vy(9))]
draw.line(pts, fill=WHITE, width=int(stroke))

# Round the line joins manually with circles
cap_r = stroke / 2
for pt in [(vx(12), vy(12)), (vx(4), vy(12)), (vx(7), vy(15)), (vx(7), vy(9))]:
    draw.ellipse([pt[0]-cap_r, pt[1]-cap_r, pt[0]+cap_r, pt[1]+cap_r], fill=WHITE)

img.save("playstore-icon.png")
print("Saved: playstore-icon.png")
