# Daily Tracker Screen — New Design Specification

## Theme Colors

| Element | Hex |
|---------|-----|
| TopAppBar background | `#275239` |
| TopAppBar title/icons | `#CDAD70` |
| Screen background | `#F9F6EE` |
| Card background | AppTheme.colors.cardBackground |
| Accent/progress | AppTheme.colors.islamicGreen |
| Dark text | AppTheme.colors.darkGreen |
| Completed indicators | AppTheme.colors.lightGreen |

## Layout
```
┌────────────────────────────────────┐
│  TopAppBar: المتابعة اليومية   │  ← #275239 bg, #CDAD70 text
├────────────────────────────────────┤
│  Screen bg: #F9F6EE                │
│                                    │
│  ┌── Progress Summary Card ─────┐  │
│  │  Today's Progress: 3/5       │  │
│  │  ███████░░░ 60%              │  │  ← green progress bar
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Activity Checklist ────────┐  │
│  │  ☑ Quran Reading             │  │
│  │  ☑ Morning Athkar            │  │
│  │  ☐ Evening Athkar            │  │
│  │  ☑ Prayer on Time            │  │
│  │  ☐ Hadith Reading            │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Khatmah Goal ─────────────┐  │
│  │  Pages read today: 5         │  │
│  └──────────────────────────────┘  │
│                                    │
├──────────────────────────────────────┤
│  [🏠] [📖] [🕌] [☆] [⋯]           │  ← Tracker hidden
└──────────────────────────────────────┘
```

## Bottom Navigation
- currentRoute: `"tracker"`
- Tracker not a primary tab — always shows all 5 tabs (Home, Quran, Prayer, Athkar, More)

## iOS Notes
- Use `ProgressView` for progress bars
- SF Symbols: `checkmark.circle.fill` (done), `circle` (pending)
- `List` with `Toggle` style for checklist items
