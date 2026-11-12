import 'package:flutter/material.dart';

import '../../features/business/domain/models/offer_draft.dart';
import '../../features/business/presentation/pages/business_shell_page.dart';
import '../../features/business/presentation/pages/create_offer_page.dart';
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
