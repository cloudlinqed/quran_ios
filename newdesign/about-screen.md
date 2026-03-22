# About Screen — New Design Specification

## Theme Colors

| Element | Hex |
|---------|-----|
| TopAppBar background | `#275239` |
| TopAppBar title/icons | `#CDAD70` |
| Screen background | `#F9F6EE` |
| Card background | AppTheme.colors.cardBackground |
| Accent links/icons | AppTheme.colors.islamicGreen |

## Layout
```
┌────────────────────────────────────┐
│  TopAppBar: حول التطبيق    [←]    │  ← #275239 bg, #CDAD70 text
├────────────────────────────────────┤
│  Screen bg: #F9F6EE                │
│                                    │
│  ┌── App Info Card ─────────────┐  │
│  │  App Icon                    │  │
│  │  الفرقان / Alfurqan          │  │
│  │  v2.3.5                      │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Links ─────────────────────┐  │
│  │  Rate App                    │  │
│  │  Share App                   │  │
│  │  Privacy Policy              │  │
│  │  Contact Developer           │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Credits ───────────────────┐  │
│  │  Quran text source           │  │
│  │  Audio sources               │  │
│  │  Font credits                │  │
│  └──────────────────────────────┘  │
│                                    │
├──────────────────────────────────────┤
│  [🏠] [📖] [🕌] [☆] [⋯]           │  ← BottomNavBar
└──────────────────────────────────────┘
```

## Bottom Navigation
- currentRoute: `"about"`
- About not a primary tab — all 5 tabs visible

## iOS Notes
- Use `Form` or `List` with grouped sections
- Link rows use `Link` or `openURL` environment
- SF Symbols: `star.fill` (rate), `square.and.arrow.up` (share), `hand.raised` (privacy)
