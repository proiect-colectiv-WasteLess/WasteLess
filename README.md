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
    │   │   ├── auth/         # Shared logout flow
    │   │   ├── constants/    # Asset paths
    │   │   ├── router/       # Named routes + page transitions
    │   │   ├── store/        # Shared in-memory AppStore
    │   │   ├── theme/        # Colors, typography, spacing, ThemeData
    │   │   └── widgets/      # Shared UI (logo, buttons, feedback)
    │   └── features/
    │       ├── onboarding/
    │       │   └── presentation/
    │       │       ├── pages/    # Welcome, role selection
    │       │       └── widgets/  # Onboarding-specific components
    │       ├── customer/
    │       │   ├── domain/       # Customer-facing Deal model
    │       │   └── presentation/ # Home, Explore, Product, Basket, Payment...
    │       └── business/
    │           ├── data/         # AI prediction repository
    │           ├── domain/       # Shared offer + order models, draft, prediction
    │           └── presentation/ # Dashboard, insights, orders, offer form
    └── test/                 # Widget tests
```

The `core/` layer holds anything reused across features; each feature owns its
own `presentation` layer and can later grow `domain/` and `data/` siblings
without disturbing the rest of the app.

## Shared in-memory store

The app ships one hardcoded customer (**Noa Martins**) and one hardcoded
business (**Fresh Bakery**). `AppStore` in `lib/core/store/` is the single
source of truth for offers, orders and the customer basket, and is created once
above `MaterialApp` so it survives logout and role switching.

- Business offer changes (create, edit, pause, delete) are reflected instantly
  in the business dashboard and in the customer’s deal feed.
- A customer order decrements the offer’s remaining stock and appears in both
  the customer’s Orders tab and the business Orders tab.
- Validating a pickup moves the shared order to Completed on both sides.
- Logging out returns to the welcome screen without clearing anything; all
  state resets only when the app process restarts.

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
4. **AI Insights** — the assistant’s greeting, predicted surplus, potential
   loss and savings, plus item-level predictions with discount
   recommendations and a create-offer shortcut.
5. **Create offer** — a form for product details, pricing and pickup windows
   with a live savings breakdown. Opens empty from the dashboard, or
   prefilled when started from an AI prediction.
6. **Orders** — active and completed tabs. Active orders show their pickup
   window, code, price and a validation action; validating a code confirms the
   pickup and moves the order to the completed archive. Customer orders placed
   in the other role show up here immediately.
7. **Business profile** — account identity, live offer/order stats, settings
   shortcuts and the logout action.
8. **Customer home** — greeting, search, promo banner, categories and the
   closest / almost-gone deals, generated from the business’s live offers.
   Entered from "I'm hungry"; the footer (Home, Explore, Orders, Profile)
   stays fixed and a floating basket appears once something is added.
9. **Explore** — radius, category, discount, sort and quick filters over the
   nearby deals.
10. **Product → Basket → Payment → Success** — deal detail with pickup info,
    quantity stepper and savings summary, a (mock) card checkout, and the
    pickup ticket with code. Paying creates the order for both roles.
11. **Notifications, Orders and Profile** — alerts that deep-link to deals,
    active/completed orders with pickup code, and the impact card with account
    menu and logout.

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
