import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_spacing.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_back_button.dart';
import '../../../../core/widgets/app_logo.dart';
import '../../../../core/widgets/app_snack_bar.dart';
import '../widgets/role_option_card.dart';

class RoleSelectionPage extends StatelessWidget {
  const RoleSelectionPage({super.key});

  void _selectRole(BuildContext context, String role) {
    showAppSnackBar(context, '$role experience is on its way.');
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.cream,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: Scaffold(
        backgroundColor: AppColors.cream,
        body: SafeArea(
          bottom: false,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 6, 12, 0),
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: AppBackButton(
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ),
              ),
              Expanded(
                child: SingleChildScrollView(
                  physics: const ClampingScrollPhysics(),
                  padding: const EdgeInsets.fromLTRB(
                    AppSpacing.pageGutter,
                    0,
                    AppSpacing.pageGutter,
                    32,
                  ),
                  child: Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(
                        maxWidth: AppSpacing.contentMaxWidth,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: <Widget>[
                          const _BrandHalo(),
                          const SizedBox(height: 6),
                          Text(
                            'One community, zero waste',
                            textAlign: TextAlign.center,
                            style: AppTypography.style(
                              size: 11,
                              weight: FontWeight.w700,
                              letterSpacing: 1.6,
                              color: AppColors.olive,
                            ),
                          ),
                          const SizedBox(height: 14),
                          Text(
                            'How will you\nuse WasteLess?',
                            textAlign: TextAlign.center,
                            style: AppTypography.style(
                              size: 33,
                              weight: FontWeight.w800,
                              height: 1.08,
                              letterSpacing: -0.9,
                              color: AppColors.ink,
                            ),
                          ),
                          const SizedBox(height: 12),
                          Text(
                            'Choose your path.',
                            textAlign: TextAlign.center,
                            style: AppTypography.style(
                              size: 14,
                              height: 1.45,
                              color: AppColors.muted,
                              letterSpacing: -0.05,
                            ),
                          ),
                          const SizedBox(height: 30),
                          RoleOptionCard(
                            title: "I'm hungry",
                            description:
                                'Discover nearby deals and rescue great food.',
                            icon: Icons.restaurant_rounded,
                            iconBackground: AppColors.sageTint,
                            iconColor: AppColors.forest,
                            onTap: () => _selectRole(context, 'Client'),
                          ),
                          const SizedBox(height: 14),
                          RoleOptionCard(
                            title: "I'm a business",
                            description:
                                'Sell surplus food and waste less with AI.',
                            icon: Icons.storefront_rounded,
                            iconBackground: AppColors.peachTint,
                            iconColor: AppColors.clay,
                            onTap: () => _selectRole(context, 'Business'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Soft sage halo behind the centered lockup.
class _BrandHalo extends StatelessWidget {
  const _BrandHalo();

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 172,
      child: Center(
        child: Container(
          width: 172,
          height: 172,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: RadialGradient(
              colors: <Color>[
                AppColors.glowTint,
                AppColors.glowTint.withValues(alpha: 0.45),
                AppColors.glowTint.withValues(alpha: 0.0),
              ],
              stops: const <double>[0.0, 0.55, 1],
            ),
          ),
          child: const FittedBox(
            fit: BoxFit.scaleDown,
            child: AppLogo(onDark: false, markSize: 34),
          ),
        ),
      ),
    );
  }
}
