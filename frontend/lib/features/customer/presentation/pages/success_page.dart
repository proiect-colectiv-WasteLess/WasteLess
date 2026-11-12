import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/router/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_primary_button.dart';
import '../../data/customer_session.dart';
import '../../domain/models/customer_order.dart';
import 'customer_shell_page.dart';

class SuccessPage extends StatelessWidget {
  const SuccessPage({super.key, required this.order});

  final CustomerOrder order;

  void _goToTab(BuildContext context, int tab) {
    Navigator.of(context).pushNamedAndRemoveUntil(
      AppRoutes.customerHome,
      (Route<dynamic> route) => false,
      arguments: tab,
    );
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.cream,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: PopScope(
        // The payment is done; leaving must land on a customer tab.
        canPop: false,
        onPopInvokedWithResult: (bool didPop, Object? result) {
          if (!didPop) {
            _goToTab(context, CustomerShellPage.homeTab);
          }
        },
        child: Scaffold(
          backgroundColor: AppColors.cream,
          body: SafeArea(
            child: Align(
              alignment: Alignment.topCenter,
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 460),
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(28, 40, 28, 25),
                  children: <Widget>[
                    const _Burst(),
                    const SizedBox(height: 20),
                    Text(
                      'RESCUE CONFIRMED',
                      textAlign: TextAlign.center,
                      style: AppTypography.style(
                        size: 10.5,
                        weight: FontWeight.w800,
                        letterSpacing: 1.5,
                        color: AppColors.olive,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'You did a\ngood thing!',
                      textAlign: TextAlign.center,
                      style: AppTypography.style(
                        size: 38,
                        weight: FontWeight.w800,
                        height: 1.05,
                        letterSpacing: -1.4,
                        color: AppColors.forestDeep,
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 13, 20, 18),
                      child: Text(
                        'Your food is waiting for you at ${order.deal.shop}.',
                        textAlign: TextAlign.center,
                        style: AppTypography.style(
                          size: 13.5,
                          height: 1.45,
                          color: AppColors.muted,
                        ),
                      ),
                    ),
                    _PickupTicket(order: order),
                    const _ImpactLine(),
                    AppPrimaryButton(
                      label: 'View my order',
                      onPressed: () =>
                          _goToTab(context, CustomerShellPage.ordersTab),
                    ),
                    const SizedBox(height: 6),
                    TextButton(
                      onPressed: () =>
                          _goToTab(context, CustomerShellPage.homeTab),
                      child: Text(
                        'Back to home',
                        style: AppTypography.style(
                          size: 14,
                          weight: FontWeight.w700,
                          color: AppColors.forest,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Burst extends StatelessWidget {
  const _Burst();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 90,
        height: 90,
        alignment: Alignment.center,
        decoration: const BoxDecoration(
          color: Color(0xFFDCEFDC),
          shape: BoxShape.circle,
        ),
        child: Container(
          width: 61,
          height: 61,
          decoration: const BoxDecoration(
            color: AppColors.forest,
            shape: BoxShape.circle,
          ),
          child: const Icon(
            Icons.check_rounded,
            size: 38,
            color: AppColors.white,
          ),
        ),
      ),
    );
  }
}

class _PickupTicket extends StatelessWidget {
  const _PickupTicket({required this.order});

  final CustomerOrder order;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(17),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(21),
        border: Border.all(color: AppColors.divider),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: AppColors.cardShadow,
            blurRadius: 18,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        children: <Widget>[
          SizedBox(
            width: double.infinity,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  'ORDER #${order.id}',
                  style: AppTypography.style(
                    size: 9,
                    letterSpacing: 1.3,
                    color: AppColors.muted,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  order.deal.name,
                  style: AppTypography.style(
                    size: 14.5,
                    weight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  'Pickup today · ${order.deal.pickupWindow}',
                  style: AppTypography.style(
                    size: 11,
                    color: AppColors.muted,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 13),
          const _DashedLine(),
          const SizedBox(height: 14),
          const _QrCode(),
          const SizedBox(height: 8),
          Text(
            order.pickupCode,
            style: AppTypography.style(
              size: 24,
              weight: FontWeight.w800,
              letterSpacing: 6,
              color: AppColors.forest,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            'Show this code at pickup',
            style: AppTypography.style(size: 10, color: AppColors.muted),
          ),
        ],
      ),
    );
  }
}

class _DashedLine extends StatelessWidget {
  const _DashedLine();

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        final int dashes = (constraints.maxWidth / 7).floor();
        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: List<Widget>.generate(
            dashes,
            (int _) => const SizedBox(
              width: 4,
              height: 1,
              child: ColoredBox(color: Color(0xFFDCDED8)),
            ),
          ),
        );
      },
    );
  }
}

/// Decorative 7x7 code that matches the mock's pickup ticket.
class _QrCode extends StatelessWidget {
  const _QrCode();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 98,
      height: 98,
      padding: const EdgeInsets.all(6),
      decoration: BoxDecoration(
        color: AppColors.white,
        border: Border.all(color: AppColors.forest, width: 2),
      ),
      child: GridView.count(
        crossAxisCount: 7,
        mainAxisSpacing: 2,
        crossAxisSpacing: 2,
        padding: EdgeInsets.zero,
        physics: const NeverScrollableScrollPhysics(),
        children: <Widget>[
          for (int i = 0; i < 49; i++)
            ColoredBox(
              color: (i * 7 + i % 5) % 3 == 0
                  ? AppColors.forest
                  : Colors.transparent,
            ),
        ],
      ),
    );
  }
}

class _ImpactLine extends StatelessWidget {
  const _ImpactLine();

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 15),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.sageTint,
        borderRadius: BorderRadius.circular(13),
      ),
      child: Row(
        children: <Widget>[
          const Icon(Icons.eco_outlined, size: 22, color: AppColors.forest),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              'You just rescued 1 meal and saved '
              '${CustomerSession.co2PerMeal}kg CO₂e.',
              style: AppTypography.style(
                size: 11.5,
                color: const Color(0xFF56725E),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
