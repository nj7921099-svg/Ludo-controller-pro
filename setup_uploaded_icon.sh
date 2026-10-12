#!/bin/bash
set -e

# Find the uploaded image
SRC=""
if [ -f "IMG_20261010_182507.png" ]; then
    SRC="IMG_20261010_182507.png"
elif [ -f "app/src/main/res/drawable/IMG_20261010_182507.png" ]; then
    SRC="app/src/main/res/drawable/IMG_20261010_182507.png"
elif [ -f "app/src/main/res/drawable/ic_neon_dice_pad.png" ]; then
    SRC="app/src/main/res/drawable/ic_neon_dice_pad.png"
fi

if [ -z "$SRC" ]; then
    echo "Source image not found"
    exit 1
fi

echo "Using source image: $SRC"

# Copy as primary master drawable
cp "$SRC" "app/src/main/res/drawable/ic_neon_dice_pad.png"
cp "$SRC" "app/src/main/res/drawable/ic_launcher_artwork.png"

# Standard ImageMagick scaling for all Android mipmap densities
BASE="app/src/main/res"
for spec in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  dir="$BASE/mipmap-${spec%:*}"
  size="${spec#*:}"
  rm -f "$dir/ic_launcher.webp" "$dir/ic_launcher_round.webp"
  convert "$SRC" -resize "${size}x${size}!" "PNG32:$dir/ic_launcher.png"
  radius=$((size / 2))
  convert "$SRC" -resize "${size}x${size}!" \
    \( -size "${size}x${size}" xc:none -fill white \
       -draw "circle $radius,$radius $radius,0" \) \
    -alpha set -compose DstIn -composite "PNG32:$dir/ic_launcher_round.png"
done

echo "Successfully generated all launcher icon densities from original artwork."
