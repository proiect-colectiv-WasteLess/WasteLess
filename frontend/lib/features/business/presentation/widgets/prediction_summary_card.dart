import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';

/// Dark card that rolls today's predictions into three headline numbers.
class PredictionSummaryCard extends StatelessWidget {
  const PredictionSummaryCard({super.key, required this.predictedSurplus});

  final int predictedSurplus;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
      decoration: BoxDecoration(
        color: AppColors.forest,
        borderRadius: BorderRadius.circular(18),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: AppColors.cardShadow,
            blurRadius: 18,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Row(
        children: <Widget>[
          Expanded(
            child: _SummaryMetric(
              label: 'PREDICTED SURPLUS',
              value: '$predictedSurplus',
              suffix: 'items',
            ),
          ),
          const _SummaryDivider(),
          const Expanded(
            child: _SummaryMetric(label: 'POTENTIAL LOSS', value: '€47.20'),
          ),
          const _SummaryDivider(),
          const Expanded(
            child: _SummaryMetric(label: 'YOU COULD SAVE', value: '€38.60'),
          ),
        ],
      ),
    );
  }
}

class _SummaryDivider extends StatelessWidget {
  const _SummaryDivider();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Container(
        width: 1,
        height: 32,
        color: AppColors.white.withValues(alpha: 0.14),
      ),
    );
  }
}

class _SummaryMetric extends StatelessWidget {
  const _SummaryMetric({required this.label, required this.value, this.suffix});

  final String label;
  final String value;
  final String? suffix;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          label,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: AppTypography.style(
            size: 7,
            weight: FontWeight.w700,
            letterSpacing: 0.65,
            color: AppColors.mist,
          ),
        ),
        const SizedBox(height: 4),
        Text.rich(
          TextSpan(
            children: <InlineSpan>[
              TextSpan(
                text: value,
                style: AppTypography.style(
                  size: 16,
                  weight: FontWeight.w800,
                  letterSpacing: -0.5,
                  color: AppColors.white,
                ),
              ),
              if (suffix != null)
                TextSpan(
                  text: ' $suffix',
                  style: AppTypography.style(
                    size: 8.5,
                    weight: FontWeight.w600,
                    color: AppColors.mist,
                  ),
                ),
            ],
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ],
    );
  }
}
