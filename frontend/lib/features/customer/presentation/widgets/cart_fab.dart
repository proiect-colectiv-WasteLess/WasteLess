import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';

/// Floating basket shortcut with an item-count badge.
class CartFab extends StatelessWidget {
  const CartFab({super.key, required this.count, required this.onTap});

  final int count;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: 'Open basket, $count items',
      child: SizedBox(
        width: 56,
        height: 56,
        child: Stack(
          clipBehavior: Clip.none,
          children: <Widget>[
            Positioned.fill(
              child: Material(
                color: AppColors.amber,
                borderRadius: BorderRadius.circular(17),
                elevation: 6,
                shadowColor: AppColors.buttonShadow,
                child: InkWell(
                  borderRadius: BorderRadius.circular(17),
                  onTap: onTap,
                  child: const Icon(
                    Icons.shopping_bag_outlined,
                    size: 22,
                    color: AppColors.forest,
                  ),
                ),
              ),
            ),
            Positioned(
              right: -4,
              top: -5,
              child: Container(
                width: 20,
                height: 20,
                alignment: Alignment.center,
                decoration: const BoxDecoration(
                  color: AppColors.forest,
                  shape: BoxShape.circle,
                ),
                child: Text(
                  '$count',
                  style: AppTypography.style(
                    size: 10,
                    weight: FontWeight.w700,
                    color: AppColors.white,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
