import 'package:flutter/material.dart';

import '../../../../core/router/app_routes.dart';
import '../../../../core/store/app_store.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_snack_bar.dart';
import '../../domain/models/deal.dart';
import '../widgets/customer_search_field.dart';
import '../widgets/deal_card.dart';
import '../widgets/section_title.dart';
import '../../../../core/constants/app_assets.dart';

class CustomerHomePage extends StatefulWidget {
  const CustomerHomePage({super.key, required this.onOpenExplore});

  /// Switches the shell to the Explore tab.
  final VoidCallback onOpenExplore;

  @override
  State<CustomerHomePage> createState() => _CustomerHomePageState();
}

class _CustomerHomePageState extends State<CustomerHomePage> {
  static const List<_Category> _categories = <_Category>[
    _Category('All', '✦'),
    _Category('Bakery', '\u{1F950}'),
    _Category('Meals', '\u{1F957}'),
    _Category('Groceries', '\u{1F955}'),
    _Category('Desserts', '\u{1F370}'),
    _Category('Drinks', '☕'),
  ];

  String _query = '';
  String _category = 'All';

  void _openDeal(Deal deal) {
    Navigator.of(context).pushNamed(AppRoutes.customerProduct, arguments: deal);
  }

  @override
  Widget build(BuildContext context) {
    final List<Deal> deals = AppStoreScope.of(context).deals;
    final String query = _query.trim().toLowerCase();
    final List<Deal> filtered = deals
        .where(
          (Deal d) =>
              d.name.toLowerCase().contains(query) ||
              d.shop.toLowerCase().contains(query),
        )
        .toList();

    return SafeArea(
      bottom: false,
      child: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 460),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 14, 20, 110),
            children: <Widget>[
              _HomeHeader(
                onChangeLocation: () =>
                    showAppSnackBar(context, 'Location picker is coming soon.'),
                onNotifications: () => Navigator.of(
                  context,
                ).pushNamed(AppRoutes.customerNotifications),
              ),
              const SizedBox(height: 18),
              CustomerSearchField(
                hint: 'Find delicious deals near you...',
                onChanged: (String value) => setState(() => _query = value),
              ),
              const SizedBox(height: 18),
              _PromoBanner(onTap: widget.onOpenExplore),
              const SectionTitle(title: 'What are you craving?'),
              SizedBox(
                height: 82,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  clipBehavior: Clip.none,
                  itemCount: _categories.length,
                  separatorBuilder: (_, _) => const SizedBox(width: 10),
                  itemBuilder: (BuildContext context, int i) {
                    final _Category c = _categories[i];
                    return _CategoryChip(
                      category: c,
                      selected: c.label == _category,
                      onTap: () => setState(() => _category = c.label),
                    );
                  },
                ),
              ),
              SectionTitle(
                title: 'Closest to you',
                showLiveDot: true,
                actionLabel: 'See all',
                onAction: widget.onOpenExplore,
              ),
              if (deals.isEmpty)
                const _NoDeals()
              else ...<Widget>[
                for (final Deal deal in filtered.take(3)) ...<Widget>[
                  DealCard(deal: deal, onTap: () => _openDeal(deal)),
                  const SizedBox(height: 11),
                ],
                SectionTitle(
                  title: 'Almost gone!',
                  actionLabel: 'See all',
                  onAction: widget.onOpenExplore,
                ),
                for (final Deal deal in deals.skip(1).take(2)) ...<Widget>[
                  DealCard(deal: deal, onTap: () => _openDeal(deal)),
                  const SizedBox(height: 10),
                ],
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _NoDeals extends StatelessWidget {
  const _NoDeals();

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 26),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.divider),
      ),
      child: Column(
        children: <Widget>[
          Container(
            width: 48,
            height: 48,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppColors.sageTint,
              borderRadius: BorderRadius.circular(15),
            ),
            child: const Icon(
              Icons.storefront_outlined,
              size: 23,
              color: AppColors.forest,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            'Fresh rescues are on the way',
            textAlign: TextAlign.center,
            style: AppTypography.style(
              size: 15,
              weight: FontWeight.w800,
              letterSpacing: -0.25,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'New offers from local businesses will appear here as soon as '
            'they are published.',
            textAlign: TextAlign.center,
            style: AppTypography.style(
              size: 12,
              height: 1.45,
              color: AppColors.muted,
            ),
          ),
        ],
      ),
    );
  }
}

class _HomeHeader extends StatelessWidget {
  const _HomeHeader({
    required this.onChangeLocation,
    required this.onNotifications,
  });

  final VoidCallback onChangeLocation;
  final VoidCallback onNotifications;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: <Widget>[
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(
                'GOOD AFTERNOON',
                style: AppTypography.style(
                  size: 10,
                  weight: FontWeight.w700,
                  letterSpacing: 1.3,
                  color: AppColors.muted,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'Hey, hungry human! \u{1F44B}',
                style: AppTypography.style(
                  size: 21,
                  weight: FontWeight.w800,
                  letterSpacing: -0.6,
                  color: AppColors.ink,
                ),
              ),
              const SizedBox(height: 5),
              Row(
                children: <Widget>[
                  const Icon(
                    Icons.location_on_outlined,
                    size: 15,
                    color: AppColors.muted,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    'Cluj-Napoca, Romania',
                    style: AppTypography.style(
                      size: 12,
                      color: AppColors.muted,
                    ),
                  ),
                  const SizedBox(width: 8),
                  GestureDetector(
                    onTap: onChangeLocation,
                    child: Text(
                      'Change',
                      style: AppTypography.style(
                        size: 11,
                        weight: FontWeight.w800,
                        color: AppColors.forest,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        _BellButton(onTap: onNotifications),
      ],
    );
  }
}

class _BellButton extends StatelessWidget {
  const _BellButton({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: 'Notifications',
      child: Material(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(14),
        elevation: 0,
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: onTap,
          child: Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.divider),
            ),
            child: Stack(
              alignment: Alignment.center,
              children: <Widget>[
                const Icon(
                  Icons.notifications_none_rounded,
                  size: 22,
                  color: AppColors.forest,
                ),
                Positioned(
                  right: 11,
                  top: 10,
                  child: Container(
                    width: 9,
                    height: 9,
                    decoration: BoxDecoration(
                      color: AppColors.alert,
                      shape: BoxShape.circle,
                      border: Border.all(color: AppColors.white, width: 2),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _PromoBanner extends StatelessWidget {
  const _PromoBanner({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final BorderRadius radius = BorderRadius.circular(24);

    return Material(
      color: AppColors.forest,
      borderRadius: radius,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: SizedBox(
          height: 164,
          child: Stack(
            children: <Widget>[
              Positioned(
                right: -22,
                top: 14,
                child: SizedBox(
                  width: 160,
                  height: 160,
                  child: Stack(
                    clipBehavior: Clip.none,
                    children: <Widget>[
                      Container(
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          border: Border.all(
                            color: AppColors.white.withValues(alpha: 0.08),
                            width: 8,
                          ),
                        ),
                        child: ClipOval(
                          child: Image.asset(
                            AppAssets.dealBowl,
                            fit: BoxFit.cover,
                            width: 160,
                            height: 160,
                            cacheWidth: 320,
                            semanticLabel: 'Colorful lunch bowl',
                          ),
                        ),
                      ),
                      Positioned(
                        left: -5,
                        bottom: 3,
                        child: Transform.rotate(
                          angle: -0.087,
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 8,
                            ),
                            decoration: BoxDecoration(
                              color: AppColors.amber,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text(
                              '€4.50',
                              style: AppTypography.style(
                                size: 13,
                                weight: FontWeight.w800,
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
              Padding(
                padding: const EdgeInsets.all(22),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Text(
                      'RESCUE. SAVE. ENJOY.',
                      style: AppTypography.style(
                        size: 9,
                        weight: FontWeight.w800,
                        letterSpacing: 1.3,
                        color: AppColors.sageTint.withValues(alpha: 0.8),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Good food.\nBetter prices.',
                      style: AppTypography.style(
                        size: 23,
                        weight: FontWeight.w800,
                        height: 1.08,
                        letterSpacing: -0.7,
                        color: AppColors.white,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Up to 70% off nearby',
                      style: AppTypography.style(
                        size: 12,
                        color: AppColors.sageTint.withValues(alpha: 0.85),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Category {
  const _Category(this.label, this.emoji);

  final String label;
  final String emoji;
}

class _CategoryChip extends StatelessWidget {
  const _CategoryChip({
    required this.category,
    required this.selected,
    required this.onTap,
  });

  final _Category category;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: SizedBox(
        width: 56,
        child: Column(
          children: <Widget>[
            AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              width: 52,
              height: 52,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: selected ? AppColors.forest : AppColors.white,
                borderRadius: BorderRadius.circular(17),
                border: Border.all(
                  color: selected ? AppColors.forest : AppColors.divider,
                ),
              ),
              child: Text(
                category.emoji,
                style: TextStyle(
                  fontSize: 22,
                  color: selected ? AppColors.white : AppColors.ink,
                ),
              ),
            ),
            const SizedBox(height: 7),
            Text(
              category.label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: AppTypography.style(
                size: 11,
                weight: selected ? FontWeight.w700 : FontWeight.w500,
                color: selected ? AppColors.forest : AppColors.muted,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
