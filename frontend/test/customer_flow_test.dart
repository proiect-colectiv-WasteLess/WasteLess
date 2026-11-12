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

  Future<void> openCustomerHome(WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(390, 844));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(const WasteLessApp());
    await tester.pumpAndSettle();
    await tester.tap(find.text("Let's get started"));
    await tester.pumpAndSettle();
    await tester.tap(find.text("I'm hungry"));
    await tester.pumpAndSettle();
  }

  Future<void> tapScrolled(WidgetTester tester, Finder finder) async {
    await tester.scrollUntilVisible(
      finder,
      200,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(finder);
    await tester.pumpAndSettle();
  }

  testWidgets("I'm hungry opens the customer home with its footer", (
    WidgetTester tester,
  ) async {
    await openCustomerHome(tester);

    expect(find.textContaining('Hey, hungry human!'), findsOneWidget);
    expect(find.text('What are you craving?'), findsOneWidget);
    expect(find.text('Home'), findsOneWidget);
    expect(find.text('Explore'), findsOneWidget);
    expect(find.text('Orders'), findsOneWidget);
    expect(find.text('Profile'), findsOneWidget);
  });

  testWidgets('bell opens notifications and back returns home', (
    WidgetTester tester,
  ) async {
    await openCustomerHome(tester);

    await tester.tap(find.byIcon(Icons.notifications_none_rounded));
    await tester.pumpAndSettle();
    expect(find.text('Today’s tastiest rescue'), findsOneWidget);

    await tester.tap(find.byIcon(Icons.arrow_back_ios_new_rounded));
    await tester.pumpAndSettle();
    expect(find.textContaining('Hey, hungry human!'), findsOneWidget);
  });

  testWidgets('explore filters deals by search radius', (
    WidgetTester tester,
  ) async {
    await openCustomerHome(tester);

    await tester.tap(find.text('Explore'));
    await tester.pumpAndSettle();
    expect(find.text('2 delicious deals'), findsOneWidget);

    await tester.tap(find.text('5 km'));
    await tester.pumpAndSettle();
    expect(find.text('4 delicious deals'), findsOneWidget);

    await tester.tap(find.text('500 m'));
    await tester.pumpAndSettle();
    expect(find.text('1 delicious deals'), findsOneWidget);
  });

  testWidgets('rescuing a deal runs product, basket, payment and success', (
    WidgetTester tester,
  ) async {
    await openCustomerHome(tester);

    await tester.tap(find.text('Explore'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Fresh bakery surprise bag'));
    await tester.pumpAndSettle();
    expect(find.text('Get directions'), findsOneWidget);

    await tester.tap(find.text('Add to basket'));
    await tester.pumpAndSettle();
    expect(find.text('Your basket'), findsOneWidget);
    expect(find.text('Continue to payment'), findsOneWidget);

    await tester.tap(find.text('Continue to payment'));
    await tester.pumpAndSettle();
    expect(find.text('Secure payment'), findsOneWidget);

    await tapScrolled(tester, find.text('Pay €3.50'));
    expect(find.text('RESCUE CONFIRMED'), findsOneWidget);
    expect(find.text('Show this code at pickup'), findsOneWidget);

    await tapScrolled(tester, find.text('View my order'));
    expect(find.text('My orders'), findsOneWidget);
    expect(find.text('READY FOR PICKUP'), findsNWidgets(2));
  });
}
