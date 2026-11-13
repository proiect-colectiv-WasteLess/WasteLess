import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/theme/app_colors.dart';
import '../widgets/business_bottom_nav.dart';
import 'business_ai_insights_page.dart';
import 'business_dashboard_page.dart';
import 'business_orders_page.dart';
import 'business_profile_page.dart';

/// Root of the business experience. The footer navigation stays fixed while
/// each tab keeps its own state alive in the [IndexedStack].
class BusinessShellPage extends StatefulWidget {
  const BusinessShellPage({super.key});

  @override
  State<BusinessShellPage> createState() => _BusinessShellPageState();
}

class _BusinessShellPageState extends State<BusinessShellPage> {
  int _currentIndex = 0;

  static const List<Widget> _pages = <Widget>[
    BusinessDashboardPage(),
    BusinessAiInsightsPage(),
    BusinessOrdersPage(),
    BusinessProfilePage(),
  ];

  void _selectTab(int index) {
    if (index == _currentIndex) {
      return;
    }
    setState(() => _currentIndex = index);
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.white,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: Scaffold(
        backgroundColor: AppColors.canvas,
        body: IndexedStack(index: _currentIndex, children: _pages),
        bottomNavigationBar: BusinessBottomNav(
          currentIndex: _currentIndex,
          onSelected: _selectTab,
        ),
      ),
    );
  }
}
