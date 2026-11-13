import 'package:flutter/material.dart';

import '../../../../core/auth/logout.dart';
import '../../../../core/store/app_store.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_snack_bar.dart';

/// Business profile tab: account identity, quick stats and logout.
class BusinessProfilePage extends StatelessWidget {
  const BusinessProfilePage({super.key});

  static const List<_ProfileEntry> _entries = <_ProfileEntry>[
    _ProfileEntry(Icons.storefront_outlined, 'Business details'),
    _ProfileEntry(Icons.schedule_rounded, 'Opening hours'),
    _ProfileEntry(Icons.account_balance_wallet_outlined, 'Payout settings'),
    _ProfileEntry(Icons.auto_awesome_outlined, 'AI preferences'),
  ];

  @override
  Widget build(BuildContext context) {
    final AppStore store = AppStoreScope.of(context);

    return SafeArea(
      bottom: false,
      child: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 460),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 110),
            children: <Widget>[
              Text(
                'ACCOUNT',
                style: AppTypography.style(
                  size: 10,
                  weight: FontWeight.w700,
                  letterSpacing: 1.4,
                  color: AppColors.olive,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                AppStore.businessName,
                style: AppTypography.style(
                  size: 22,
                  weight: FontWeight.w800,
                  letterSpacing: -0.6,
                  color: AppColors.ink,
                ),
              ),
              const SizedBox(height: 16),
              const _BusinessIdentityCard(),
              const SizedBox(height: 16),
              _StoreStats(
                offers: store.offers.length,
                activeOrders: store.activeOrders.length,
                completedOrders: store.completedOrders.length,
              ),
              const SizedBox(height: 18),
              Container(
                clipBehavior: Clip.antiAlias,
                decoration: BoxDecoration(
                  color: AppColors.white,
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(color: AppColors.divider),
                ),
                child: Column(
                  children: <Widget>[
                    for (int i = 0; i < _entries.length; i++)
                      _ProfileRow(
                        entry: _entries[i],
                        showDivider: i < _entries.length - 1,
                        onTap: () => showAppSnackBar(
                          context,
                          '${_entries[i].label} is coming soon.',
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

class _ProfileEntry {
  const _ProfileEntry(this.icon, this.label);

  final IconData icon;
  final String label;
}

class _BusinessIdentityCard extends StatelessWidget {
  const _BusinessIdentityCard();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(17),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: <Color>[AppColors.forest, AppColors.forestDeep],
        ),
        borderRadius: BorderRadius.circular(22),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: AppColors.cardShadow,
            blurRadius: 20,
            offset: Offset(0, 9),
          ),
        ],
      ),
      child: Row(
        children: <Widget>[
          Container(
            width: 58,
            height: 58,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppColors.white.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(19),
              border: Border.all(
                color: AppColors.amber.withValues(alpha: 0.55),
                width: 1.4,
              ),
            ),
            child: Text(
              'F&F',
              style: AppTypography.style(
                size: 15,
                weight: FontWeight.w800,
                letterSpacing: 0.5,
                color: AppColors.amberSoft,
              ),
            ),
          ),
          const SizedBox(width: 13),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  AppStore.businessName,
                  style: AppTypography.style(
                    size: 17,
                    weight: FontWeight.w800,
                    letterSpacing: -0.35,
                    color: AppColors.white,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  AppStore.businessEmail,
                  style: AppTypography.style(
                    size: 11.5,
                    color: AppColors.sageTint.withValues(alpha: 0.82),
                  ),
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.white.withValues(alpha: 0.14),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      const Icon(
                        Icons.verified_rounded,
                        size: 11,
                        color: AppColors.amberSoft,
                      ),
                      const SizedBox(width: 3),
                      Text(
                        'VERIFIED PARTNER',
                        style: AppTypography.style(
                          size: 8,
                          weight: FontWeight.w800,
                          letterSpacing: 0.65,
                          color: AppColors.sageTint,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _StoreStats extends StatelessWidget {
  const _StoreStats({
    required this.offers,
    required this.activeOrders,
    required this.completedOrders,
  });

  final int offers;
  final int activeOrders;
  final int completedOrders;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 15),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.divider),
      ),
      child: IntrinsicHeight(
        child: Row(
          children: <Widget>[
            _Stat(value: '$offers', label: 'offers'),
            _divider(),
            _Stat(value: '$activeOrders', label: 'active orders'),
            _divider(),
            _Stat(value: '$completedOrders', label: 'completed'),
          ],
        ),
      ),
    );
  }

  Widget _divider() => Container(width: 1, color: AppColors.divider);
}

class _Stat extends StatelessWidget {
  const _Stat({required this.value, required this.label});

  final String value;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        children: <Widget>[
          Text(
            value,
            style: AppTypography.style(
              size: 20,
              weight: FontWeight.w800,
              letterSpacing: -0.5,
              color: AppColors.forest,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: AppTypography.style(
              size: 9.5,
              weight: FontWeight.w500,
              color: AppColors.muted,
            ),
          ),
        ],
      ),
    );
  }
}

class _ProfileRow extends StatelessWidget {
  const _ProfileRow({
    required this.entry,
    required this.showDivider,
    required this.onTap,
  });

  final _ProfileEntry entry;
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
              child: Icon(entry.icon, size: 18, color: AppColors.forest),
            ),
            const SizedBox(width: 11),
            Expanded(
              child: Text(
                entry.label,
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
