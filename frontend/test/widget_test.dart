import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:frontend/app.dart';

void main() {
  setUpAll(() async {
    final FontLoader fontLoader = FontLoader('PlusJakartaSans');
    fontLoader.addFont(rootBundle.load('assets/fonts/PlusJakartaSans.ttf'));
    await fontLoader.load();
  });

  Future<void> pumpApp(WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(390, 844));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(const WasteLessApp());
    await tester.pumpAndSettle();
  }

  Future<void> openBusinessDashboard(WidgetTester tester) async {
    await pumpApp(tester);
    await tester.tap(find.text("Let's get started"));
    await tester.pumpAndSettle();
    await tester.tap(find.text("I'm a business"));
    await tester.pumpAndSettle();
  }

  testWidgets('welcome screen presents the brand and primary CTA', (
    WidgetTester tester,
  ) async {
    await pumpApp(tester);

    expect(find.text('WasteLess'), findsOneWidget);
    expect(find.text("Let's get started"), findsOneWidget);
    expect(find.textContaining('Already a member?'), findsOneWidget);
  });

  testWidgets("Let's get started opens the role selection screen", (
    WidgetTester tester,
  ) async {
    await pumpApp(tester);

    await tester.tap(find.text("Let's get started"));
    await tester.pumpAndSettle();

    expect(find.textContaining('How will you'), findsOneWidget);
    expect(find.text("I'm hungry"), findsOneWidget);
    expect(find.text("I'm a business"), findsOneWidget);
  });

  testWidgets('back button returns to the welcome screen', (
    WidgetTester tester,
  ) async {
    await pumpApp(tester);

    await tester.tap(find.text("Let's get started"));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.arrow_back_ios_new_rounded));
    await tester.pumpAndSettle();

    expect(find.text("Let's get started"), findsOneWidget);
  });

  testWidgets('business dashboard shows the performance snapshot and offers', (
    WidgetTester tester,
  ) async {
    await openBusinessDashboard(tester);

    expect(find.text('Fresh Bakery'), findsOneWidget);
    expect(find.text('GOOD AFTERNOON'), findsOneWidget);
    expect(find.text('€284.40'), findsOneWidget);
    expect(find.text('Products sold'), findsOneWidget);
    expect(find.text('Waste avoided'), findsOneWidget);
    expect(find.text('Active offers'), findsOneWidget);
    expect(find.text('Commission'), findsOneWidget);
    expect(find.text('Create Offer'), findsOneWidget);
    expect(find.text('Active Offers'), findsOneWidget);
    expect(find.text('Croissant rescue bag'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Pastry selection'),
      200,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('Pastry selection'), findsOneWidget);
    expect(find.text('Manage all'), findsNothing);
    expect(find.text('AI Waste Assistant'), findsNothing);
    expect(find.text('Dashboard'), findsOneWidget);
  });

  testWidgets('footer navigation switches tabs and keeps the dashboard alive', (
    WidgetTester tester,
  ) async {
    await openBusinessDashboard(tester);

    await tester.tap(find.text('Profile'));
    await tester.pumpAndSettle();
    expect(
      find.text(
        'Manage your business details, hours and payout settings here.',
      ),
      findsOneWidget,
    );

    await tester.tap(find.text('Dashboard'));
    await tester.pumpAndSettle();
    expect(find.text('€284.40'), findsOneWidget);
  });

  testWidgets('AI Insights tab shows predictions and the assistant summary', (
    WidgetTester tester,
  ) async {
    await openBusinessDashboard(tester);

    await tester.tap(find.text('AI Insights'));
    await tester.pumpAndSettle();

    expect(find.text("Good afternoon, let's waste less."), findsOneWidget);
    expect(find.text('PREDICTED SURPLUS'), findsOneWidget);
    expect(find.text('POTENTIAL LOSS'), findsOneWidget);
    expect(find.text('YOU COULD SAVE'), findsOneWidget);
    expect(find.text("Today's predictions"), findsOneWidget);
    expect(find.text('Updated 2m ago'), findsOneWidget);
    expect(find.text('Croissants'), findsOneWidget);
    expect(find.text('HIGH'), findsOneWidget);
    expect(find.text('MEDIUM'), findsOneWidget);
    expect(find.text('AI RECOMMENDATION'), findsWidgets);

    await tester.scrollUntilVisible(
      find.text('Chocolate cake'),
      250,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('Chocolate cake'), findsOneWidget);
    expect(find.text('LOW'), findsOneWidget);
    expect(find.text('No discount necessary yet.'), findsOneWidget);
  });

  testWidgets('prediction CTA opens a prefilled create offer form', (
    WidgetTester tester,
  ) async {
    await openBusinessDashboard(tester);
    await tester.tap(find.text('AI Insights'));
    await tester.pumpAndSettle();

    final Finder cta = find.text('Create offer from prediction').first;
    await tester.ensureVisible(cta);
    await tester.pumpAndSettle();
    await tester.tap(cta);
    await tester.pumpAndSettle();

    expect(find.text('Create new offer'), findsOneWidget);
    expect(find.text('Croissant rescue bag'), findsOneWidget);
    expect(find.text('Bakery'), findsOneWidget);
    expect(
      find.text('A surprise mix of our flaky, freshly baked croissants.'),
      findsOneWidget,
    );
    expect(find.text('8.00'), findsOneWidget);
    expect(find.text('5.20'), findsOneWidget);
    expect(find.text('€2.80 · 35%'), findsOneWidget);
    expect(find.text('€4.68'), findsOneWidget);
    expect(find.text('WasteLess commission: €0.52 per sale'), findsOneWidget);
    expect(find.text('12'), findsOneWidget);
    expect(
      tester
          .widget<EditableText>(find.byType(EditableText).last)
          .controller
          .text,
      '16:00 – 19:00',
    );
    expect(find.text('Publish offer'), findsOneWidget);
  });

  testWidgets('Create Offer opens an empty form and can be dismissed', (
    WidgetTester tester,
  ) async {
    await openBusinessDashboard(tester);

    final Finder createOffer = find.text('Create Offer');
    await tester.ensureVisible(createOffer);
    await tester.pumpAndSettle();
    await tester.tap(createOffer);
    await tester.pumpAndSettle();

    expect(find.text('Create new offer'), findsOneWidget);
    expect(find.text('Add photo'), findsOneWidget);
    expect(find.text('Add a cover photo'), findsOneWidget);
    expect(find.text('Select a category'), findsOneWidget);
    expect(find.text('Publish offer'), findsOneWidget);

    final Iterable<EditableText> fields = tester.widgetList<EditableText>(
      find.byType(EditableText),
    );
    expect(fields.length, 6);
    expect(
      fields.every((EditableText field) => field.controller.text.isEmpty),
      isTrue,
    );

    await tester.tap(find.text('Publish offer'));
    await tester.pump();
    expect(
      find.text('Complete the offer details before publishing.'),
      findsOneWidget,
    );

    await tester.tap(find.byIcon(Icons.arrow_back_ios_new_rounded));
    await tester.pumpAndSettle();
    expect(find.text('€284.40'), findsOneWidget);
  });

  testWidgets('Orders tab lists active orders and switches to completed', (
    WidgetTester tester,
  ) async {
    await openBusinessDashboard(tester);
    await tester.tap(find.text('Orders'));
    await tester.pumpAndSettle();

    expect(find.text('Order management'), findsOneWidget);
    expect(find.text('Active · 3'), findsOneWidget);
    expect(find.text('ORDER #WL-8042'), findsOneWidget);
    expect(find.text('1 × Bakery surprise bag'), findsOneWidget);
    expect(find.text('Pickup: Today, 17:30–19:00 · Code 4862'), findsOneWidget);
    expect(find.text('€7.00'), findsOneWidget);
    expect(find.text('Validate code / QR'), findsNWidgets(3));

    await tester.tap(find.text('Completed'));
    await tester.pumpAndSettle();
    expect(find.text('1 × Pastry selection'), findsOneWidget);
    expect(find.text('2 × Croissant rescue bag'), findsOneWidget);
    expect(find.text('Validate code / QR'), findsNothing);
    expect(find.text('Collected'), findsNWidgets(2));

    await tester.tap(find.text('Active · 3'));
    await tester.pumpAndSettle();
    expect(find.text('ORDER #WL-8042'), findsOneWidget);
  });

  testWidgets('validating an order moves it to the completed tab', (
    WidgetTester tester,
  ) async {
    await openBusinessDashboard(tester);
    await tester.tap(find.text('Orders'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Validate code / QR').first);
    await tester.pumpAndSettle();

    expect(find.text('Validate pickup'), findsOneWidget);
    expect(find.text('4862'), findsOneWidget);

    await tester.tap(find.text('Confirm pickup'));
    await tester.pumpAndSettle();

    expect(find.text('Order #WL-8042 completed.'), findsOneWidget);
    expect(find.text('1 × Bakery surprise bag'), findsNothing);
    expect(find.text('Active · 2'), findsOneWidget);

    await tester.tap(find.text('Completed'));
    await tester.pumpAndSettle();
    expect(find.text('1 × Bakery surprise bag'), findsOneWidget);
    expect(find.text('Collected'), findsNWidgets(3));
  });

  testWidgets('an offer can be deleted from the dashboard list', (
    WidgetTester tester,
  ) async {
    await openBusinessDashboard(tester);

    final Finder deleteAction = find.text('Delete').first;
    await tester.ensureVisible(deleteAction);
    await tester.pumpAndSettle();
    await tester.tap(deleteAction);
    await tester.pumpAndSettle();

    expect(find.text('Delete offer?'), findsOneWidget);
    await tester.tap(find.text('Delete offer'));
    await tester.pumpAndSettle();

    expect(find.text('Croissant rescue bag'), findsNothing);
    expect(find.text('Pastry selection'), findsOneWidget);
  });

  testWidgets('an offer can be edited from the dashboard list', (
    WidgetTester tester,
  ) async {
    await openBusinessDashboard(tester);

    final Finder editAction = find.text('Edit').first;
    await tester.ensureVisible(editAction);
    await tester.pumpAndSettle();
    await tester.tap(editAction);
    await tester.pumpAndSettle();

    expect(find.text('Edit offer'), findsOneWidget);
    await tester.enterText(find.byType(TextField).first, 'Morning rescue box');
    await tester.tap(find.text('Save changes'));
    await tester.pumpAndSettle();

    expect(find.text('Morning rescue box'), findsOneWidget);
    expect(find.text('Croissant rescue bag'), findsNothing);
  });
}
