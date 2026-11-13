import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';

/// Section heading with an optional trailing text action ("See all").
class SectionTitle extends StatelessWidget {
  const SectionTitle({
    super.key,
    required this.title,
    this.actionLabel,
    this.onAction,
    this.showLiveDot = false,
  });

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;
  final bool showLiveDot;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 22, bottom: 12),
      child: Row(
        children: <Widget>[
          Text(
            title,
            style: AppTypography.style(
              size: 17,
              weight: FontWeight.w800,
              letterSpacing: -0.4,
              color: AppColors.ink,
            ),
          ),
          if (showLiveDot) ...<Widget>[
            const SizedBox(width: 6),
            Container(
              width: 7,
              height: 7,
              decoration: const BoxDecoration(
                color: AppColors.alert,
                shape: BoxShape.circle,
              ),
            ),
          ],
          const Spacer(),
          if (actionLabel != null)
            GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: onAction,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Text(
                  actionLabel!,
                  style: AppTypography.style(
                    size: 12.5,
                    weight: FontWeight.w700,
                    color: AppColors.forest,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
