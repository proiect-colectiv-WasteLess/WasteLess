import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../data/customer_session.dart';
import '../../domain/models/customer_order.dart';
import '../../domain/models/deal.dart';
import '../widgets/customer_page_header.dart';

class CustomerOrdersPage extends StatefulWidget {
  const CustomerOrdersPage({super.key});

  @override
  State<CustomerOrdersPage> createState() => _CustomerOrdersPageState();
}

class _CustomerOrdersPageState extends State<CustomerOrdersPage> {
  OrderStatus _tab = OrderStatus.active;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      bottom: false,
      child: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 460),
          child: ListenableBuilder(
            listenable: CustomerSession.instance,
            builder: (BuildContext context, _) {
              final List<CustomerOrder> orders = CustomerSession
                  .instance
                  .orders
                  .where((CustomerOrder o) => o.status == _tab)
                  .toList();

              return ListView(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 110),
                children: <Widget>[
                  const CustomerPageHeader(title: 'My orders'),
                  const SizedBox(height: 16),
                  _Tabs(
                    selected: _tab,
                    onSelected: (OrderStatus s) => setState(() => _tab = s),
                  ),
                  for (final CustomerOrder order in orders) ...<Widget>[
                    _ActiveOrderCard(order: order),
                    const SizedBox(height: 12),
                  ],
                  if (orders.isEmpty)
                    _EmptyState(tab: _tab)
                  else
                    const _RescueHint(),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}

class _Tabs extends StatelessWidget {
  const _Tabs({required this.selected, required this.onSelected});

  final OrderStatus selected;
  final ValueChanged<OrderStatus> onSelected;

  static const Map<OrderStatus, String> _labels = <OrderStatus, String>{
    OrderStatus.active: 'Active',
    OrderStatus.completed: 'Completed',
    OrderStatus.cancelled: 'Cancelled',
  };

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: AppColors.divider)),
      ),
      child: Row(
        children: <Widget>[
          for (final MapEntry<OrderStatus, String> e in _labels.entries)
            Expanded(
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => onSelected(e.key),
                child: Container(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  decoration: BoxDecoration(
                    border: Border(
                      bottom: BorderSide(
                        color: e.key == selected
                            ? AppColors.forest
                            : Colors.transparent,
                        width: 2,
                      ),
                    ),
                  ),
                  child: Text(
                    e.value,
                    textAlign: TextAlign.center,
                    style: AppTypography.style(
                      size: 12.5,
                      weight: e.key == selected
                          ? FontWeight.w800
                          : FontWeight.w500,
                      color: e.key == selected
                          ? AppColors.forest
                          : AppColors.muted,
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _ActiveOrderCard extends StatelessWidget {
  const _ActiveOrderCard({required this.order});

  final CustomerOrder order;

  @override
  Widget build(BuildContext context) {
    final Deal deal = order.deal;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(20),
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
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Container(
                width: 7,
                height: 7,
                margin: const EdgeInsets.only(right: 5),
                decoration: const BoxDecoration(
                  color: AppColors.success,
                  shape: BoxShape.circle,
                ),
              ),
              Text(
                'READY FOR PICKUP',
                style: AppTypography.style(
                  size: 9,
                  weight: FontWeight.w800,
                  letterSpacing: 0.6,
                  color: const Color(0xFF4D8659),
                ),
              ),
            ],
          ),
          const SizedBox(height: 13),
          Row(
            children: <Widget>[
              ClipRRect(
                borderRadius: BorderRadius.circular(13),
                child: Image.asset(
                  deal.imageAsset,
                  width: 50,
                  height: 50,
                  fit: BoxFit.cover,
                  cacheWidth: 130,
                  semanticLabel: deal.shop,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      deal.shop,
                      style: AppTypography.style(
                        size: 13,
                        weight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '${order.quantity} × ${_shortName(deal)}',
                      style: AppTypography.style(
                        size: 10.5,
                        color: AppColors.muted,
                      ),
                    ),
                  ],
                ),
              ),
              Text(
                formatEuro(order.total),
                style: AppTypography.style(
                  size: 13.5,
                  weight: FontWeight.w800,
                  color: AppColors.forest,
                ),
              ),
            ],
          ),
          const SizedBox(height: 13),
          Container(
            padding: const EdgeInsets.all(11),
            decoration: BoxDecoration(
              color: const Color(0xFFFFF5E8),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: <Widget>[
                const Icon(
                  Icons.access_time_rounded,
                  size: 20,
                  color: Color(0xFF9A6037),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        'Pickup by',
                        style: AppTypography.style(
                          size: 9,
                          color: const Color(0xFF9A6037),
                        ),
                      ),
                      Text(
                        'Today, ${deal.pickupEnd}',
                        style: AppTypography.style(
                          size: 12,
                          weight: FontWeight.w700,
                          color: const Color(0xFF9A6037),
                        ),
                      ),
                    ],
                  ),
                ),
                Text(
                  '1h 24m left',
                  style: AppTypography.style(
                    size: 10,
                    weight: FontWeight.w700,
                    color: const Color(0xFF9A6037),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 13),
          Row(
            children: <Widget>[
              Text(
                'Pickup code',
                style: AppTypography.style(size: 10, color: AppColors.muted),
              ),
              const SizedBox(width: 8),
              Text(
                order.pickupCode,
                style: AppTypography.style(
                  size: 20,
                  weight: FontWeight.w800,
                  letterSpacing: 3,
                ),
              ),
              const Spacer(),
              Material(
                color: AppColors.white,
                borderRadius: BorderRadius.circular(12),
                child: InkWell(
                  borderRadius: BorderRadius.circular(12),
                  onTap: () => _showQr(context, order),
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppColors.divider),
                    ),
                    child: Text(
                      'View QR',
                      style: AppTypography.style(
                        size: 10.5,
                        weight: FontWeight.w700,
                        color: AppColors.forest,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// "Fresh bakery surprise bag" -> "Bakery surprise bag", as in the mock.
  static String _shortName(Deal deal) {
    final String name = deal.name.replaceFirst(RegExp(r'^Fresh '), '');
    return name.isEmpty ? name : name[0].toUpperCase() + name.substring(1);
  }

  void _showQr(BuildContext context, CustomerOrder order) {
    showDialog<void>(
      context: context,
      builder: (BuildContext context) => AlertDialog(
        backgroundColor: AppColors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        title: Text(
          'Pickup code',
          style: AppTypography.style(size: 16, weight: FontWeight.w800),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Text(
              order.pickupCode,
              style: AppTypography.style(
                size: 34,
                weight: FontWeight.w800,
                letterSpacing: 8,
                color: AppColors.forest,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'Show this code at ${order.deal.shop}.',
              textAlign: TextAlign.center,
              style: AppTypography.style(size: 12, color: AppColors.muted),
            ),
          ],
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(
              'Close',
              style: AppTypography.style(
                size: 13,
                weight: FontWeight.w700,
                color: AppColors.forest,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _RescueHint extends StatelessWidget {
  const _RescueHint();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 28),
      child: Column(
        children: <Widget>[
          const Icon(Icons.eco_outlined, size: 24, color: AppColors.olive),
          const SizedBox(height: 9),
          Text(
            'One more meal rescued',
            style: AppTypography.style(size: 13.5, weight: FontWeight.w700),
          ),
          const SizedBox(height: 4),
          Text(
            'That’s ${CustomerSession.co2PerMeal}kg CO₂e kept out of '
            'the atmosphere.',
            textAlign: TextAlign.center,
            style: AppTypography.style(size: 11, color: AppColors.muted),
          ),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.tab});

  final OrderStatus tab;

  @override
  Widget build(BuildContext context) {
    final String label = tab == OrderStatus.completed ? 'completed' : 'cancelled';

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 55, horizontal: 20),
      child: Column(
        children: <Widget>[
          const Icon(Icons.check_rounded, size: 28, color: AppColors.forest),
          const SizedBox(height: 10),
          Text(
            tab == OrderStatus.active
                ? 'No active orders'
                : 'No $label orders yet',
            style: AppTypography.style(
              size: 14,
              weight: FontWeight.w800,
              color: AppColors.forestDeep,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Rescue a deal and it will show up here.',
            style: AppTypography.style(size: 11.5, color: AppColors.muted),
          ),
        ],
      ),
    );
  }
}
