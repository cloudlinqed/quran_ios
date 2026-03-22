# Downloads Screen — New Design Specification

## Theme Colors

| Element | Hex |
|---------|-----|
| TopAppBar background | `#275239` |
| TopAppBar title/icons | `#CDAD70` |
| Screen background | `#F9F6EE` |
| Card background | AppTheme.colors.cardBackground |
| FAB color | AppTheme.colors.islamicGreen |
| Progress indicator | AppTheme.colors.islamicGreen |

## Layout
- TopAppBar: "التحميلات" / "Downloads", back arrow,  — all in gold on dark green
- FAB: floating action button for "download all" if applicable
- List of downloaded surahs with reciter info, file size, delete option
- Empty state with download icon and instructional text

## Bottom Navigation
- currentRoute: `"downloads"`
- Downloads tab hidden from bar

## iOS Notes
- SF Symbols: `arrow.down.circle.fill` for download, `trash` for delete
- Use `EditMode` for bulk delete
