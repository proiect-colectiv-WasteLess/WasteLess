import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';

/// Stand-ins for the business tabs that are not part of this iteration.
class BusinessPlaceholderPage extends StatelessWidget {
  const BusinessPlaceholderPage({
    super.key,
    required this.title,
    required this.message,
    required this.icon,
  });

  final String title;
  final String message;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.white,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: SafeArea(
        bottom: false,
        child: Align(
          alignment: Alignment.topCenter,
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(24, 18, 24, 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  Text(
                    'Fresh Bakery',
                    style: AppTypography.style(
                      size: 21,
                      weight: FontWeight.w800,
                      letterSpacing: -0.6,
                    ),
                  ),
                  Expanded(
                    child: Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          Container(
                            width: 66,
                            height: 66,
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: AppColors.sageTint,
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Icon(
                              icon,
                              size: 30,
                              color: AppColors.forest,
                            ),
                          ),
                          const SizedBox(height: 18),
                          Text(
                            title,
                            textAlign: TextAlign.center,
                            style: AppTypography.style(
                              size: 23,
                              weight: FontWeight.w800,
                              letterSpacing: -0.7,
                            ),
                          ),
                          const SizedBox(height: 8),
                          ConstrainedBox(
                            constraints: const BoxConstraints(maxWidth: 300),
                            child: Text(
                              message,
                              textAlign: TextAlign.center,
                              style: AppTypography.style(
                                size: 13.5,
                                height: 1.5,
                                color: AppColors.muted,
                              ),
                            ),
                          ),
                          const SizedBox(height: 16),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 5,
                            ),
                            decoration: BoxDecoration(
                              color: AppColors.amberTint,
                              borderRadius: BorderRadius.circular(999),
                            ),
                            child: Text(
                              'COMING SOON',
                              style: AppTypography.style(
                                size: 9.5,
                                weight: FontWeight.w800,
                                letterSpacing: 1.1,
                                color: AppColors.amberInk,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
