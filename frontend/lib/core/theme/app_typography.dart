import 'package:flutter/material.dart';

import 'app_colors.dart';

abstract final class AppTypography {
  static const String family = 'PlusJakartaSans';

  static TextStyle style({
    required double size,
    FontWeight weight = FontWeight.w400,
    Color color = AppColors.ink,
    double? height,
    double? letterSpacing,
    FontStyle fontStyle = FontStyle.normal,
    TextDecoration decoration = TextDecoration.none,
    List<Shadow>? shadows,
  }) {
    return TextStyle(
      fontFamily: family,
      fontSize: size,
      fontWeight: weight,
      fontVariations: <FontVariation>[
        FontVariation('wght', weight.value.toDouble()),
      ],
      fontStyle: fontStyle,
      height: height,
      letterSpacing: letterSpacing,
      color: color,
      decoration: decoration,
      decorationColor: color,
      shadows: shadows,
    );
  }

  static final TextTheme textTheme = TextTheme(
    displayLarge: style(
      size: 40,
      weight: FontWeight.w800,
      height: 1.04,
      letterSpacing: -1.4,
    ),
    displayMedium: style(
      size: 33,
      weight: FontWeight.w800,
      height: 1.08,
      letterSpacing: -0.8,
    ),
    headlineSmall: style(
      size: 20,
      weight: FontWeight.w800,
      letterSpacing: -0.5,
      height: 1.2,
    ),
    titleLarge: style(
      size: 18,
      weight: FontWeight.w700,
      height: 1.25,
      letterSpacing: -0.3,
    ),
    titleMedium: style(
      size: 16.5,
      weight: FontWeight.w800,
      height: 1.25,
      letterSpacing: -0.25,
    ),
    bodyLarge: style(size: 15, height: 1.5, letterSpacing: -0.1),
    bodyMedium: style(size: 14, height: 1.45, letterSpacing: -0.05),
    bodySmall: style(size: 13, height: 1.4),
    labelLarge: style(
      size: 15.5,
      weight: FontWeight.w700,
      letterSpacing: 0.1,
    ),
    labelMedium: style(
      size: 12,
      weight: FontWeight.w700,
      letterSpacing: 1.4,
    ),
    labelSmall: style(
      size: 11,
      weight: FontWeight.w700,
      letterSpacing: 1.5,
    ),
  );
}
