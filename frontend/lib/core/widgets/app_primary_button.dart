import 'package:flutter/material.dart';

import '../theme/app_colors.dart';
import '../theme/app_typography.dart';

class AppPrimaryButton extends StatelessWidget {
  const AppPrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.trailingIcon = Icons.chevron_right_rounded,
    this.height = 58,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? trailingIcon;
  final double height;

  @override
  Widget build(BuildContext context) {
    const BorderRadius radius = BorderRadius.all(Radius.circular(17));

    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: radius,
        boxShadow: <BoxShadow>[
          BoxShadow(
            color: AppColors.buttonShadow,
            blurRadius: 22,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Material(
        color: AppColors.amber,
        borderRadius: radius,
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onPressed,
          splashColor: AppColors.white.withValues(alpha: 0.22),
          highlightColor: AppColors.white.withValues(alpha: 0.10),
          child: SizedBox(
            height: height,
            width: double.infinity,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Text(
                  label,
                  style: AppTypography.style(
                    size: 15.5,
                    weight: FontWeight.w700,
                    color: AppColors.forestDeep,
                    letterSpacing: -0.15,
                  ),
                ),
                if (trailingIcon != null) ...<Widget>[
                  const SizedBox(width: 4),
                  Icon(
                    trailingIcon,
                    size: 20,
                    color: AppColors.forestDeep,
                  ),
                  const SizedBox(width: 10),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
