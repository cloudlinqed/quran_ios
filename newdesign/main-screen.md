# Main Screen (Home) — New Design Specification

## Design System: "The Digital Sanctuary"

High-end editorial Islamic aesthetic with warm tones, generous spacing, and no harsh borders.

---

## Color Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `ScreenBg` | `#FDF9F1` (light) / `#1A1A1A` (dark) | Screen background — dark-mode aware |
| `GoldAccent` | `#C3A364` | App name, progress bar, decorative accents |
| `PanelGreenStart` | `#234C3E` | Glass media panel gradient start |
| `PanelGreenEnd` | `#143228` | Glass media panel gradient end |
| `CardGreen` | `#E6F2E6` | Quran card background |
| `CardGreenBorder` | `#CDE2CD` | Quran card border (not used — no-line rule) |
| `CardYellow` | `#FDF6E3` | Prayer Times card background |
| `CardYellowBorder` | `#EBE2C8` | Language pill background |
| `CardBlue` | `#EAF4FF` | Hadith card background |
| `CardBlueBorder` | `#D1E5FC` | Hadith card border (not used) |
| `CardLight` | `#F9F7EF` | Secondary cards (Rows 2-3) background |
| `CardLightBorder` | `#EBE6D6` | Secondary cards border (not used) |

## Design Rules

- **No 1px borders** — boundaries defined by background color shifts only
- **Rounded corners**: 20-24dp for panels, 14-18dp for cards/pills
- **No pure black shadows** — use tinted, low-opacity shadows
- **Generous spacing**: 12dp between sections

---

## Layout Structure

```
┌────────────────────────────────────┐
│        الْفُرْقَان     [AR/EN] [⋮] │  ← centered gold title, controls end-side
├────────────────────────────────────┤
│  ┌──── Glass Media Panel ───────┐  │  ← dark green gradient #234C3E→#143228
│  │       تلاوة                   │  │
│  │  ━━━━━━━━━━ (gold bar)       │  │  ← progress bar
│  │  04:20 /          12:45      │  │
│  │  ┌─السورة──┐ ┌──القارئ───┐   │  │  ← white pills, rounded 14dp
│  │  └─────────┘ └───────────┘   │  │
│  │  🔀  ⏮  ▶(white)  ⏭  🔁   │  │  ← gold icons, white play button
│  └──────────────────────────────┘  │
│                                    │
│  ┌─────┐ ┌─────┐ ┌─────┐         │  ← Row 1: 3 tall cards (128dp)
│  │Quran│ │Salah│ │Hadth│         │
│  │green│ │yellw│ │blue │         │
│  └─────┘ └─────┘ └─────┘         │
│                                    │
│  ┌──────────┐ ┌──────────┐        │  ← Row 2: 2 wide cards (56dp)
│  │ Tracker ☑│ │ Athkar ☆ │        │
│  └──────────┘ └──────────┘        │
│                                    │
│  ┌──────────┐ ┌──────────┐        │  ← Row 3: 2 wide cards (56dp)
│  │Downloads⬇│ │Bookmarks♥│        │
│  └──────────┘ └──────────┘        │
│                                    │
│              v2.3.5                │
└────────────────────────────────────┘
```

---

## Header

- **No top bar / scaffold app bar** — custom Box with statusBarsPadding
- App name: `"الْفُرْقَان"` (Arabic) / `"AlFurqan"` (English)
  - Font: scheherazadeFont (Arabic) / system (English)
  - Size: 30sp (Arabic) / 24sp (English)
  - Color: `#C3A364` (gold)
  - Position: **centered** — spans full width with `textAlign = Center`
  - Weight: **Bold**
- Language pill + 3-dot menu: overlaid on top of the centered name, pushed to end side
- Language pill: `"AR / EN"` on `CardYellow` (#FDF6E3) background, rounded 20dp
- No overflow menu — all navigation moved to bottom bar "More" tab

```kotlin
// Centered app name (full width, behind controls)
Text(
    text = if (isArabic) "الْفُرْقَان" else "AlFurqan",
    fontFamily = if (isArabic) scheherazadeFont else null,
    fontSize = if (isArabic) 30.sp else 24.sp,
    fontWeight = FontWeight.Bold,
    color = GoldAccent,  // #C3A364
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth()
)
// Language pill overlaid via a second Row on top (no overflow menu)
Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
    Spacer(weight 1f)
    LanguagePill  // AR / EN toggle only
}
```

---

## Glass Media Panel ("تلاوة")

Dark green glass-effect card with media controls.

```kotlin
// Gradient background
Brush.linearGradient(
    colors = listOf(
        Color(0xFF234C3E).copy(alpha = 0.95f),  // PanelGreenStart
        Color(0xFF143228).copy(alpha = 0.95f)    // PanelGreenEnd
    )
)
```

### Components:
- **"تلاوة" heading**: 20sp bold, color `#E8F0E8`, centered
- **Progress bar**: 3dp height, track `white@15%`, fill `#C3A364` (gold)
- **Timestamps**: 10sp, left = `white@50%`, right = gold
- **Surah/Reciter pills**: white Surface, rounded 14dp, side-by-side
  - Each pill: dropdown arrow (gray) + label (9sp gray) + value (13sp bold black)
- **Playback controls** (LTR layout forced):
  - Repeat + SkipPrevious + **Play** (white circle 52dp, green icon) + SkipNext + Shuffle
  - Skip icons: gold `#C3A364`
  - Repeat/Shuffle: gold at 70% alpha

---

## Feature Cards Grid

### Row 1: 3 Primary Cards (tall, square)

| Card | Background | Icon | Icon Color |
|------|-----------|------|-----------|
| القرآن الكريم | `#E6F2E6` | `MenuBook` | `#234C3E` |
| مواقيت الصلاة | `#FDF6E3` | `Schedule` | `#234C3E` | No subtitle |
| الأحاديث | `#EAF4FF` | `HistoryEdu` | `#4A729A` |

```kotlin
// PrimaryCard structure
Surface(height = 128.dp, shape = RoundedCornerShape(20.dp), color = cardColor) {
    Column(center) {
        Spacer(weight 0.2)
        Icon(32.dp)          // centered
        Spacer(weight 0.3)
        Title(14sp bold)     // centered
        Subtitle(9sp gray)   // optional
        Spacer(weight 0.1)
    }
}
```

- Prayer Times card has **no subtitle** — same height as other cards, icon + title only
- **RTL order**: Quran first → appears rightmost in Arabic

### Rows 2-3: Secondary Cards (wide, horizontal)

| Card | Icon | Icon Position |
|------|------|--------------|
| المتابعة اليومية | `CheckBox` | end |
| الأذكار | `StarOutline` | end |
| التنزيلات | `Download` | end |
| المفضلة | `FavoriteBorder` | end |

```kotlin
// SecondaryCard structure
Surface(height = 56.dp, shape = RoundedCornerShape(14.dp), color = CardLight) {
    Row(SpaceBetween, padding 14dp) {
        Text(title, 14sp bold)   // weight 1
        Icon(20dp, BrandGreen)   // end side
    }
}
```

- All use `CardLight` (#F9F7EF) background
- Icon color: `#234C3E` (PanelGreenStart / BrandGreen)
- Height: 56dp
- Corner radius: 14dp

---

## Typography

| Element | Size | Weight | Font |
|---------|------|--------|------|
| App name (AR) | 30sp | Bold | Scheherazade |
| App name (EN) | 24sp | Bold | System |
| Panel heading | 20sp | Bold | Scheherazade |
| Card title (AR) | 14sp | Bold | Scheherazade |
| Card title (EN) | 12sp | Bold | System |
| Card subtitle | 9sp | Normal | System |
| Selector label | 9sp | Normal | System |
| Selector value | 13sp | Bold | System/Scheherazade |
| Version | 11sp | Normal | System |

---

## RTL/LTR Behavior

- Whole screen wrapped in `CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection())`
- Header: app name **centered** (same position in both AR and EN), language pill + menu on end side
- Feature grid Row 1: Quran card first → rightmost in Arabic
- Media controls: forced LTR via `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)`
- Skip button actions swap for RTL (left = next in Arabic)
