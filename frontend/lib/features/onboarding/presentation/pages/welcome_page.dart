import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/constants/app_assets.dart';
import '../../../../core/router/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_logo.dart';
import '../../../../core/widgets/app_primary_button.dart';
import '../../../../core/widgets/app_snack_bar.dart';

/// The first screen customers and businesses see when opening the app.
class WelcomePage extends StatelessWidget {
  const WelcomePage({super.key});

  static const double _heroFraction = 0.50;
  static const double _sheetRadius = 26;

  @override
  Widget build(BuildContext context) {
    final Size screen = MediaQuery.sizeOf(context);
    final double heroHeight =
        (screen.height * _heroFraction).clamp(240.0, 620.0);

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.forest,
        systemNavigationBarIconBrightness: Brightness.light,
      ),
      child: Scaffold(
        backgroundColor: AppColors.forest,
        body: SingleChildScrollView(
          physics: const ClampingScrollPhysics(),
          child: ConstrainedBox(
            constraints: BoxConstraints(minHeight: screen.height),
            child: Stack(
              children: <Widget>[
                Positioned(
                  top: 0,
                  left: 0,
                  right: 0,
                  height: heroHeight + _sheetRadius,
                  child: const _WelcomeHero(),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: <Widget>[
                    SizedBox(height: heroHeight),
                    ConstrainedBox(
                      constraints: BoxConstraints(
                        minHeight: math.max(0, screen.height - heroHeight),
                      ),
                      child: const _WelcomeSheet(radius: _sheetRadius),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _WelcomeHero extends StatelessWidget {
  const _WelcomeHero();

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: <Widget>[
        Image.asset(
          AppAssets.welcomeHero,
          fit: BoxFit.cover,
          alignment: const Alignment(0, -0.08),
          filterQuality: FilterQuality.medium,
          semanticLabel: 'Fresh golden croissants on parchment',
        ),
        // Scrim keeps the lockup legible over the bright pastry.
        const DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: <Color>[Color(0x40000000), Color(0x00000000)],
              stops: <double>[0, 0.34],
            ),
          ),
        ),
        SafeArea(
          bottom: false,
          child: Align(
            alignment: Alignment.topLeft,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 10, 20, 0),
              child: const AppLogo(markSize: 34),
            ),
          ),
        ),
      ],
    );
  }
}

class _WelcomeSheet extends StatelessWidget {
  const _WelcomeSheet({required this.radius});

  final double radius;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.forest,
        borderRadius: BorderRadius.vertical(top: Radius.circular(radius)),
      ),
      child: SafeArea(
        top: false,
        minimum: const EdgeInsets.only(bottom: 10),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 26, 24, 16),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Text(
                    'Eat and leave no crumbs',
                    style: AppTypography.style(
                      size: 12.5,
                      weight: FontWeight.w600,
                      color: AppColors.gold,
                      letterSpacing: 0.35,
                    ),
                  ),
                  const SizedBox(height: 11),
                  _Headline(),
                  const SizedBox(height: 14),
                  Text(
                    'Discover delicious food from local favorites, '
                    'for up to 70% less.',
                    style: AppTypography.style(
                      size: 15,
                      height: 1.55,
                      color: AppColors.mist,
                      letterSpacing: -0.1,
                    ),
                  ),
                  const SizedBox(height: 22),
                  AppPrimaryButton(
                    label: "Let's get started",
                    onPressed: () => Navigator.of(context).pushNamed(
                      AppRoutes.roleSelection,
                    ),
                  ),
                  const SizedBox(height: 14),
                  const _MemberRow(),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Headline extends StatelessWidget {
  const _Headline();

  @override
  Widget build(BuildContext context) {
    final TextStyle base = AppTypography.style(
      size: 40,
      weight: FontWeight.w800,
      height: 1.05,
      letterSpacing: -1.5,
    );

    return Text.rich(
      TextSpan(
        children: <TextSpan>[
          TextSpan(
            text: 'Eat well.\nSave more.\n',
            style: base.copyWith(color: AppColors.white),
          ),
          TextSpan(
            text: 'Waste less.',
            style: base.copyWith(color: AppColors.amber),
          ),
        ],
      ),
    );
  }
}

class _MemberRow extends StatelessWidget {
  const _MemberRow();

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: <Widget>[
        Text(
          'Already a member?',
          style: AppTypography.style(
            size: 13.5,
            weight: FontWeight.w500,
            color: AppColors.mist,
          ),
        ),
        TextButton(
          onPressed: () => showAppSnackBar(context, 'Sign in is coming soon.'),
          style: TextButton.styleFrom(
            minimumSize: Size.zero,
            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 4),
            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            foregroundColor: AppColors.white,
          ),
          child: Text(
            'Log In',
            style: AppTypography.style(
              size: 13.5,
              weight: FontWeight.w700,
              color: AppColors.white,
              decoration: TextDecoration.underline,
            ),
          ),
        ),
      ],
    );
  }
}
