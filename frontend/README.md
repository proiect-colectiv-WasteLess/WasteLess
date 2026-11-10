# WasteLess — Flutter app

The client application for WasteLess. See the [repository README](../README.md)
for the full project layout and design notes.

## Architecture

- `lib/core` — theme tokens (`AppColors`, `AppTypography`, `AppSpacing`),
  routing (`AppRoutes`, `AppRouter`), shared widgets and asset paths.
- `lib/features/<feature>/presentation` — pages and feature-specific widgets.
  Features stay isolated; shared pieces move to `core/widgets`.

The UI uses Material 3 with a custom brand theme. Typography is Plus Jakarta
Sans, bundled as a variable font so every weight renders natively offline.

## Commands

```bash
flutter pub get
flutter run            # choose a connected device
flutter analyze
flutter test
```
