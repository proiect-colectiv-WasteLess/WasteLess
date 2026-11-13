import 'package:flutter/material.dart';

import '../../../../core/auth/logout.dart';
import '../../../../core/store/app_store.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_snack_bar.dart';
import '../widgets/customer_page_header.dart';

class CustomerProfilePage extends StatelessWidget {
  const CustomerProfilePage({super.key});

  static const List<_MenuItem> _menu = <_MenuItem>[
    _MenuItem(Icons.favorite_border_rounded, 'Favorite businesses'),
    _MenuItem(Icons.account_balance_wallet_outlined, 'Payment methods'),
    _MenuItem(Icons.notifications_none_rounded, 'Notifications'),
    _MenuItem(Icons.location_on_outlined, 'Location permissions'),
    _MenuItem(Icons.shopping_bag_outlined, 'Order history'),
    _MenuItem(Icons.grid_view_rounded, 'Settings'),
  ];

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      bottom: false,
      child: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 460),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 110),
            children: <Widget>[
              const CustomerPageHeader(title: 'Profile'),
              const SizedBox(height: 14),
              _ProfileHead(
                onEdit: () =>
                    showAppSnackBar(context, 'Profile editing is coming soon.'),
              ),
              const SizedBox(height: 20),
              const _ImpactCard(),
              const SizedBox(height: 17),
              Container(
                clipBehavior: Clip.antiAlias,
                decoration: BoxDecoration(
                  color: AppColors.white,
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(color: AppColors.divider),
                ),
                child: Column(
                  children: <Widget>[
                    for (int i = 0; i < _menu.length; i++)
                      _MenuRow(
                        item: _menu[i],
                        showDivider: i < _menu.length - 1,
                        onTap: () => showAppSnackBar(
                          context,
                          '${_menu[i].label} is coming soon.',
                        ),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              _LogoutTile(onTap: () => logout(context)),
            ],
          ),
        ),
      ),
    );
  }
}

class _MenuItem {
  const _MenuItem(this.icon, this.label);

  final IconData icon;
  final String label;
}

class _ProfileHead extends StatelessWidget {
  const _ProfileHead({required this.onEdit});

  final VoidCallback onEdit;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        Container(
          width: 64,
          height: 64,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: AppColors.forest,
            borderRadius: BorderRadius.circular(21),
          ),
          child: Text(
            'NM',
            style: AppTypography.style(
              size: 19,
              weight: FontWeight.w800,
              color: AppColors.white,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(
                AppStore.customerName,
                style: AppTypography.style(
                  size: 18,
                  weight: FontWeight.w800,
                  letterSpacing: -0.4,
                  color: AppColors.forestDeep,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                AppStore.customerEmail,
                style: AppTypography.style(size: 11.5, color: AppColors.muted),
              ),
            ],
          ),
        ),
        Material(
          color: AppColors.white,
          borderRadius: BorderRadius.circular(13),
          child: InkWell(
            borderRadius: BorderRadius.circular(13),
            onTap: onEdit,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(13),
                border: Border.all(color: AppColors.divider),
              ),
              child: Text(
                'Edit',
                style: AppTypography.style(
                  size: 11.5,
                  weight: FontWeight.w700,
                  color: AppColors.forest,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _ImpactCard extends StatelessWidget {
  const _ImpactCard();

  @override
  Widget build(BuildContext context) {
    final Color soft = AppColors.sageTint.withValues(alpha: 0.75);

    return Container(
      padding: const EdgeInsets.all(19),
      decoration: BoxDecoration(
        color: AppColors.forest,
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'YOUR IMPACT',
            style: AppTypography.style(
              size: 9,
              letterSpacing: 1.3,
              color: soft,
            ),
          ),
          const SizedBox(height: 5),
          Text(
            'Small choices.\nBig difference.',
            style: AppTypography.style(
              size: 19,
              weight: FontWeight.w800,
              height: 1.2,
              letterSpacing: -0.4,
              color: AppColors.white,
            ),
          ),
          const SizedBox(height: 16),
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                const _ImpactStat(value: '18', label: 'meals rescued'),
                _divider(),
                const _ImpactStat(value: '€72', label: 'money saved'),
                _divider(),
                const _ImpactStat(value: '21kg', label: 'CO₂e avoided'),
              ],
            ),
          ),
          const SizedBox(height: 17),
          const _LevelBar(),
        ],
      ),
    );
  }

  Widget _divider() => Container(
    width: 1,
    margin: const EdgeInsets.only(right: 9),
    color: AppColors.white.withValues(alpha: 0.15),
  );
}

class _ImpactStat extends StatelessWidget {
  const _ImpactStat({required this.value, required this.label});

  final String value;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Padding(
        padding: const EdgeInsets.only(right: 4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Text(
              value,
              style: AppTypography.style(
                size: 18,
                weight: FontWeight.w800,
                letterSpacing: -0.4,
                color: AppColors.white,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              label,
              style: AppTypography.style(
                size: 9.5,
                color: AppColors.sageTint.withValues(alpha: 0.8),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LevelBar extends StatelessWidget {
  const _LevelBar();

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: <Widget>[
        Align(
          alignment: Alignment.centerRight,
          child: Text(
            'Eco Hero · Level 4',
            style: AppTypography.style(
              size: 9.5,
              color: AppColors.sageTint.withValues(alpha: 0.9),
            ),
          ),
        ),
        const SizedBox(height: 6),
        ClipRRect(
          borderRadius: BorderRadius.circular(9),
          child: LinearProgressIndicator(
            value: 0.68,
            minHeight: 7,
            backgroundColor: AppColors.white.withValues(alpha: 0.15),
            valueColor: const AlwaysStoppedAnimation<Color>(AppColors.amber),
          ),
        ),
      ],
    );
  }
}

class _LogoutTile extends StatelessWidget {
  const _LogoutTile({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.white,
      borderRadius: BorderRadius.circular(16),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.divider),
          ),
          child: Row(
            children: <Widget>[
              Container(
                width: 33,
                height: 33,
                decoration: BoxDecoration(
                  color: AppColors.danger.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(
                  Icons.logout_rounded,
                  size: 17,
                  color: AppColors.danger,
                ),
              ),
              const SizedBox(width: 11),
              Text(
                'Log out',
                style: AppTypography.style(
                  size: 12.5,
                  weight: FontWeight.w700,
                  color: AppColors.danger,
                ),
              ),
              const Spacer(),
              Text(
                'Switch role',
                style: AppTypography.style(size: 10, color: AppColors.muted),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MenuRow extends StatelessWidget {
  const _MenuRow({
    required this.item,
    required this.showDivider,
    required this.onTap,
  });

  final _MenuItem item;
  final bool showDivider;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 11),
        decoration: BoxDecoration(
          border: showDivider
              ? const Border(bottom: BorderSide(color: AppColors.divider))
              : null,
        ),
        child: Row(
          children: <Widget>[
            Container(
              width: 33,
              height: 33,
              decoration: BoxDecoration(
                color: AppColors.sageTint,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(item.icon, size: 18, color: AppColors.forest),
            ),
            const SizedBox(width: 11),
            Expanded(
              child: Text(
                item.label,
                style: AppTypography.style(
                  size: 12.5,
                  weight: FontWeight.w500,
                  color: const Color(0xFF38413B),
                ),
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
    );
  }
}
