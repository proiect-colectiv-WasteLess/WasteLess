import 'package:flutter/material.dart';

import '../theme/app_colors.dart';

class AppBackButton extends StatelessWidget {
  const AppBackButton({super.key, this.onPressed});

  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      shape: const CircleBorder(),
      child: InkWell(
        onTap: onPressed ?? () => Navigator.of(context).maybePop(),
        customBorder: const CircleBorder(),
        splashColor: AppColors.forest.withValues(alpha: 0.08),
        highlightColor: AppColors.forest.withValues(alpha: 0.04),
        child: const SizedBox(
          width: 44,
          height: 44,
          child: Icon(
            Icons.arrow_back_ios_new_rounded,
            size: 17,
            color: AppColors.ink,
          ),
        ),
      ),
    );
  }
}
