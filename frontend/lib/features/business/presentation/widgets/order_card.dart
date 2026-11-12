import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../domain/models/business_order.dart';

/// A single customer order with its pickup code and validation action.
class OrderCard extends StatelessWidget {
  const OrderCard({super.key, required this.order, this.onValidate});

  final BusinessOrder order;
  final VoidCallback? onValidate;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.divider),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: AppColors.cardShadow,
            blurRadius: 12,
            offset: Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          Row(
            children: <Widget>[
              Text(
                order.reference,
                style: AppTypography.style(
                  size: 8,
                  weight: FontWeight.w700,
                  letterSpacing: 0.55,
                  color: AppColors.olive,
                ),
              ),
              const Spacer(),
              Text(
                order.timeAgo,
                style: AppTypography.style(
                  size: 8,
                  weight: FontWeight.w500,
                  color: AppColors.muted,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            order.title,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: AppTypography.style(
              size: 12.5,
              weight: FontWeight.w800,
              letterSpacing: -0.25,
              color: AppColors.forest,
            ),
          ),
          const SizedBox(height: 3),
          Text(
            order.pickupLabel,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: AppTypography.style(
              size: 9,
              weight: FontWeight.w500,
              color: AppColors.muted,
            ),
          ),
          const SizedBox(height: 11),
          Row(
            children: <Widget>[
              Text(
                order.priceLabel,
                style: AppTypography.style(
                  size: 16,
                  weight: FontWeight.w800,
                  letterSpacing: -0.55,
                  color: AppColors.forest,
                ),
              ),
              const Spacer(),
              if (order.isCompleted)
                const _CollectedPill()
              else
                _ValidateButton(onPressed: onValidate),
            ],
          ),
        ],
      ),
    );
  }
}

class _ValidateButton extends StatelessWidget {
  const _ValidateButton({required this.onPressed});

  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.forest,
      borderRadius: BorderRadius.circular(999),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onPressed,
        splashColor: AppColors.white.withValues(alpha: 0.14),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 7.5),
          child: Text(
            'Validate code / QR',
            style: AppTypography.style(
              size: 8.5,
              weight: FontWeight.w700,
              letterSpacing: -0.05,
              color: AppColors.white,
            ),
          ),
        ),
      ),
    );
  }
}

class _CollectedPill extends StatelessWidget {
  const _CollectedPill();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: AppColors.sageTint,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          const Icon(Icons.check_rounded, size: 11, color: AppColors.forest),
          const SizedBox(width: 3),
          Text(
            'Collected',
            style: AppTypography.style(
              size: 8,
              weight: FontWeight.w700,
              color: AppColors.forest,
            ),
          ),
        ],
      ),
    );
  }
}
