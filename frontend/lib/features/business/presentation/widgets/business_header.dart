import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';

/// Business identity row with a greeting and a notifications shortcut.
class BusinessHeader extends StatelessWidget {
  const BusinessHeader({
    super.key,
    this.businessName = 'Fresh Bakery',
    this.onNotificationsPressed,
  });

  final String businessName;
  final VoidCallback? onNotificationsPressed;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        const _BusinessAvatar(),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(
                'GOOD AFTERNOON',
                style: AppTypography.style(
                  size: 10,
                  weight: FontWeight.w700,
                  letterSpacing: 1.4,
                  color: AppColors.olive,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                businessName,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppTypography.style(
                  size: 21,
                  weight: FontWeight.w800,
                  letterSpacing: -0.6,
                  height: 1.15,
                  color: AppColors.ink,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 8),
        _NotificationsButton(
          onPressed: onNotificationsPressed,
        ),
      ],
    );
  }
}

class _BusinessAvatar extends StatelessWidget {
  const _BusinessAvatar();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 48,
      height: 48,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: <Color>[AppColors.forest, AppColors.forestDeep],
        ),
        border: Border.all(
          color: AppColors.amber.withValues(alpha: 0.55),
          width: 1.5,
        ),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: AppColors.cardShadow,
            blurRadius: 12,
            offset: Offset(0, 5),
          ),
        ],
      ),
      child: Text(
        'F&F',
        style: AppTypography.style(
          size: 12.5,
          weight: FontWeight.w800,
          letterSpacing: 0.4,
          color: AppColors.amberSoft,
        ),
      ),
    );
  }
}

class _NotificationsButton extends StatelessWidget {
  const _NotificationsButton({this.onPressed});

  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: 'Notifications',
      child: Material(
        color: AppColors.white,
        shape: const CircleBorder(
          side: BorderSide(color: AppColors.divider),
        ),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onPressed,
          child: SizedBox(
            width: 44,
            height: 44,
            child: Stack(
              clipBehavior: Clip.none,
              alignment: Alignment.center,
              children: <Widget>[
                const Icon(
                  Icons.notifications_none_rounded,
                  size: 21,
                  color: AppColors.forest,
                ),
                Positioned(
                  top: 10,
                  right: 11,
                  child: Container(
                    width: 9,
                    height: 9,
                    decoration: BoxDecoration(
                      color: AppColors.alert,
                      shape: BoxShape.circle,
                      border: Border.all(color: AppColors.white, width: 1.6),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
