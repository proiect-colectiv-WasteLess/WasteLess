import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/constants/app_assets.dart';
import '../../../../core/router/app_routes.dart';
import '../../../../core/store/app_store.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../domain/models/deal.dart';
import '../widgets/customer_page_header.dart';

class NotificationsPage extends StatelessWidget {
  const NotificationsPage({super.key});

  static const List<_Notice> _notices = <_Notice>[
    _Notice(
      title: 'Fresh bakery deals just dropped!',
      body: 'Flour & Fold · 500m away · Up to 50% off',
      age: '8 min',
      icon: Icons.auto_awesome_outlined,
      background: AppColors.sageTint,
      foreground: AppColors.forest,
    ),
    _Notice(
      title: 'Only 2 surprise bags left',
      body: 'Green Table is almost sold out for today.',
      age: '24 min',
      icon: Icons.shopping_bag_outlined,
      background: AppColors.peachTint,
      foreground: Color(0xFFBD6334),
    ),
    _Notice(
      title: 'Your pickup window ends soon',
      body: 'Collect order #WL-7921 within 30 minutes.',
      age: '1h',
      icon: Icons.access_time_rounded,
      background: AppColors.infoTint,
      foreground: Color(0xFF5E72A0),
    ),
  ];

  void _open(BuildContext context, Deal deal) {
    Navigator.of(context).pushNamed(AppRoutes.customerProduct, arguments: deal);
  }

  @override
  Widget build(BuildContext context) {
    final List<Deal> deals = AppStoreScope.of(context).deals;

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.cream,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: Scaffold(
        backgroundColor: AppColors.cream,
        body: SafeArea(
          child: Align(
            alignment: Alignment.topCenter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: ListView(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
                children: <Widget>[
                  const CustomerPageHeader(
                    title: 'Notifications',
                    showBack: true,
                  ),
                  const SizedBox(height: 16),
                  _FeatureCard(
                    onView: deals.isEmpty
                        ? null
                        : () => _open(context, deals.first),
                  ),
                  Padding(
                    padding: const EdgeInsets.only(top: 22, bottom: 10),
                    child: Text(
                      'Today',
                      style: AppTypography.style(
                        size: 16,
                        weight: FontWeight.w800,
                        letterSpacing: -0.3,
                        color: AppColors.forestDeep,
                      ),
                    ),
                  ),
                  for (int i = 0; i < _notices.length; i++) ...<Widget>[
                    _NoticeTile(
                      notice: _notices[i],
                      onTap: deals.isEmpty
                          ? null
                          : () => _open(context, deals[i % deals.length]),
                    ),
                    const SizedBox(height: 9),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Notice {
  const _Notice({
    required this.title,
    required this.body,
    required this.age,
    required this.icon,
    required this.background,
    required this.foreground,
  });

  final String title;
  final String body;
  final String age;
  final IconData icon;
  final Color background;
  final Color foreground;
}

class _FeatureCard extends StatelessWidget {
  const _FeatureCard({required this.onView});

  final VoidCallback? onView;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: Container(
        height: 180,
        color: AppColors.forest,
        child: Row(
          children: <Widget>[
            Expanded(
              flex: 13,
              child: Padding(
                padding: const EdgeInsets.all(17),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Text(
                      'JUST FOR YOU',
                      style: AppTypography.style(
                        size: 9,
                        letterSpacing: 1.2,
                        color: AppColors.sageTint.withValues(alpha: 0.8),
                      ),
                    ),
                    const SizedBox(height: 7),
                    Text(
                      'Today’s tastiest rescue',
                      style: AppTypography.style(
                        size: 17,
                        weight: FontWeight.w800,
                        height: 1.15,
                        letterSpacing: -0.4,
                        color: AppColors.white,
                      ),
                    ),
                    const SizedBox(height: 7),
                    Text(
                      'Pastries for €2.80, just around the corner.',
                      style: AppTypography.style(
                        size: 10.5,
                        color: AppColors.sageTint.withValues(alpha: 0.85),
                      ),
                    ),
                    const SizedBox(height: 10),
                    Material(
                      color: AppColors.amber,
                      borderRadius: BorderRadius.circular(9),
                      child: InkWell(
                        borderRadius: BorderRadius.circular(9),
                        onTap: onView,
                        child: Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12,
                            vertical: 9,
                          ),
                          child: Text(
                            'View deal',
                            style: AppTypography.style(
                              size: 10.5,
                              weight: FontWeight.w700,
                              color: AppColors.forestDeep,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Expanded(
              flex: 10,
              child: SizedBox.expand(
                child: Image.asset(
                  AppAssets.dealPastry,
                  fit: BoxFit.cover,
                  cacheWidth: 360,
                  semanticLabel: 'Pastry selection',
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _NoticeTile extends StatelessWidget {
  const _NoticeTile({required this.notice, required this.onTap});

  final _Notice notice;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.white,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: <Widget>[
              Container(
                width: 43,
                height: 43,
                decoration: BoxDecoration(
                  color: notice.background,
                  borderRadius: BorderRadius.circular(13),
                ),
                child: Icon(notice.icon, size: 22, color: notice.foreground),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      notice.title,
                      style: AppTypography.style(
                        size: 12.5,
                        weight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      notice.body,
                      style: AppTypography.style(
                        size: 10.5,
                        color: AppColors.muted,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      notice.age,
                      style: AppTypography.style(
                        size: 9.5,
                        color: AppColors.mist,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(
                Icons.chevron_right_rounded,
                size: 19,
                color: AppColors.muted,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
