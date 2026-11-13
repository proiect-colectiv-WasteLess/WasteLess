# WasteLess — Flutter app

The client application for WasteLess. See the [repository README](../README.md)
for the full project layout and design notes.

## Architecture

- `lib/core` — theme tokens (`AppColors`, `AppTypography`, `AppSpacing`),
  routing (`AppRoutes`, `AppRouter`), the shared `AppStore` store scope,
  the logout flow, shared widgets and asset paths.
- `lib/features/<feature>` — features grow `domain/`, `data/` and
  `presentation/` siblings as needed. Onboarding stays isolated, the business
  and customer features share the offer/order models in
  `features/business/domain`, and shared pieces move to `core`.

`AppStore` is created once in `WasteLessApp` above `MaterialApp`, so offers,
orders and the basket survive logout and role switching. Nothing is persisted
to disk: restarting the app resets the demo data.

The UI uses Material 3 with a custom brand theme. Typography is Plus Jakarta
Sans, bundled as a variable font so every weight renders natively offline.

## Credits

Prediction illustrations use [Twemoji](https://github.com/jdecked/twemoji)
graphics, licensed under CC-BY 4.0.

## Commands

```bash
flutter pub get
flutter run            # choose a connected device
flutter analyze
flutter test
```
