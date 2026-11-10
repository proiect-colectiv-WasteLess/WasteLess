import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:frontend/app.dart';

void main() {
  Future<void> pumpApp(WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(390, 844));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(const WasteLessApp());
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
}
