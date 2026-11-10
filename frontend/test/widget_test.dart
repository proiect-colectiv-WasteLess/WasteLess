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

    await tester.tap(find.text('Orders'));
    await tester.pumpAndSettle();
    expect(
      find.text('Customer rescues and pickup updates will show up here.'),
      findsOneWidget,
    );

    await tester.tap(find.text('Dashboard'));
    await tester.pumpAndSettle();
    expect(find.text('€284.40'), findsOneWidget);
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
