import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'app_colors.dart';
import 'app_typography.dart';

abstract final class AppTheme {
  static ThemeData get light {
    final ColorScheme colorScheme = ColorScheme.fromSeed(
      seedColor: AppColors.forest,
      brightness: Brightness.light,
    ).copyWith(
      primary: AppColors.forest,
      onPrimary: AppColors.white,
      secondary: AppColors.amber,
      onSecondary: AppColors.forestDeep,
      surface: AppColors.cream,
      onSurface: AppColors.ink,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: AppColors.cream,
      fontFamily: AppTypography.family,
      textTheme: AppTypography.textTheme,
      splashFactory: InkRipple.splashFactory,
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.cream,
        foregroundColor: AppColors.ink,
        elevation: 0,
        scrolledUnderElevation: 0,
        systemOverlayStyle: SystemUiOverlayStyle.dark,
        titleTextStyle: AppTypography.style(
          size: 18,
          weight: FontWeight.w800,
          color: AppColors.ink,
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: AppColors.forest,
        contentTextStyle: AppTypography.style(
          size: 13.5,
          weight: FontWeight.w500,
          color: AppColors.cream,
        ),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
        ),
        insetPadding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
      ),
    );
  }
}
