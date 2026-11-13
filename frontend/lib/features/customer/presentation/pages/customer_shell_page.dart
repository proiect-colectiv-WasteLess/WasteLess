import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/router/app_routes.dart';
import '../../../../core/store/app_store.dart';
import '../../../../core/theme/app_colors.dart';
import '../widgets/cart_fab.dart';
import '../widgets/customer_bottom_nav.dart';
import 'customer_explore_page.dart';
import 'customer_home_page.dart';
import 'customer_orders_page.dart';
import 'customer_profile_page.dart';

/// Root of the customer experience. The footer navigation stays fixed while
/// each tab keeps its own state alive in the [IndexedStack].
class CustomerShellPage extends StatefulWidget {
  const CustomerShellPage({super.key, this.initialTab = homeTab});

  static const int homeTab = 0;
  static const int exploreTab = 1;
  static const int ordersTab = 2;
  static const int profileTab = 3;

  final int initialTab;

  @override
  State<CustomerShellPage> createState() => _CustomerShellPageState();
}

class _CustomerShellPageState extends State<CustomerShellPage> {
  late int _currentIndex = widget.initialTab;

  void _selectTab(int index) {
    if (index == _currentIndex) {
      return;
    }
    setState(() => _currentIndex = index);
  }

  @override
  Widget build(BuildContext context) {
    final AppStore store = AppStoreScope.of(context);

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.white,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: Scaffold(
        backgroundColor: AppColors.cream,
        body: Stack(
          children: <Widget>[
            IndexedStack(
              index: _currentIndex,
              children: <Widget>[
                CustomerHomePage(
                  onOpenExplore: () => _selectTab(CustomerShellPage.exploreTab),
                ),
                const CustomerExplorePage(),
                const CustomerOrdersPage(),
                const CustomerProfilePage(),
              ],
            ),
            if (store.basketQuantity > 0)
              Positioned(
                right: 17,
                bottom: 14,
                child: CartFab(
                  count: store.basketQuantity,
                  onTap: () =>
                      Navigator.of(context).pushNamed(AppRoutes.customerCart),
                ),
              ),
          ],
        ),
        bottomNavigationBar: CustomerBottomNav(
          currentIndex: _currentIndex,
          onSelected: _selectTab,
        ),
      ),
    );
  }
}
