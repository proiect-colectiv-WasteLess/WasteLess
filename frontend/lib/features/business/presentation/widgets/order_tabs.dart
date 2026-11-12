import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';

/// Two-tab switch for active and completed orders.
class OrderTabs extends StatelessWidget {
  const OrderTabs({
    super.key,
    required this.activeCount,
    required this.selectedIndex,
    required this.onSelected,
  });

  final int activeCount;
  final int selectedIndex;
  final ValueChanged<int> onSelected;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        Row(
          children: <Widget>[
            Expanded(
              child: _OrderTab(
                label: 'Active · $activeCount',
                selected: selectedIndex == 0,
                onTap: () => onSelected(0),
              ),
            ),
            Expanded(
              child: _OrderTab(
                label: 'Completed',
                selected: selectedIndex == 1,
                onTap: () => onSelected(1),
              ),
            ),
          ],
        ),
        const Divider(height: 1, thickness: 1, color: AppColors.divider),
      ],
    );
  }
}

class _OrderTab extends StatelessWidget {
  const _OrderTab({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: SizedBox(
        height: 38,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: AppTypography.style(
                size: 11.5,
                weight: selected ? FontWeight.w800 : FontWeight.w500,
                letterSpacing: -0.15,
                color: selected ? AppColors.forest : AppColors.muted,
              ),
            ),
            const SizedBox(height: 5),
            AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              curve: Curves.easeOut,
              width: selected ? 52 : 0,
              height: 2.5,
              decoration: BoxDecoration(
                color: selected ? AppColors.forest : Colors.transparent,
                borderRadius: BorderRadius.circular(999),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
