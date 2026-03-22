# Unified Settings Screen — New Design Specification

## Theme Colors

| Element | Hex |
|---------|-----|
| TopAppBar background | `#275239` |
| TopAppBar title/icons | `#CDAD70` |
| Screen background | `#F9F6EE` |
| Section card background | AppTheme.colors.cardBackground |
| Section title | AppTheme.colors.islamicGreen |
| Setting label text | AppTheme.colors.textPrimary |
| Setting subtitle | AppTheme.colors.textSecondary |
| Switch track (on) | AppTheme.colors.islamicGreen |
| Accent/selected | AppTheme.colors.islamicGreen |
| Chip selected bg | AppTheme.colors.chipSelectedBackground |

## Layout
```
┌────────────────────────────────────┐
│  TopAppBar: الإعدادات       [←]   │  ← #275239 bg, #CDAD70 text
├────────────────────────────────────┤
│  Screen bg: #F9F6EE                │
│  Scrollable Column                 │
│                                    │
│  ┌── Reading Theme Section ─────┐  │
│  │  Theme chips (Sepia, Light,  │  │
│  │  Night, Paper, Ocean,        │  │
│  │  Tajweed, Custom)            │  │
│  │  Custom color pickers        │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Athkar Notifications ──────┐  │
│  │  Morning Athkar [toggle]     │  │
│  │  Evening Athkar [toggle]     │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Display Settings ──────────┐  │
│  │  Dark Mode selector          │  │
│  │  Language selector           │  │
│  │  Indo-Arabic numerals        │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Reading Reminder ──────────┐  │
│  │  Enable [toggle]             │  │
│  │  Interval selector           │  │
│  │  Quiet hours                 │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Playback Settings ────────┐  │
│  │  Speed, pitch, volume, etc.  │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Font Settings ─────────────┐  │
│  │  Mushaf Font / Plain Text    │  │
│  │  Bold Font toggle            │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Prayer Settings ──────────┐  │
│  │  Calculation method          │  │
│  │  Per-prayer athan modes      │  │
│  │  Athan volume/silence        │  │
│  └──────────────────────────────┘  │
│                                    │
│  No BottomNavBar (sub-screen)      │
└────────────────────────────────────┘
```

## Note
Settings is a utility sub-screen — NO bottom navigation bar. User navigates back via the back arrow.

## Section Card Pattern (SettingsSectionWood)
- White card background, rounded corners
- Section title in islamicGreen, bold
- Items separated by spacing (no dividers per design system)
- Switch items, clickable items, chip selectors

## iOS Notes
- Use `Form` with `Section` for grouped settings
- `Toggle` for switches
- `Picker` with `.segmented` style for theme selection
- `NavigationLink` for sub-screens (athan settings, quiet hours)
