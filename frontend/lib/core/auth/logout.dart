import 'package:flutter/material.dart';

import '../router/app_routes.dart';
import '../theme/app_colors.dart';
import '../theme/app_typography.dart';

/// Asks the user to confirm and then returns to the welcome screen.
///
/// The shared store lives above the navigator, so all offers, orders and
/// changes stay intact while the user switches roles.
Future<void> logout(BuildContext context) async {
  final bool? confirmed = await showDialog<bool>(
    context: context,
    barrierColor: AppColors.forestDeep.withValues(alpha: 0.45),
    builder: (BuildContext context) => AlertDialog(
      backgroundColor: AppColors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      title: Text(
        'Log out?',
        style: AppTypography.style(
          size: 18,
          weight: FontWeight.w800,
          letterSpacing: -0.4,
        ),
      ),
      content: Text(
        'Your offers and orders are kept until the app closes, so you can '
        'switch roles and pick up where you left off.',
        style: AppTypography.style(
          size: 13.5,
          height: 1.5,
          color: AppColors.muted,
        ),
      ),
      actionsPadding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: Text(
            'Cancel',
            style: AppTypography.style(
              size: 13.5,
              weight: FontWeight.w700,
              color: AppColors.muted,
            ),
          ),
        ),
        FilledButton(
          onPressed: () => Navigator.of(context).pop(true),
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.forest,
            foregroundColor: AppColors.white,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
          ),
          child: Text(
            'Log out',
            style: AppTypography.style(
              size: 13,
              weight: FontWeight.w700,
              color: AppColors.white,
            ),
          ),
        ),
      ],
    ),
  );

  if (!context.mounted || confirmed != true) {
    return;
  }

  Navigator.of(
    context,
  ).pushNamedAndRemoveUntil(AppRoutes.welcome, (Route<dynamic> route) => false);
}
