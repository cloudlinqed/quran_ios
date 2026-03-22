# Bookmarks Screen — New Design Specification

## Overview
Displays reading bookmarks, playback bookmarks, and recent pages. Accessible from Home feature grid and bottom nav "More" menu.

---

## Theme Colors

| Element | Color | Hex |
|---------|-------|-----|
| TopAppBar background | BrandGreenDark | `#275239` |
| TopAppBar title/icons | BrandGold | `#CDAD70` |
| Screen background | ScreenBg | `#F9F6EE` |
| Card background | White | `#FFFFFF` (AppTheme.colors.cardBackground) |
| Section header text | IslamicGreen | `#2E7D32` (AppTheme.colors.islamicGreen) |
| Primary text | TextPrimary | AppTheme.colors.textPrimary |
| Secondary text | TextSecondary | AppTheme.colors.textSecondary |
| Delete icon | Error red | MaterialTheme.colorScheme.error |

---

## Layout

```
┌────────────────────────────────────┐
│  TopAppBar: المفضلة     [🗑]   │  ← #275239 bg, #CDAD70 text
├────────────────────────────────────┤
│  Screen bg: #F9F6EE                │
│                                    │
│  § الصفحات الأخيرة (Recent Pages)  │  ← section header (green)
│  ┌──────────────────────────────┐  │
│  │ 🕐 Page 555 — سورة المنافقون │  │  ← History icon, time ago
│  └──────────────────────────────┘  │
│                                    │
│  § علامات القراءة (Reading Bookmarks)│
│  ┌──────────────────────────────┐  │
│  │ 📖 Page 42 — سورة البقرة [🗑]│  │  ← MenuBook icon, delete
│  └──────────────────────────────┘  │
│                                    │
│  § علامات الاستماع (Playback)      │
│  ┌──────────────────────────────┐  │
│  │ 🎧 Al-Baqarah — Minshawi [🗑]│  │  ← Headphones icon
│  └──────────────────────────────┘  │
│                                    │
├──────────────────────────────────────┤
│  [🏠] [📖] [🕌] [☆] [⋯]           │  ← BottomNavBar (bookmarks hidden)
└──────────────────────────────────────┘
```

---

## Components

### Section Headers
- Font: scheherazadeFont (Arabic) / system (English)
- Size: 16sp, Bold
- Color: AppTheme.colors.islamicGreen (unchanged)

### Reading Bookmark Card
- White card, rounded 12dp
- Row: MenuBook icon (green, 32dp) + page number + surah name + date + delete button
- Delete button: red trash icon

### Recent Page Card
- White card, rounded 12dp, elevation 1dp
- Row: History icon (textSecondary, 28dp) + page number + surah name + relative time ("2m ago")
- No delete button

### Playback Bookmark Card
- White card, rounded 12dp
- Row: Headphones icon (green, 32dp) + surah name + reciter + ayah + date + delete

---

## Bottom Navigation
- currentRoute: `"bookmarks"`
- Bookmarks tab hidden from bar
- Visible: Home, Quran, Prayer, Athkar, More

---

## iOS Notes
- Use `List` with sections for the three bookmark types
- SF Symbols: `clock.arrow.circlepath` (recent), `bookmark.fill` (reading), `headphones` (playback)
- Swipe-to-delete on bookmark rows
