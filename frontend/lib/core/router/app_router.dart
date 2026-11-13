import 'package:flutter/material.dart';

import '../../features/business/domain/models/business_order.dart';
import '../../features/business/domain/models/offer_draft.dart';
import '../../features/business/presentation/pages/business_shell_page.dart';
import '../../features/business/presentation/pages/create_offer_page.dart';
import '../../features/customer/domain/models/deal.dart';
import '../../features/customer/presentation/pages/cart_page.dart';
import '../../features/customer/presentation/pages/checkout_page.dart';
import '../../features/customer/presentation/pages/customer_shell_page.dart';
import '../../features/customer/presentation/pages/notifications_page.dart';
import '../../features/customer/presentation/pages/product_page.dart';
import '../../features/customer/presentation/pages/success_page.dart';
import '../../features/onboarding/presentation/pages/role_selection_page.dart';
import '../../features/onboarding/presentation/pages/welcome_page.dart';
import 'app_routes.dart';

abstract final class AppRouter {
  static Route<dynamic> onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case AppRoutes.createOffer:
        final Object? arguments = settings.arguments;
        return _page(
          CreateOfferPage(
            initialDraft: arguments is OfferDraft ? arguments : null,
          ),
          settings,
        );
      case AppRoutes.businessDashboard:
        return _page(const BusinessShellPage(), settings);
      case AppRoutes.customerHome:
        final Object? tab = settings.arguments;
        return _page(
          CustomerShellPage(
            initialTab: tab is int ? tab : CustomerShellPage.homeTab,
          ),
          settings,
        );
      case AppRoutes.customerProduct:
        final Object? deal = settings.arguments;
        if (deal is! Deal) {
          return _page(const CustomerShellPage(), settings);
        }
        return _page(ProductPage(deal: deal), settings);
      case AppRoutes.customerCart:
        return _page(const CartPage(), settings);
      case AppRoutes.customerCheckout:
        return _page(const CheckoutPage(), settings);
      case AppRoutes.customerSuccess:
        final Object? order = settings.arguments;
        if (order is! BusinessOrder) {
          return _page(const CustomerShellPage(), settings);
        }
        return _page(SuccessPage(order: order), settings);
      case AppRoutes.customerNotifications:
        return _page(const NotificationsPage(), settings);
      case AppRoutes.roleSelection:
        return _page(const RoleSelectionPage(), settings);
      case AppRoutes.welcome:
      default:
        return _page(const WelcomePage(), settings);
    }
  }

  static PageRouteBuilder<void> _page(Widget child, RouteSettings settings) {
    return PageRouteBuilder<void>(
      settings: settings,
      transitionDuration: const Duration(milliseconds: 420),
      reverseTransitionDuration: const Duration(milliseconds: 300),
      pageBuilder: (_, _, _) => child,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        final CurvedAnimation curve = CurvedAnimation(
          parent: animation,
          curve: Curves.easeOutCubic,
          reverseCurve: Curves.easeInCubic,
        );

        return FadeTransition(
          opacity: curve,
          child: SlideTransition(
            position: Tween<Offset>(
              begin: const Offset(0, 0.035),
              end: Offset.zero,
            ).animate(curve),
            child: child,
          ),
        );
      },
    );
  }
}
