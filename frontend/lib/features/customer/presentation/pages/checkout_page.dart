import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/router/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_primary_button.dart';
import '../../data/customer_session.dart';
import '../../domain/models/customer_order.dart';
import '../../domain/models/deal.dart';
import '../widgets/customer_page_header.dart';

class CheckoutPage extends StatefulWidget {
  const CheckoutPage({super.key});

  @override
  State<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends State<CheckoutPage> {
  static const List<String> _methods = <String>[
    'Card',
    'Apple Pay',
    'Google Pay',
  ];

  String _method = 'Card';

  void _pay() {
    final CustomerOrder? order = CustomerSession.instance.placeOrder();
    if (order == null) {
      return;
    }
    Navigator.of(context).pushNamedAndRemoveUntil(
      AppRoutes.customerSuccess,
      (Route<dynamic> route) => route.settings.name == AppRoutes.customerHome,
      arguments: order,
    );
  }

  @override
  Widget build(BuildContext context) {
    final CustomerSession session = CustomerSession.instance;
    final Deal? deal = session.basketDeal;
    final String total = formatEuro(session.basketTotal);

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
              child: ListView(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
                children: <Widget>[
                  const CustomerPageHeader(
                    title: 'Secure payment',
                    showBack: true,
                  ),
                  const SizedBox(height: 4),
                  const _SecureRow(),
                  _TotalBanner(total: total),
                  Padding(
                    padding: const EdgeInsets.only(top: 22, bottom: 10),
                    child: Text(
                      'Pay with',
                      style: AppTypography.style(
                        size: 16,
                        weight: FontWeight.w800,
                        letterSpacing: -0.3,
                        color: AppColors.forestDeep,
                      ),
                    ),
                  ),
                  Row(
                    children: <Widget>[
                      for (int i = 0; i < _methods.length; i++) ...<Widget>[
                        if (i > 0) const SizedBox(width: 7),
                        Expanded(
                          child: _MethodTab(
                            label: _methods[i],
                            selected: _methods[i] == _method,
                            onTap: () => setState(() => _method = _methods[i]),
                          ),
                        ),
                      ],
                    ],
                  ),
                  const _CardPreview(),
                  const _Field(label: 'Cardholder name', value: 'Noa Martins'),
                  const _Field(
                    label: 'Card number',
                    value: '4242 4242 4242 4242',
                  ),
                  const Row(
                    children: <Widget>[
                      Expanded(
                        child: _Field(label: 'Expiry', value: '12 / 28'),
                      ),
                      SizedBox(width: 11),
                      Expanded(
                        child: _Field(
                          label: 'CVV',
                          value: '•••',
                          obscure: true,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 18),
                  AppPrimaryButton(
                    label: 'Pay $total',
                    trailingIcon: Icons.check_rounded,
                    onPressed: deal == null ? null : _pay,
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

class _SecureRow extends StatelessWidget {
  const _SecureRow();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 18),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          Container(
            width: 20,
            height: 20,
            decoration: const BoxDecoration(
              color: AppColors.sageTint,
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.check_rounded,
              size: 13,
              color: AppColors.forest,
            ),
          ),
          const SizedBox(width: 6),
          Text(
            'Encrypted & secure checkout',
            style: AppTypography.style(size: 11.5, color: AppColors.olive),
          ),
        ],
      ),
    );
  }
}

class _TotalBanner extends StatelessWidget {
  const _TotalBanner({required this.total});

  final String total;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: AppColors.forest,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Text(
            'Total to pay',
            style: AppTypography.style(
              size: 12.5,
              color: AppColors.sageTint.withValues(alpha: 0.85),
            ),
          ),
          Text(
            total,
            style: AppTypography.style(
              size: 27,
              weight: FontWeight.w800,
              letterSpacing: -0.7,
              color: AppColors.white,
            ),
          ),
        ],
      ),
    );
  }
}

class _MethodTab extends StatelessWidget {
  const _MethodTab({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 160),
        height: 40,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.circular(11),
          border: Border.all(
            color: selected ? AppColors.forest : AppColors.divider,
            width: selected ? 1.4 : 1,
          ),
        ),
        child: Text(
          label,
          style: AppTypography.style(
            size: 11.5,
            weight: selected ? FontWeight.w700 : FontWeight.w500,
            color: selected ? AppColors.forest : AppColors.muted,
          ),
        ),
      ),
    );
  }
}

class _CardPreview extends StatelessWidget {
  const _CardPreview();

  @override
  Widget build(BuildContext context) {
    final TextStyle small = AppTypography.style(
      size: 10,
      color: AppColors.white,
    );

    return Container(
      height: 170,
      margin: const EdgeInsets.symmetric(vertical: 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(22),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: <Color>[Color(0xFF235E46), Color(0xFF0B3828)],
        ),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: Color(0x33144632),
            blurRadius: 30,
            offset: Offset(0, 15),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Container(
            width: 34,
            height: 25,
            decoration: BoxDecoration(
              color: AppColors.amber,
              borderRadius: BorderRadius.circular(7),
            ),
          ),
          const SizedBox(height: 15),
          Text(
            'WASTELESS CARD',
            style: AppTypography.style(
              size: 9,
              letterSpacing: 1.4,
              color: AppColors.sageTint.withValues(alpha: 0.75),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            '••••  ••••  ••••  4242',
            style: AppTypography.style(
              size: 18,
              weight: FontWeight.w600,
              letterSpacing: 1,
              color: AppColors.white,
            ),
          ),
          const Spacer(),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Text('NOA MARTINS', style: small),
              Text('12/28', style: small),
            ],
          ),
        ],
      ),
    );
  }
}

class _Field extends StatelessWidget {
  const _Field({
    required this.label,
    required this.value,
    this.obscure = false,
  });

  final String label;
  final String value;
  final bool obscure;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            label,
            style: AppTypography.style(
              size: 11,
              weight: FontWeight.w700,
              color: AppColors.muted,
            ),
          ),
          const SizedBox(height: 6),
          TextFormField(
            initialValue: value,
            obscureText: obscure,
            obscuringCharacter: '•',
            cursorColor: AppColors.forest,
            style: AppTypography.style(size: 13, color: AppColors.ink),
            decoration: InputDecoration(
              isDense: true,
              filled: true,
              fillColor: AppColors.white,
              contentPadding: const EdgeInsets.symmetric(
                horizontal: 13,
                vertical: 14,
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: AppColors.divider),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: AppColors.forest),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
