import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_back_button.dart';

/// Centered page title with an optional back button, as used across the
/// customer screens.
class CustomerPageHeader extends StatelessWidget {
  const CustomerPageHeader({
    super.key,
    required this.title,
    this.showBack = false,
    this.onBack,
  });

  final String title;
  final bool showBack;
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 44,
      child: Row(
        children: <Widget>[
          SizedBox(
            width: 44,
            child: showBack ? AppBackButton(onPressed: onBack) : null,
          ),
          Expanded(
            child: Text(
              title,
              textAlign: TextAlign.center,
              style: AppTypography.style(
                size: 18,
                weight: FontWeight.w800,
                color: AppColors.ink,
                letterSpacing: -0.3,
              ),
            ),
          ),
          const SizedBox(width: 44),
        ],
      ),
    );
  }
}
