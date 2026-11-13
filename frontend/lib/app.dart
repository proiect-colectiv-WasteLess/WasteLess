import 'package:flutter/material.dart';

import 'core/router/app_router.dart';
import 'core/router/app_routes.dart';
import 'core/store/app_store.dart';
import 'core/theme/app_theme.dart';

class WasteLessApp extends StatefulWidget {
  const WasteLessApp({super.key});

  @override
  State<WasteLessApp> createState() => _WasteLessAppState();
}

class _WasteLessAppState extends State<WasteLessApp> {
  /// Created once for the lifetime of the app. Logging out only changes the
  /// route, so the store (and every offer/order change) survives role swaps.
  late final AppStore _store = AppStore();

  @override
  void dispose() {
    _store.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AppStoreScope(
      store: _store,
      child: MaterialApp(
        title: 'WasteLess',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light,
        initialRoute: AppRoutes.welcome,
        onGenerateRoute: AppRouter.onGenerateRoute,
      ),
    );
  }
}
