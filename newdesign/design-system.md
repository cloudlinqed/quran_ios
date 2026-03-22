# Alfurqan Design System — "The Digital Sanctuary"

Cross-platform design system for Android (Jetpack Compose) and iOS (SwiftUI) implementations.

---

## Creative Direction

A high-end editorial Islamic experience. Serene, spacious, intentional. The interface should feel like a curated publication — not a utility app grid.

**Key principles:**
- No 1px solid borders — use background color shifts for separation
- No pure black shadows — use tinted, low-opacity shadows (4-8%)
- Generous white space between sections (12-16dp)
- Surface layering: stack color tiers instead of using elevation
- Rounded corners everywhere (14-24dp)

---

## Color Tokens

### Core Palette

| Token | Hex | RGB | Usage |
|-------|-----|-----|-------|
| `screenBg` | `#FDF9F1` | 253,249,241 | Primary screen background |
| `screenBgAlt` | `#F9F6EE` | 249,246,238 | Alternative background (prayer times) |
| `goldAccent` | `#CDAD70` | 205,173,112 | App name, TopAppBar text, section headers |
| `brandGold` | `#CDAD70` | 205,173,112 | Unified gold (from Hadith design) |
| `panelGreenStart` | `#234C3E` | 35,76,62 | Dark glass panel gradient start |
| `panelGreenEnd` | `#143228` | 20,50,40 | Dark glass panel gradient end |
| `brandGreen` | `#2B4234` | 43,66,52 | Primary dark green for cards/bars |
| `brandGreenDark` | `#275239` | 39,82,57 | Top bars app-wide (from Hadith design) |
| `brandGreenLight` | `#3A5A46` | 58,90,70 | Countdown box, lighter green |

### Card Backgrounds

| Token | Hex | Usage |
|-------|-----|-------|
| `cardGreen` | `#E6F2E6` | Quran card |
| `cardYellow` | `#FDF6E3` | Prayer Times card, language pill |
| `cardBlue` | `#EAF4FF` | Hadith card |
| `cardLight` | `#F9F7EF` | Secondary cards (rows 2-3) |
| `cardWhite` | `#FFFFFF` | Location card, prayer list |
| `highlightBg` | `#EFE6D5` | Active/current row highlight |
| `iconCircleBg` | `#F3ECE0` | Prayer icon circle background |
| `sectionHeaderBg` | `#F9F5EC` | Prayer list section header |

### Text Colors

| Token | Hex | Usage |
|-------|-----|-------|
| `textPrimary` | `#333333` | Main body text |
| `textMuted` | `#666666` | Secondary labels |
| `textOnDark` | `#FFFFFF` | Text on dark green panels |
| `textOnDarkMuted` | `#FFFFFF` at 70% | Secondary text on dark panels |

---

## Typography

### Fonts

| Platform | Arabic Text | English Text |
|----------|------------|-------------|
| Android | `scheherazadeFont` (Scheherazade Regular) | System default |
| iOS | System Arabic (SF Arabic) or bundled Scheherazade | SF Pro |

### Scale

| Level | Size | Weight | Use Case |
|-------|------|--------|----------|
| Display | 28-30sp | Bold | App name (centered), next prayer name |
| Headline | 20sp | Bold | Panel headings ("تلاوة") |
| Title | 16sp | Bold | Section headers, prayer names, Qibla |
| Body | 13-14sp | Medium/Bold | Card titles, selector values, times |
| Caption | 10-12sp | Normal | Labels, subtitles, timestamps |
| Micro | 9sp | Normal | Selector labels, card subtitles |

---

## Corner Radii

| Element | Radius |
|---------|--------|
| Glass panels | 24dp |
| Primary cards | 20dp |
| Secondary cards | 14dp |
| Selector pills | 14dp |
| Top bar (none) | 0dp |
| Prayer list card top | 24dp |
| Prayer list card bottom | 14dp |
| Countdown box | 14dp |
| Prayer row highlight | 10dp |
| Time pill | 10dp |
| Icon circles | 50% (circle) |
| Language pill | 20dp |

---

## Shadows

| Element | Elevation | Approach |
|---------|-----------|----------|
| Glass media panel | 8dp | CardDefaults elevation |
| Primary cards | 0dp | Color shift only (no shadow) |
| Secondary cards | 0dp | Color shift only |
| Location card | 0dp | White on warm bg = natural lift |
| Play button | Cast shadow via Surface | Subtle ambient |

---

## Icons (Material Icons)

| Feature | Icon Name | Android | iOS (SF Symbols) |
|---------|-----------|---------|-------------------|
| Quran | `MenuBook` | `Icons.Default.MenuBook` | `book.fill` |
| Prayer Times | `Schedule` | `Icons.Default.Schedule` | `clock.fill` |
| Hadith | `HistoryEdu` | `Icons.Default.HistoryEdu` | `text.book.closed.fill` |
| Athkar | `StarOutline` | `Icons.Default.StarOutline` | `star` |
| Tracker | `CheckBox` | `Icons.Default.CheckBox` | `checkmark.square.fill` |
| Downloads | `Download` | `Icons.Default.Download` | `arrow.down.circle.fill` |
| Bookmarks | `FavoriteBorder` | `Icons.Default.FavoriteBorder` | `heart` |
| Fajr/Isha | `NightsStay` | `Icons.Default.NightsStay` | `moon.fill` |
| Sunrise/Dhuhr | `WbSunny` | `Icons.Default.WbSunny` | `sun.max.fill` |
| Asr/Maghrib | `WbTwilight` | `Icons.Default.WbTwilight` | `sun.haze.fill` |
| Qibla | `Explore` | `Icons.Default.Explore` | `safari.fill` |
| Location | `LocationOn` | `Icons.Default.LocationOn` | `mappin.circle.fill` |
| Calendar | `CalendarMonth` | `Icons.Default.CalendarMonth` | `calendar` |
| Play | `PlayArrow` | `Icons.Default.PlayArrow` | `play.fill` |
| Pause | `Pause` | `Icons.Default.Pause` | `pause.fill` |
| Skip Next | `SkipNext` | `Icons.Default.SkipNext` | `forward.fill` |
| Skip Prev | `SkipPrevious` | `Icons.Default.SkipPrevious` | `backward.fill` |
| Repeat | `Repeat` | `Icons.Default.Repeat` | `repeat` |
| Shuffle | `Shuffle` | `Icons.Default.Shuffle` | `shuffle` |
| Settings | `Settings` | `Icons.Default.Settings` | `gear` |
| Back | `ArrowBack` | `Icons.AutoMirrored.Filled.ArrowBack` | `chevron.backward` |
| More | `MoreVert` | `Icons.Default.MoreVert` | `ellipsis` |

---

## RTL/LTR Behavior

### Android
- Wrap screens in `CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection())`
- `Row` with `Arrangement.SpaceBetween` auto-flips in RTL
- Media controls forced LTR: `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)`
- Skip button actions swap: left = next in Arabic (forward = left in RTL reading)

### iOS (SwiftUI)
- Set `.environment(\.layoutDirection, language == .arabic ? .rightToLeft : .leftToRight)`
- `HStack` with `Spacer()` auto-flips in RTL
- Use `.flipsForRightToLeftLayoutDirection(false)` on media controls to keep LTR
- Swap skip button actions same as Android

---

## Glass Panel Effect

### Android (Compose)
```kotlin
Brush.linearGradient(
    colors = listOf(
        Color(0xFF234C3E).copy(alpha = 0.95f),
        Color(0xFF143228).copy(alpha = 0.95f)
    )
)
```

### iOS (SwiftUI)
```swift
LinearGradient(
    colors: [
        Color(hex: "234C3E").opacity(0.95),
        Color(hex: "143228").opacity(0.95)
    ],
    startPoint: .topLeading,
    endPoint: .bottomTrailing
)
```

---

## Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| `xs` | 4dp | Inner element gaps |
| `sm` | 6-8dp | Tight spacing |
| `md` | 10-12dp | Between cards in grid, padding |
| `lg` | 14-16dp | Card internal padding |
| `xl` | 20dp | Section padding, header padding |
| `xxl` | 24dp | Major section gaps |

---

## App Logo & Icons

New logo: `logo.png` — mosque dome with crescent in gold ornamental circular border, "الفرقان" text.

### Android Launcher Icons
Generated from `logo.png` to all mipmap densities:
- `ic_launcher.webp` — 48/72/96/144/192px (mdpi through xxxhdpi)
- `ic_launcher_round.webp` — same sizes
- `ic_launcher_foreground.webp` — 108/162/216/324/432px (adaptive icon foreground with 67% safe zone)
- Background color: `#F9F6F0` (warm cream, set in `values/ic_launcher_background.xml`)

### Android Auto
- `drawable-nodpi/quran_logo.webp` — 512x512 high-res logo for media browse items
- Referenced in `QuranMediaBrowserService.kt` via `R.drawable.quran_logo`

### iOS
- Use Asset Catalog with 1x/2x/3x versions
- App icon: 1024x1024 for App Store, auto-scaled for home screen
- Generate from same `logo.png` source

---

## Header Color (App-Wide)

All TopAppBar backgrounds use `#275239` (brand-green from Hadith Stitch design).
All TopAppBar text/icons use `#CDAD70` (brand-gold).

**No overflow/3-dot menu in headers** — all navigation via bottom bar "More" tab.
- **Only exception: QuranReaderScreen** — retains overflow because it has no bottom nav (full-screen reading)
- Search icon retained where applicable (QuranIndex, HadithLibrary)
- "More" dropdown background: `#F9F6EE` (warm cream — matches app theme)

Screens with tabs (QuranIndex): TabRow uses same `#275239` containerColor to visually merge with header.

---

## Dark Mode (Implemented)

All design colors are now dark-mode aware. The `AppColors.kt` defines both `LightAppColors` and `DarkAppColors`.

### Key Dark Mode Colors
| Token | Light | Dark |
|-------|-------|------|
| `screenBackground` | `#F9F6EE` | `#1A1A1A` |
| `cardBackground` | `#FFFFFF` | `#2A2A2A` |
| `topBarBackground` | `#275239` | `#1E2E1F` |
| `goldAccent` | `#CDAD70` | `#CDAD70` (same) |
| `textOnHeader` | `#CDAD70` | `#CDAD70` (gold in both) |
| `textPrimary` | `#3E2723` | `#DDD8D3` |
| Card green | `#E6F2E6` | `#1E2E1F` |
| Card yellow | `#FDF6E3` | `#2E2A1E` |
| Card blue | `#EAF4FF` | `#1E2430` |
| Card light | `#F9F7EF` | `#2A2A2A` |
| Nav bar bg | `#FFFFFF` | `#1E1E1E` |
| Nav tab color | `#275239` | `#CDAD70` (gold in dark) |
| More menu bg | `#F9F6EE` | `#2A2A2A` |
| Overflow bg | `AppTheme.colors.cardBackground` | Dark-mode aware |

### Dark Mode Toggle
- Visible in **every screen's TopAppBar** as a gold sun/moon icon (via `DarkModeToggle` composable)
- Home screen: toggle on the start side of the header
- Other screens: toggle in the TopAppBar `actions` block
- SF Symbols (iOS): `sun.max.fill` / `moon.fill`
- Toggles between `DarkModePreference.ON` and `DarkModePreference.OFF`
- Implemented via `onToggleDarkMode` callback from `MainActivity` through `QuranNavGraph`
- TalkBack: "Switch to dark mode" / "تفعيل الوضع الداكن"

### Overflow Menu Background
- **Light mode**: `#F9F6EE` (warm cream)
- **Dark mode**: `#111111` (near-black) — NOT grey, BLACK for proper contrast

### Rules
- `brandGold` (`#CDAD70`) works in both modes — don't change it
- `brandGreen` panels stay dark — they're already dark in both modes
- Card backgrounds must darken (10-15% luminance)
- Text on cards must be light in dark mode
- Icon tints should brighten in dark mode (use `@Composable` color functions)
