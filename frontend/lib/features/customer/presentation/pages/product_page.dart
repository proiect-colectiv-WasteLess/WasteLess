import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/router/app_routes.dart';
import '../../../../core/store/app_store.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_primary_button.dart';
import '../../../../core/widgets/app_snack_bar.dart';
import '../../domain/models/deal.dart';
import '../widgets/deal_card.dart';

class ProductPage extends StatelessWidget {
  const ProductPage({super.key, required this.deal});

  final Deal deal;

  void _addToBasket(BuildContext context) {
    AppStoreScope.read(context).addToBasket(deal);
    Navigator.of(context).pushNamed(AppRoutes.customerCart);
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.white,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: Scaffold(
        backgroundColor: AppColors.cream,
        body: Align(
          alignment: Alignment.topCenter,
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: ListView(
              padding: EdgeInsets.zero,
              children: <Widget>[
                _Hero(deal: deal),
                Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Row(
                        children: <Widget>[
                          Expanded(
                            child: Text(
                              deal.shop,
                              style: AppTypography.style(
                                size: 13,
                                weight: FontWeight.w700,
                                color: AppColors.forest,
                              ),
                            ),
                          ),
                          const Icon(
                            Icons.star_rounded,
                            size: 15,
                            color: AppColors.gold,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            '${deal.rating.toStringAsFixed(1)} (248)',
                            style: AppTypography.style(
                              size: 12.5,
                              color: AppColors.amberInk,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 7),
                      Text(
                        deal.name,
                        style: AppTypography.style(
                          size: 26,
                          weight: FontWeight.w800,
                          height: 1.15,
                          letterSpacing: -0.8,
                          color: AppColors.forestDeep,
                        ),
                      ),
                      const SizedBox(height: 10),
                      Row(
                        children: <Widget>[
                          Text(
                            deal.priceLabel,
                            style: AppTypography.style(
                              size: 24,
                              weight: FontWeight.w800,
                              letterSpacing: -0.6,
                              color: AppColors.forest,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            deal.oldPriceLabel,
                            style: AppTypography.style(
                              size: 14,
                              color: AppColors.mist,
                              decoration: TextDecoration.lineThrough,
                            ),
                          ),
                          const Spacer(),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 5,
                            ),
                            decoration: BoxDecoration(
                              color: AppColors.sageTint,
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              'You save ${deal.savingLabel}!',
                              style: AppTypography.style(
                                size: 11,
                                weight: FontWeight.w700,
                                color: const Color(0xFF387747),
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 18),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: <Widget>[
                          _InfoChip(
                            icon: Icons.access_time_rounded,
                            label: deal.pickupWindow,
                          ),
                          _InfoChip(
                            icon: Icons.shopping_bag_outlined,
                            label: 'Only ${deal.left} left',
                          ),
                        ],
                      ),
                      const SizedBox(height: 18),
                      _Block(
                        title: 'What you’ll get',
                        child: _BodyText(
                          deal.description.isNotEmpty
                              ? deal.description
                              : 'A delicious selection of today’s freshly made '
                                    'favorites. The exact mix is a surprise—it’s '
                                    'part of the fun, and always great value.',
                        ),
                      ),
                      _Block(
                        title: 'Pickup location',
                        child: _PickupLocation(
                          deal: deal,
                          onDirections: () => showAppSnackBar(
                            context,
                            'Directions will open in your maps app soon.',
                          ),
                        ),
                      ),
                      const _Block(
                        title: 'Good to know',
                        child: _BodyText(
                          'May contain gluten, dairy, eggs and nuts. Bring '
                          'your own bag to make your rescue even greener.',
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        bottomNavigationBar: _StickyAction(
          deal: deal,
          onAdd: () => _addToBasket(context),
        ),
      ),
    );
  }
}

class _Hero extends StatelessWidget {
  const _Hero({required this.deal});

  final Deal deal;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 330,
      child: Stack(
        fit: StackFit.expand,
        children: <Widget>[
          Image.asset(
            deal.imageAsset,
            fit: BoxFit.cover,
            semanticLabel: deal.name,
          ),
          Positioned(
            left: 18,
            top: MediaQuery.paddingOf(context).top + 12,
            child: _RoundButton(
              icon: Icons.arrow_back_ios_new_rounded,
              iconSize: 17,
              label: 'Back',
              onTap: () => Navigator.of(context).maybePop(),
            ),
          ),
          Positioned(
            right: 18,
            top: MediaQuery.paddingOf(context).top + 12,
            child: _RoundButton(
              icon: Icons.favorite_border_rounded,
              iconSize: 21,
              label: 'Save to favorites',
              onTap: () => showAppSnackBar(context, 'Saved to favorites.'),
            ),
          ),
          Positioned(
            left: 18,
            bottom: 16,
            child: DiscountBadge(label: deal.discountLabel, large: true),
          ),
        ],
      ),
    );
  }
}

class _RoundButton extends StatelessWidget {
  const _RoundButton({
    required this.icon,
    required this.iconSize,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final double iconSize;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: label,
      child: Material(
        color: AppColors.white,
        shape: const CircleBorder(),
        elevation: 3,
        shadowColor: AppColors.cardShadow,
        child: InkWell(
          customBorder: const CircleBorder(),
          onTap: onTap,
          child: SizedBox(
            width: 44,
            height: 44,
            child: Icon(icon, size: iconSize, color: AppColors.forest),
          ),
        ),
      ),
    );
  }
}

class _InfoChip extends StatelessWidget {
  const _InfoChip({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 10),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(11),
        border: Border.all(color: AppColors.divider),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Icon(icon, size: 17, color: AppColors.ink),
          const SizedBox(width: 6),
          Text(
            label,
            style: AppTypography.style(size: 12, color: AppColors.ink),
          ),
        ],
      ),
    );
  }
}

class _Block extends StatelessWidget {
  const _Block({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(top: 4),
      padding: const EdgeInsets.only(top: 17, bottom: 8),
      decoration: const BoxDecoration(
        border: Border(top: BorderSide(color: AppColors.divider)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            title,
            style: AppTypography.style(
              size: 16,
              weight: FontWeight.w800,
              letterSpacing: -0.3,
              color: AppColors.forestDeep,
            ),
          ),
          const SizedBox(height: 8),
          child,
        ],
      ),
    );
  }
}

class _BodyText extends StatelessWidget {
  const _BodyText(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: AppTypography.style(
        size: 13,
        height: 1.55,
        color: AppColors.muted,
      ),
    );
  }
}

class _PickupLocation extends StatelessWidget {
  const _PickupLocation({required this.deal, required this.onDirections});

  final Deal deal;
  final VoidCallback onDirections;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: AppColors.white,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppColors.divider),
          ),
          child: Row(
            children: <Widget>[
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: AppColors.sageTint,
                  borderRadius: BorderRadius.circular(13),
                ),
                child: const Icon(
                  Icons.location_on_outlined,
                  size: 21,
                  color: AppColors.forest,
                ),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      deal.shop,
                      style: AppTypography.style(
                        size: 13,
                        weight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      'Strada Memorandumului 24, Cluj-Napoca',
                      style: AppTypography.style(
                        size: 11,
                        color: AppColors.muted,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '${deal.distanceLabel} away · Today, ${deal.pickupWindow}',
                      style: AppTypography.style(
                        size: 10.5,
                        weight: FontWeight.w700,
                        color: AppColors.forest,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 9),
        SizedBox(
          width: double.infinity,
          height: 42,
          child: OutlinedButton(
            onPressed: onDirections,
            style: OutlinedButton.styleFrom(
              backgroundColor: AppColors.white,
              foregroundColor: AppColors.forest,
              side: const BorderSide(color: AppColors.divider),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(15),
              ),
              textStyle: AppTypography.style(size: 12, weight: FontWeight.w700),
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Text('Get directions'),
                SizedBox(width: 4),
                Icon(Icons.chevron_right_rounded, size: 18),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _StickyAction extends StatelessWidget {
  const _StickyAction({required this.deal, required this.onAdd});

  final Deal deal;
  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        color: AppColors.white,
        border: Border(top: BorderSide(color: AppColors.divider)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(19, 12, 19, 12),
          child: Center(
            heightFactor: 1,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: Row(
                children: <Widget>[
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      Text(
                        'Total',
                        style: AppTypography.style(
                          size: 11,
                          color: AppColors.muted,
                        ),
                      ),
                      Text(
                        deal.priceLabel,
                        style: AppTypography.style(
                          size: 22,
                          weight: FontWeight.w800,
                          letterSpacing: -0.5,
                          color: AppColors.forest,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(width: 18),
                  Expanded(
                    child: AppPrimaryButton(
                      label: 'Add to basket',
                      trailingIcon: Icons.shopping_bag_outlined,
                      onPressed: onAdd,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
