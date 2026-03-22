# Qibla Direction Screen — New Design Specification

## Theme Colors

| Element | Hex |
|---------|-----|
| TopAppBar background | `#275239` |
| TopAppBar title/icons | `#CDAD70` |
| Screen background | `#F9F6EE` |
| Compass accent | AppTheme.colors.islamicGreen |
| Qibla indicator | AppTheme.colors.islamicGreen |
| Text primary | AppTheme.colors.textPrimary |
| Text secondary | AppTheme.colors.textSecondary |

## Layout
```
┌────────────────────────────────────┐
│  TopAppBar: اتجاه القبلة   [←]    │  ← #275239 bg, #CDAD70 text
├────────────────────────────────────┤
│  Screen bg: #F9F6EE                │
│                                    │
│           ┌─────────┐              │
│           │         │              │
│           │ Compass │              │  ← rotating compass with
│           │  Rose   │              │     Qibla direction indicator
│           │         │              │
│           └─────────┘              │
│                                    │
│        Qibla: 245° SW             │  ← bearing text
│        Distance: 1,234 km          │
│                                    │
│  ┌── Calibration Card ──────────┐  │
│  │  Move phone in figure-8      │  │
│  └──────────────────────────────┘  │
│                                    │
├──────────────────────────────────────┤
│  [🏠] [📖] [🕌] [☆] [⋯]           │  ← BottomNavBar
└──────────────────────────────────────┘
```

## Bottom Navigation
- currentRoute: `"qibla"`
- Qibla not a primary tab — all 5 tabs visible

## iOS Notes
- Use `CoreLocation` heading for compass
- SF Symbols: `safari.fill` (compass), `location.north.circle` (Qibla arrow)
- Sensor: `CLLocationManager.startUpdatingHeading()`
