import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/router/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_primary_button.dart';
import '../../data/customer_session.dart';
import '../../domain/models/deal.dart';
import '../widgets/customer_page_header.dart';

class CartPage extends StatelessWidget {
  const CartPage({super.key});

  @override
  Widget build(BuildContext context) {
    final CustomerSession session = CustomerSession.instance;

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.cream,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: Scaffold(
        backgroundColor: AppColors.cream,
        body: SafeArea(
          child: Align(
            alignment: Alignment.topCenter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: ListenableBuilder(
                listenable: session,
                builder: (BuildContext context, _) {
                  final Deal? deal = session.basketDeal;

                  return ListView(
                    padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
                    children: <Widget>[
                      const CustomerPageHeader(
                        title: 'Your basket',
                        showBack: true,
                      ),
                      if (deal == null)
                        const _EmptyBasket()
                      else ...<Widget>[
                        _ShopLabel(shop: deal.shop),
                        _CartItem(
                          deal: deal,
                          quantity: session.basketQuantity,
                          onChanged: session.setQuantity,
                        ),
                        const _PlanetNote(),
                        _Summary(session: session, deal: deal),
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          child: Text(
                            'No hidden fees. WasteLess commission is paid by '
                            'the business.',
                            textAlign: TextAlign.center,
                            style: AppTypography.style(
                              size: 10.5,
                              color: AppColors.muted,
                            ),
                          ),
                        ),
                        AppPrimaryButton(
                          label: 'Continue to payment',
                          onPressed: () => Navigator.of(
                            context,
                          ).pushNamed(AppRoutes.customerCheckout),
                        ),
                      ],
                    ],
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _EmptyBasket extends StatelessWidget {
  const _EmptyBasket();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 90),
      child: Column(
        children: <Widget>[
          const Icon(
            Icons.shopping_bag_outlined,
            size: 32,
            color: AppColors.forest,
          ),
          const SizedBox(height: 10),
          Text(
            'Your basket is empty',
            style: AppTypography.style(size: 15, weight: FontWeight.w800),
          ),
          const SizedBox(height: 4),
          Text(
            'Find a deal you love and rescue it.',
            style: AppTypography.style(size: 12, color: AppColors.muted),
          ),
        ],
      ),
    );
  }
}

class _ShopLabel extends StatelessWidget {
  const _ShopLabel({required this.shop});

  final String shop;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 12, bottom: 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'ORDER FROM',
            style: AppTypography.style(
              size: 9,
              weight: FontWeight.w700,
              letterSpacing: 1.3,
              color: AppColors.muted,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            shop,
            style: AppTypography.style(
              size: 17,
              weight: FontWeight.w800,
              letterSpacing: -0.3,
            ),
          ),
        ],
      ),
    );
  }
}

class _CartItem extends StatelessWidget {
  const _CartItem({
    required this.deal,
    required this.quantity,
    required this.onChanged,
  });

  final Deal deal;
  final int quantity;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: _cardDecoration(),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: Image.asset(
              deal.imageAsset,
              width: 75,
              height: 80,
              fit: BoxFit.cover,
              cacheWidth: 200,
              semanticLabel: deal.name,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: SizedBox(
              height: 80,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    deal.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: AppTypography.style(
                      size: 13.5,
                      weight: FontWeight.w700,
                      height: 1.2,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Pickup today · ${deal.pickupWindow}',
                    style: AppTypography.style(
                      size: 10.5,
                      color: AppColors.muted,
                    ),
                  ),
                  const Spacer(),
                  Row(
                    children: <Widget>[
                      Text(
                        deal.priceLabel,
                        style: AppTypography.style(
                          size: 14,
                          weight: FontWeight.w800,
                          color: AppColors.forest,
                        ),
                      ),
                      const Spacer(),
                      _StepButton(
                        label: '−',
                        semantic: 'Decrease quantity',
                        onTap: () => onChanged(quantity - 1),
                      ),
                      SizedBox(
                        width: 28,
                        child: Text(
                          '$quantity',
                          textAlign: TextAlign.center,
                          style: AppTypography.style(
                            size: 12,
                            weight: FontWeight.w700,
                          ),
                        ),
                      ),
                      _StepButton(
                        label: '+',
                        semantic: 'Increase quantity',
                        onTap: () => onChanged(quantity + 1),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _StepButton extends StatelessWidget {
  const _StepButton({
    required this.label,
    required this.semantic,
    required this.onTap,
  });

  final String label;
  final String semantic;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: semantic,
      child: Material(
        color: AppColors.sageTint,
        borderRadius: BorderRadius.circular(8),
        child: InkWell(
          borderRadius: BorderRadius.circular(8),
          onTap: onTap,
          child: SizedBox(
            width: 27,
            height: 27,
            child: Center(
              child: Text(
                label,
                style: AppTypography.style(
                  size: 15,
                  weight: FontWeight.w800,
                  color: AppColors.forest,
                  height: 1,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _PlanetNote extends StatelessWidget {
  const _PlanetNote();

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 15),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.sageTint,
        borderRadius: BorderRadius.circular(15),
      ),
      child: Row(
        children: <Widget>[
          const Icon(Icons.eco_outlined, size: 24, color: AppColors.forest),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  'A small rescue, a big difference',
                  style: AppTypography.style(
                    size: 12.5,
                    weight: FontWeight.w700,
                    color: AppColors.forest,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  'This order prevents about 1.2kg of CO₂e.',
                  style: AppTypography.style(
                    size: 10.5,
                    color: AppColors.olive,
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

class _Summary extends StatelessWidget {
  const _Summary({required this.session, required this.deal});

  final CustomerSession session;
  final Deal deal;

  @override
  Widget build(BuildContext context) {
    final TextStyle muted = AppTypography.style(
      size: 12.5,
      color: AppColors.muted,
    );

    return Container(
      padding: const EdgeInsets.all(17),
      decoration: _cardDecoration(),
      child: Column(
        children: <Widget>[
          _SummaryRow(
            label: 'Original total',
            value: formatEuro(session.basketOriginalTotal),
            style: muted.copyWith(decoration: TextDecoration.lineThrough),
            labelStyle: muted,
          ),
          const SizedBox(height: 12),
          _SummaryRow(
            label: 'WasteLess price',
            value: formatEuro(session.basketTotal),
            style: muted,
            labelStyle: muted,
          ),
          const SizedBox(height: 12),
          _SummaryRow(
            label: 'Your savings',
            value: formatEuro(session.basketSavings),
            style: AppTypography.style(
              size: 12.5,
              weight: FontWeight.w700,
              color: const Color(0xFF4A8354),
            ),
            labelStyle: AppTypography.style(
              size: 12.5,
              color: const Color(0xFF4A8354),
            ),
          ),
          const SizedBox(height: 13),
          const Divider(height: 1, color: AppColors.divider),
          const SizedBox(height: 13),
          _SummaryRow(
            label: 'Total',
            value: formatEuro(session.basketTotal),
            style: AppTypography.style(
              size: 19,
              weight: FontWeight.w800,
              letterSpacing: -0.4,
              color: AppColors.forest,
            ),
            labelStyle: AppTypography.style(
              size: 13,
              weight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({
    required this.label,
    required this.value,
    required this.style,
    required this.labelStyle,
  });

  final String label;
  final String value;
  final TextStyle style;
  final TextStyle labelStyle;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: <Widget>[
        Text(label, style: labelStyle),
        Text(value, style: style),
      ],
    );
  }
}

BoxDecoration _cardDecoration() {
  return BoxDecoration(
    color: AppColors.white,
    borderRadius: BorderRadius.circular(18),
    border: Border.all(color: AppColors.divider),
    boxShadow: const <BoxShadow>[
      BoxShadow(
        color: AppColors.cardShadow,
        blurRadius: 18,
        offset: Offset(0, 8),
      ),
    ],
  );
}
