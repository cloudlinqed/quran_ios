# SVG Mushaf Composable - iOS/KMP Implementation Guide

## Overview

The SVG Mushaf renders each of the 604 Quran pages as a full-screen SVG vector graphic. The Android implementation lives in `SVGPageComposable.kt` (545 lines). This document captures every technical decision so the same behavior can be replicated in KMP/iOS without trial and error.

Key problems solved:
1. **Full-screen fit without overflow** in both portrait and landscape
2. **Reliable long-press ayah detection** on an SVG image (no DOM, no text layout)
3. **Per-word ayah highlighting** that aligns with the rendered SVG text positions

---

## Architecture

```
QuranReaderScreen (HorizontalPager, RTL)
  |
  |-- per page: load SVG string + QCFPageData JSON
  |
  +-- SVGPageComposable
        |
        |-- BoxWithConstraints  (measures available pixels)
        |-- LaunchedEffect      (renders SVG to bitmap on Dispatchers.Default)
        |     |-- recolorSvg()         swap 3 fill colors for theming
        |     |-- SVG.getFromString()  parse SVG
        |     |-- detectContentBounds() pixel-scan to find text bounding box
        |     +-- Canvas draw          scale + translate + clip + draw
        |
        |-- Image(bitmap)       display the rendered bitmap
        +-- SvgAyahOverlay      transparent Canvas layer for gestures + highlights
```

---

## Data Models

### QCFPageData (per-page JSON)

Each page has a companion JSON file: `qcf-pages/page-001.json` through `page-604.json`.

```
QCFPageData
  page: Int                          // 1-604
  lines: [QCFLineData]

QCFLineData
  line: Int?                         // 1-15 (line slot number)
  type: QCFLineType                  // TEXT, BASMALA, SURAH_HEADER
  text: String?
  surah: String?
  words: [QCFWordData]?

QCFWordData
  location: String                   // "surah:verse:wordIndex" e.g. "2:255:1"
  word: String                       // Arabic Unicode text
  qpcV2: String                      // glyph code (V2 font)
  qpcV1: String                      // glyph code (V1 font)
  isEnd: Boolean?                    // true = last word of verse (ayah number marker)
```

**Key constant**: Every Quran page has exactly **15 line slots**. Some slots may be `SURAH_HEADER` or `BASMALA` instead of `TEXT`.

### RenderedSvgPage (render output)

After rendering, these values are stored and used by the overlay:

```
bitmap: Bitmap                       // final rendered bitmap
outputHeightPx: Int                  // bitmap height (may exceed screen in landscape)
scale: Float                         // scale factor applied to SVG picture
offsetX: Float                       // horizontal translation applied
offsetY: Float                       // vertical translation applied
picH: Int                            // original SVG picture height (pre-scale)
contentLeft: Int                     // detected content left edge (picture coords)
contentRight: Int                    // detected content right edge (picture coords)
contentTop: Int                      // detected content top edge (picture coords)
contentBottom: Int                   // detected content bottom edge (picture coords)
```

---

## Step 1: SVG Recoloring

Before parsing, do string replacement on the raw SVG markup. The SVGs contain 3 hardcoded fill colors:

| Original Color | What It Is | Replace With |
|---------------|-----------|-------------|
| `#231f20` | Quran text + ayah number digits | theme `textPrimary` |
| `#bfe8c1` | Ornament circles (verse markers) | blended ornament color |
| `#ffffff` | Page background | theme `background` |

**Match all case/spacing variants**: `fill:#231f20`, `fill: #231f20`, `fill:#231F20`, etc.

### Ornament Blending Formula

Ornaments are blended with the background so they stay lighter than text:

- **Pages 3-604** (normal): `0.4 * accent + 0.6 * background`
- **Pages 1-2** (muted, heavy ornamentation): `0.2 * accent + 0.8 * background`

Per-channel: `result.r = accent.r * ratio + background.r * (1 - ratio)`

---

## Step 2: Content Bounds Detection

**Problem**: SVG pages have varying amounts of whitespace/margins. To fill the screen we need to find where the actual text content sits.

**Solution**: Render the SVG to a probe bitmap, then pixel-scan to find the bounding box of non-background pixels.

### Algorithm

1. Render SVG to a temporary bitmap at native SVG dimensions (`picW x picH`)
2. Read all pixels into a flat array
3. For each pixel, check if it's "foreground":
   - `alpha > 16` (FOREGROUND_ALPHA_THRESHOLD)
   - AND color is NOT within tolerance of the background color (BG_COLOR_TOLERANCE = 10 per RGB channel)
4. Count foreground pixels per column (`foregroundByColumn[x]`) and per row (`foregroundByRow[y]`)

### Horizontal Bounds (Left/Right)

Two-pass approach to handle pages with sparse decorations:

1. **Strict pass**: column must have >= `8%` of page height in foreground pixels (`CONTENT_COLUMN_COVERAGE_RATIO = 0.08f`)
   - Find leftmost and rightmost columns meeting this threshold
   - Accept only if detected width >= 60% of page width (`MIN_CONTENT_WIDTH_RATIO = 0.6f`)
2. **Relaxed fallback**: if strict fails, use `2%` threshold (`CONTENT_COLUMN_COVERAGE_RELAXED_RATIO = 0.02f`)
3. **Final fallback**: full page width `(0, picW-1)`
4. Add 2px padding on each side (`CONTENT_SIDE_PADDING_PX = 2`)

### Vertical Bounds (Top/Bottom)

- Row threshold: `max(2, picW * 0.02)` foreground pixels
- `top` = first row meeting threshold
- `bottom` = last row meeting threshold

### Color Comparison

```
isNearColor(color, target, tolerance=10):
  abs(color.r - target.r) <= 10
  AND abs(color.g - target.g) <= 10
  AND abs(color.b - target.b) <= 10
```

---

## Step 3: Scaling and Positioning (The Core of Full-Screen Fit)

This is the critical logic that prevents overflow while maximizing screen usage.

### Scale Calculation

```
contentW = contentBounds.right - contentBounds.left + 1   // detected text width
scale = containerW / contentW                               // fit width exactly
scaledH = picH * scale                                      // proportional height
```

The scale is based on **detected content width**, not the full SVG width. This means the text always fills the screen edge-to-edge horizontally, cropping only empty margins.

### Bitmap Size

| Mode | Bitmap Width | Bitmap Height |
|------|-------------|--------------|
| **Portrait** | containerW | containerH (always = screen height) |
| **Landscape** | containerW | max(containerH, scaledH) |

Portrait **always** fits the screen exactly. Landscape may produce a taller bitmap if the scaled content exceeds screen height.

### Translation (Offset)

```
offsetX = -contentBounds.left * scale
```

This shifts the content so its left edge aligns with pixel 0 of the output bitmap.

```
offsetY:
  if landscape AND scaledH > containerH:
    0                                  // top-align (enables scrolling)
  else:
    (bitmapH - scaledH) / 2           // vertically center
```

### Canvas Drawing Sequence

```
canvas.fill(backgroundColor)
canvas.save()
canvas.translate(offsetX, offsetY)
canvas.scale(scale, scale)
canvas.clipRect(0, 0, picW, picH)     // prevent any bleed
canvas.drawPicture(svgPicture)
canvas.restore()
```

---

## Step 4: Display Layout (Portrait vs Landscape)

### Portrait

```
Box(fillMaxSize) {
    Image(bitmap, fillMaxSize, contentScale = FillBounds)
    SvgAyahOverlay(fillMaxSize)
}
```

- Bitmap is exactly screen-sized, so `FillBounds` = pixel-perfect, no scaling by the Image composable
- No scrolling needed

### Landscape

```
if (outputHeightPx > screenHeightPx) {
    // Content taller than screen: enable vertical scroll
    Box(fillMaxSize, verticalScroll) {
        Box(fillMaxWidth, height = outputHeightPx) {
            Image(bitmap, fillMaxSize, contentScale = FillBounds)
            SvgAyahOverlay(fillMaxSize)
        }
    }
} else {
    // Content fits: same as portrait
    Box(fillMaxSize) { ... }
}
```

### Display Cutout (Notch) Handling

Only in landscape: apply `windowInsetsPadding(WindowInsets.displayCutout)` to the `BoxWithConstraints`. This reduces the available width so the content doesn't render under the notch.

Portrait does not need this because the notch is at the top/bottom, not overlapping the page content.

---

## Step 5: Long-Press Ayah Detection

The SVG is a flat bitmap with no text layout information. Ayah detection works by mapping tap coordinates back to the 15-line grid using the page data JSON.

### Coordinate Transformation

Screen tap `(localX, localY)` to SVG picture coordinates:

```
pictureX = (localX - offsetX) / scale
pictureY = (localY - offsetY) / scale
```

### Line Slot Detection

```
contentTopPic = contentBounds.top                    // in picture coords
contentHeightPic = contentBounds.bottom - contentBounds.top
lineSlotH = contentHeightPic / 15                    // 15 fixed line slots

lineSlot = floor((pictureY - contentTopPic) / lineSlotH) + 1
```

`lineSlot` is 1-indexed (matches `QCFLineData.line`).

### Word Detection (RTL-Aware)

Once the line is found, determine which word was tapped:

```
// Find the TEXT line matching this slot
textLine = pageData.lines.find { it.line == lineSlot AND it.type == TEXT }
words = textLine.words

// Horizontal position within content region
contentWidth = contentRight - contentLeft + 1
relativeX = (pictureX - contentLeft) / contentWidth   // 0.0 (left) to 1.0 (right)
relativeX = clamp(relativeX, 0, 1)

// RTL: right side = first word (index 0), left side = last word
wordIndex = floor((1.0 - relativeX) * words.size)
wordIndex = clamp(wordIndex, 0, words.lastIndex)

tappedWord = words[wordIndex]
surah = tappedWord.location.split(":")[0]    // e.g. "2"
verse = tappedWord.location.split(":")[1]    // e.g. "255"
```

### Gesture Handling

```
detectTapGestures(
    onTap = { toggleControls() },
    onLongPress = { offset ->
        hapticFeedback(LongPress)
        // ... coordinate transform + word detection ...
        onAyahLongPress(ayah, globalOffset)
    }
)
```

The `globalOffset` (screen-space position of the tap) is passed so the caller can position a context menu near the tap point.

**Global offset calculation**: `overlayPositionInRoot + localOffset`
(The overlay tracks its root position via `onGloballyPositioned { positionInRoot() }`)

---

## Step 6: Ayah Highlighting

When an ayah is highlighted (during playback or after long-press selection), draw semi-transparent rounded rectangles over the matching words.

### Per-Line Highlight Calculation

For each `TEXT` line in pageData:

```
// Find words belonging to the highlighted ayah
firstIdx = words.indexOfFirst { surah matches AND verse matches }
lastIdx  = words.indexOfLast  { surah matches AND verse matches }
if firstIdx < 0: skip this line

// Vertical bounds (bitmap coordinates)
lineTopPic    = contentTop + (lineNum - 1) * lineSlotH
lineBottomPic = contentTop + lineNum * lineSlotH
lineTopBm     = lineTopPic * scale + offsetY
lineBottomBm  = lineBottomPic * scale + offsetY

// Horizontal bounds (bitmap coordinates, RTL)
contentLeftBm  = contentLeft * scale + offsetX
contentRightBm = (contentRight + 1) * scale + offsetX
bitmapContentW = contentRightBm - contentLeftBm
totalWords     = words.size

highlightLeft  = contentRightBm - ((lastIdx + 1) / totalWords) * bitmapContentW
highlightRight = contentRightBm - (firstIdx / totalWords) * bitmapContentW
```

### Drawing

```
drawRoundRect(
    color = highlightColor.copy(alpha = 0.3),
    topLeft = (highlightLeft, lineTopBm),
    size = (highlightRight - highlightLeft, lineBottomBm - lineTopBm),
    cornerRadius = 4.dp
)
```

### RTL Layout Rule

Words are indexed right-to-left:
- Word 0 is at the **right** edge of the content area
- Word N-1 is at the **left** edge
- Formula: `x = contentRightBm - (wordIndex / totalWords) * bitmapContentW`

This means `firstIdx` (smaller index) maps to the **right** side, and `lastIdx` (larger index) maps to the **left** side. The highlight rect goes from `highlightLeft` to `highlightRight`.

---

## Constants Reference

| Constant | Value | Purpose |
|----------|-------|---------|
| `TOTAL_LINE_SLOTS` | 15 | Fixed lines per Mushaf page |
| `FOREGROUND_ALPHA_THRESHOLD` | 16 | Min alpha to count as content pixel |
| `BG_COLOR_TOLERANCE` | 10 | RGB channel tolerance for background match |
| `CONTENT_COLUMN_COVERAGE_RATIO` | 0.08 | Strict: 8% column fill to count as content |
| `CONTENT_COLUMN_COVERAGE_RELAXED_RATIO` | 0.02 | Relaxed: 2% column fill |
| `MIN_CONTENT_WIDTH_RATIO` | 0.6 | Min detected width = 60% of page |
| `CONTENT_SIDE_PADDING_PX` | 2 | Padding around detected bounds |

---

## Asset Structure

```
assets/
  quran-svg/
    001.svg ... 604.svg              // SVG page files
  qcf-pages/
    page-001.json ... page-604.json  // per-page line/word data
```

SVG files can be bundled or downloaded. The loader checks downloaded files first, then falls back to bundled assets. Availability check: `>= 600` SVG files present.

---

## Rendering Mode Selection

The reader supports 3 font modes. SVG is used when:

```
useSVGMode = useQCFFont AND (NOT tajweedMode) AND svgIsAvailable
```

| Condition | Mode |
|-----------|------|
| `useQCFFont` ON, Tajweed OFF, SVGs available | **SVG Mushaf** (this doc) |
| `useQCFFont` ON, Tajweed ON, V4 fonts available | QCF Tajweed (per-page COLRv1 fonts) |
| Otherwise | Plain text (KFGQPC Unicode font) |

---

## iOS/KMP Implementation Notes

### SVG Rendering
- Android uses `com.caverock.androidsvg` (SVG.getFromString, renderToPicture). On iOS, use **SVGKit** (`SVGKImage`) or **SwiftSVG**, or render via `WebKit` into a `UIImage`.
- The recoloring is plain string replacement on the SVG markup before parsing, so it works identically on any platform.

### Content Bounds Detection
- The pixel-scanning approach is platform-agnostic. On iOS, render the SVG to a `CGContext` bitmap, read pixels with `CGBitmapContext` / `cgImage.dataProvider`, and run the same column/row analysis.
- Consider caching detected bounds per page (they only change when the theme changes).

### Scaling and Canvas
- Use `CGAffineTransform` for translate + scale on iOS Core Graphics.
- The same `scale = containerWidth / contentWidth` formula applies.
- For SwiftUI: render to `UIImage`, display in an `Image` view with `.resizable().scaledToFill()` inside a `GeometryReader`.

### Landscape Scrolling
- Wrap in a `ScrollView(.vertical)` when `outputHeight > screenHeight`.
- Use `GeometryReader` to detect available space (equivalent of `BoxWithConstraints`).
- Apply `.ignoresSafeArea(.container, edges: .horizontal)` or handle display cutout with `safeAreaInsets`.

### Long-Press Detection
- Use `.onLongPressGesture(minimumDuration:)` in SwiftUI or `UILongPressGestureRecognizer` in UIKit.
- The coordinate transform math is identical: `pictureCoord = (screenCoord - offset) / scale`.
- Trigger haptic feedback via `UIImpactFeedbackGenerator(style: .medium)`.

### Highlighting
- Use SwiftUI `Canvas` or UIKit `CAShapeLayer` for the overlay.
- Draw rounded rects with `UIBezierPath(roundedRect:cornerRadius:)` or `Path.addRoundedRect()`.
- Alpha: 0.3 on the highlight color.

### Performance Tips
- Render SVG to bitmap on a background queue (equivalent of `Dispatchers.Default`).
- Cache rendered bitmaps (LRU, ~5-10 pages around current page).
- The probe bitmap for content detection can be at reduced resolution (e.g., 50% scale) to save memory, as long as thresholds are adjusted proportionally.
- Recycle/release bitmaps when pages scroll out of the visible window.
