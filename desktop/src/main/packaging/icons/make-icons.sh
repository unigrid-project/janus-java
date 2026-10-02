#!/bin/sh
# Rebuilds the Windows and macOS icons from the 1024 pixel picture, so that Explorer, the Start menu, the
# taskbar, the Dock and Finder each get a size drawn for them instead of one scaled down. Needs ImageMagick
# and Python with Pillow. The results are committed, so this runs only when the picture changes.
set -eu

here=$(cd "$(dirname "$0")" && pwd)
source="$here/unigrid-1024x1024.png"

convert "$source" -define icon:auto-resize=256,128,64,48,32,24,16 "$here/unigrid-256-256.ico"

python3 - "$source" "$here/unigrid-1024x1024.icns" <<'PYTHON'
import sys
from PIL import Image

picture = Image.open(sys.argv[1])
sizes = [(16, 16), (32, 32), (64, 64), (128, 128), (256, 256), (512, 512), (1024, 1024)]
picture.save(sys.argv[2], sizes=sizes)
PYTHON

echo "Rebuilt the icons in $here"
