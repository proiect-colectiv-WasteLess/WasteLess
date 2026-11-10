import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';

/// Soft lavender banner introducing the assistant.
class AiAssistantBanner extends StatelessWidget {
  const AiAssistantBanner({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: <Color>[AppColors.lavenderTint, AppColors.white],
        ),
        border: Border.all(
          color: AppColors.lavenderSoft.withValues(alpha: 0.7),
        ),
      ),
      child: Row(
        children: <Widget>[
          Container(
            width: 38,
            height: 38,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppColors.lavenderSoft,
              borderRadius: BorderRadius.circular(11),
            ),
            child: const Icon(
              Icons.auto_awesome_rounded,
              size: 18,
              color: AppColors.white,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  'YOUR AI FOOD WASTE ASSISTANT',
                  style: AppTypography.style(
                    size: 7.5,
                    weight: FontWeight.w700,
                    letterSpacing: 1.1,
                    color: AppColors.lavenderInk,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  "Good afternoon, let's waste less.",
                  style: AppTypography.style(
                    size: 14.5,
                    weight: FontWeight.w800,
                    letterSpacing: -0.35,
                    height: 1.2,
                    color: AppColors.forest,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
