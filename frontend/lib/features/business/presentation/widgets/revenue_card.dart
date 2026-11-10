import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';

/// Dark summary card with today's revenue and a compact weekly chart.
class RevenueCard extends StatelessWidget {
  const RevenueCard({super.key});

  static const List<double> _barFractions = <double>[
    0.42,
    0.58,
    0.5,
    0.74,
    0.56,
    0.86,
    1,
  ];
  static const List<String> _dayLabels = <String>[
    'M',
    'T',
    'W',
    'T',
    'F',
    'S',
    'S',
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 20, 20, 16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: <Color>[AppColors.forest, AppColors.forestDeep],
        ),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: AppColors.cardShadow,
            blurRadius: 24,
            offset: Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Text(
                "TODAY'S REVENUE",
                style: AppTypography.style(
                  size: 10,
                  weight: FontWeight.w700,
                  letterSpacing: 1.3,
                  color: AppColors.mist,
                ),
              ),
              const Spacer(),
              Container(
                padding: const EdgeInsets.fromLTRB(7, 4, 9, 4),
                decoration: BoxDecoration(
                  color: AppColors.white.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    const Icon(
                      Icons.arrow_upward_rounded,
                      size: 11,
                      color: AppColors.amber,
                    ),
                    const SizedBox(width: 2),
                    Text(
                      '18%',
                      style: AppTypography.style(
                        size: 10.5,
                        weight: FontWeight.w700,
                        color: AppColors.amberSoft,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            '€284.40',
            style: AppTypography.style(
              size: 34,
              weight: FontWeight.w800,
              letterSpacing: -1.2,
              height: 1.05,
              color: AppColors.white,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Paid out from 24 rescues this week',
            style: AppTypography.style(
              size: 11.5,
              weight: FontWeight.w500,
              color: AppColors.mist,
            ),
          ),
          const SizedBox(height: 18),
          const _RevenueChart(
            barFractions: _barFractions,
            dayLabels: _dayLabels,
          ),
        ],
      ),
    );
  }
}

class _RevenueChart extends StatelessWidget {
  const _RevenueChart({required this.barFractions, required this.dayLabels});

  final List<double> barFractions;
  final List<String> dayLabels;

  @override
  Widget build(BuildContext context) {
    const double maxBarHeight = 52;
    final int lastIndex = barFractions.length - 1;

    return SizedBox(
      height: 80,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: <Widget>[
          for (int i = 0; i < barFractions.length; i++)
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: <Widget>[
                    Container(
                      height: 6 + (maxBarHeight * barFractions[i]),
                      decoration: BoxDecoration(
                        color: i == lastIndex
                            ? AppColors.amber
                            : AppColors.white.withValues(alpha: 0.24),
                        borderRadius: const BorderRadius.vertical(
                          top: Radius.circular(5),
                          bottom: Radius.circular(5),
                        ),
                      ),
                    ),
                    const SizedBox(height: 7),
                    Text(
                      dayLabels[i],
                      style: AppTypography.style(
                        size: 9.5,
                        weight: FontWeight.w600,
                        color: i == lastIndex
                            ? AppColors.amberSoft
                            : AppColors.mist,
                      ),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}
