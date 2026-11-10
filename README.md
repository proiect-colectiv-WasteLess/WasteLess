# WasteLess

WasteLess helps people rescue delicious surplus food from local businesses —
and helps those businesses waste less.

## Repository layout

```
WasteLess/
├── README.md
└── frontend/                 # Flutter application
    ├── assets/
    │   ├── fonts/            # Plus Jakarta Sans (variable)
    │   └── images/           # Brand photography
    ├── lib/
    │   ├── main.dart         # Entry point + system chrome
    │   ├── app.dart          # MaterialApp + theme + routing
    │   ├── core/             # Cross-feature foundations
    │   │   ├── constants/    # Asset paths
    │   │   ├── router/       # Named routes + page transitions
    │   │   ├── theme/        # Colors, typography, spacing, ThemeData
    │   │   └── widgets/      # Shared UI (logo, buttons, feedback)
    │   └── features/
    │       ├── onboarding/
    │       │   └── presentation/
    │       │       ├── pages/    # Welcome, role selection
    │       │       └── widgets/  # Onboarding-specific components
    │       └── business/
    │           ├── data/         # Offer repository (in-memory for now)
    │           ├── domain/       # Business offer model
    │           └── presentation/ # Dashboard, tab shell, widgets
    └── test/                 # Widget tests
```

The `core/` layer holds anything reused across features; each feature owns its
own `presentation` layer and can later grow `domain/` and `data/` siblings
without disturbing the rest of the app.

## Screens

1. **Welcome** — full-bleed food photography, brand lockup, value proposition,
   and the “Let’s get started” call to action.
2. **Role selection** — choose between the client (“I’m hungry”) and business
   paths. Reached from the welcome CTA; the back action returns to it.
3. **Business dashboard** — revenue snapshot, performance stats, and the
   published offers list with view, edit, pause and delete actions. Entering it
   from “I’m a business” makes the business tab shell the app root, so the
   footer navigation stays fixed across Dashboard, AI Insights, Orders and
   Profile.

## Running

```bash
cd frontend
flutter pub get
flutter run
```

Requires Flutter 3.44+ (Dart 3.12+).

## Quality checks

```bash
cd frontend
flutter analyze
flutter test
```
