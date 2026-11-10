import 'package:flutter/material.dart';

import '../theme/app_colors.dart';
import '../theme/app_typography.dart';

class AppLogo extends StatelessWidget {
  const AppLogo({
    super.key,
    this.onDark = true,
    this.markSize = 34,
    this.gap = 10,
  });

  /// Whether the wordmark sits on a dark surface.
  final bool onDark;

  final double markSize;
  final double gap;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Container(
          width: markSize,
          height: markSize,
          decoration: BoxDecoration(
            color: AppColors.amber,
            borderRadius: BorderRadius.circular(markSize * 0.3),
            boxShadow: <BoxShadow>[
              BoxShadow(
                color: AppColors.amber.withValues(alpha: 0.28),
                blurRadius: 14,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Transform.rotate(
            angle: -0.18,
            child: Icon(
              Icons.eco_rounded,
              color: AppColors.white,
              size: markSize * 0.58,
            ),
          ),
        ),
        SizedBox(width: gap),
        Text(
          'WasteLess',
          style: AppTypography.style(
            size: markSize * 0.6,
            weight: FontWeight.w800,
            letterSpacing: -0.6,
            color: onDark ? AppColors.white : AppColors.forest,
            height: 1,
            shadows: onDark
                ? const <Shadow>[
                    Shadow(
                      color: Color(0x4D000000),
                      blurRadius: 10,
                      offset: Offset(0, 2),
                    ),
                  ]
                : null,
          ),
        ),
      ],
    );
  }
}
