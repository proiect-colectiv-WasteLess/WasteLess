import 'package:flutter/material.dart';

import '../theme/app_colors.dart';
import '../theme/app_typography.dart';

void showAppSnackBar(BuildContext context, String message) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(
      SnackBar(
        content: Text(
          message,
          style: AppTypography.style(
            size: 13.5,
            weight: FontWeight.w500,
            color: AppColors.cream,
          ),
        ),
        duration: const Duration(seconds: 2),
      ),
    );
}
