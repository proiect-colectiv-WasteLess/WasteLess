import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../domain/models/business_order.dart';

/// Confirmation sheet that shows the pickup code before completing an order.
Future<bool> showOrderValidationSheet(
  BuildContext context,
  BusinessOrder order,
) async {
  final bool? result = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    backgroundColor: Colors.transparent,
    barrierColor: AppColors.forestDeep.withValues(alpha: 0.45),
    builder: (BuildContext context) => _OrderValidationSheet(order: order),
  );

  return result ?? false;
}

class _OrderValidationSheet extends StatelessWidget {
  const _OrderValidationSheet({required this.order});

  final BusinessOrder order;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      padding: const EdgeInsets.fromLTRB(24, 10, 24, 20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          Center(
            child: Container(
              width: 42,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.divider,
                borderRadius: BorderRadius.circular(999),
              ),
            ),
          ),
          const SizedBox(height: 20),
          Text(
            'Validate pickup',
            textAlign: TextAlign.center,
            style: AppTypography.style(
              size: 18,
              weight: FontWeight.w800,
              letterSpacing: -0.45,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            order.reference,
            textAlign: TextAlign.center,
            style: AppTypography.style(
              size: 9,
              weight: FontWeight.w700,
              letterSpacing: 0.55,
              color: AppColors.olive,
            ),
          ),
          const SizedBox(height: 14),
          Text(
            order.title,
            textAlign: TextAlign.center,
            style: AppTypography.style(
              size: 15,
              weight: FontWeight.w700,
              letterSpacing: -0.25,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            order.pickupLabel,
            textAlign: TextAlign.center,
            style: AppTypography.style(
              size: 10,
              weight: FontWeight.w500,
              color: AppColors.muted,
            ),
          ),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.symmetric(vertical: 14),
            decoration: BoxDecoration(
              color: AppColors.sageTint,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Column(
              children: <Widget>[
                Text(
                  'PICKUP CODE',
                  style: AppTypography.style(
                    size: 8,
                    weight: FontWeight.w800,
                    letterSpacing: 1.1,
                    color: AppColors.forest.withValues(alpha: 0.7),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  order.code,
                  style: AppTypography.style(
                    size: 28,
                    weight: FontWeight.w800,
                    letterSpacing: 8,
                    color: AppColors.forest,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          Row(
            children: <Widget>[
              Expanded(
                child: OutlinedButton(
                  onPressed: () => Navigator.of(context).pop(false),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.muted,
                    side: const BorderSide(color: AppColors.divider),
                    minimumSize: const Size.fromHeight(46),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(13),
                    ),
                    textStyle: AppTypography.style(
                      size: 13,
                      weight: FontWeight.w700,
                    ),
                  ),
                  child: const Text('Cancel'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: FilledButton(
                  onPressed: () => Navigator.of(context).pop(true),
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.forest,
                    foregroundColor: AppColors.white,
                    minimumSize: const Size.fromHeight(46),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(13),
                    ),
                    textStyle: AppTypography.style(
                      size: 13,
                      weight: FontWeight.w700,
                    ),
                  ),
                  child: const Text('Confirm pickup'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
