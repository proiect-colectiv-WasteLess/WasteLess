import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../domain/models/deal.dart';

/// Compact horizontal deal card: photo with discount badge on the left,
/// shop, name, price and pickup meta on the right.
class DealCard extends StatelessWidget {
  const DealCard({super.key, required this.deal, required this.onTap});

  final Deal deal;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final BorderRadius radius = BorderRadius.circular(20);

    return DecoratedBox(
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: radius,
        border: Border.all(color: AppColors.divider),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: AppColors.cardShadow,
            blurRadius: 18,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: radius,
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                SizedBox(
                  width: 106,
                  child: Stack(
                    fit: StackFit.expand,
                    children: <Widget>[
                      Image.asset(
                        deal.imageAsset,
                        fit: BoxFit.cover,
                        cacheWidth: 260,
                        semanticLabel: deal.name,
                      ),
                      Positioned(
                        left: 8,
                        top: 8,
                        child: DiscountBadge(label: deal.discountLabel),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Row(
                          children: <Widget>[
                            Expanded(
                              child: Text(
                                deal.shop,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: AppTypography.style(
                                  size: 11,
                                  color: AppColors.muted,
                                ),
                              ),
                            ),
                            const Icon(
                              Icons.star_rounded,
                              size: 13,
                              color: AppColors.gold,
                            ),
                            const SizedBox(width: 2),
                            Text(
                              deal.rating.toStringAsFixed(1),
                              style: AppTypography.style(
                                size: 11,
                                weight: FontWeight.w600,
                                color: AppColors.amberInk,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          deal.name,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: AppTypography.style(
                            size: 14.5,
                            weight: FontWeight.w800,
                            height: 1.2,
                            letterSpacing: -0.25,
                            color: AppColors.ink,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Row(
                          children: <Widget>[
                            Text(
                              deal.priceLabel,
                              style: AppTypography.style(
                                size: 16,
                                weight: FontWeight.w800,
                                letterSpacing: -0.3,
                                color: AppColors.forest,
                              ),
                            ),
                            const SizedBox(width: 7),
                            Text(
                              deal.oldPriceLabel,
                              style: AppTypography.style(
                                size: 11.5,
                                color: AppColors.mist,
                                decoration: TextDecoration.lineThrough,
                              ),
                            ),
                            const Spacer(),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 6,
                                vertical: 3,
                              ),
                              decoration: BoxDecoration(
                                color: AppColors.peachTint,
                                borderRadius: BorderRadius.circular(7),
                              ),
                              child: Text(
                                '${deal.left} left',
                                style: AppTypography.style(
                                  size: 10,
                                  weight: FontWeight.w700,
                                  color: AppColors.danger,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        const Divider(height: 1, color: AppColors.divider),
                        const SizedBox(height: 7),
                        Row(
                          children: <Widget>[
                            _Meta(
                              icon: Icons.location_on_outlined,
                              label: deal.distanceLabel,
                            ),
                            const Spacer(),
                            _Meta(
                              icon: Icons.access_time_rounded,
                              label: deal.pickupWindow,
                            ),
                          ],
                        ),
                      ],
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

class _Meta extends StatelessWidget {
  const _Meta({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Icon(icon, size: 13, color: AppColors.muted),
        const SizedBox(width: 3),
        Text(
          label,
          style: AppTypography.style(size: 10.5, color: AppColors.muted),
        ),
      ],
    );
  }
}

/// Amber pill showing the percentage saved.
class DiscountBadge extends StatelessWidget {
  const DiscountBadge({super.key, required this.label, this.large = false});

  final String label;
  final bool large;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: large ? 11 : 8,
        vertical: large ? 8 : 6,
      ),
      decoration: BoxDecoration(
        color: AppColors.amber,
        borderRadius: BorderRadius.circular(large ? 11 : 9),
      ),
      child: Text(
        label,
        style: AppTypography.style(
          size: large ? 14 : 11,
          weight: FontWeight.w800,
          color: AppColors.forestDeep,
        ),
      ),
    );
  }
}
