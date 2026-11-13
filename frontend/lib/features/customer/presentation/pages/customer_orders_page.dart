import 'package:flutter/material.dart';

import '../../../../core/store/app_store.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../business/domain/models/business_order.dart';
import '../widgets/customer_page_header.dart';

enum _OrdersTab { active, completed }

/// Customer order history backed by the shared store.
///
/// A pickup validated by the business moves the order from Active to
/// Completed here automatically.
class CustomerOrdersPage extends StatefulWidget {
  const CustomerOrdersPage({super.key});

  @override
  State<CustomerOrdersPage> createState() => _CustomerOrdersPageState();
}

class _CustomerOrdersPageState extends State<CustomerOrdersPage> {
  _OrdersTab _tab = _OrdersTab.active;

  @override
  Widget build(BuildContext context) {
    final AppStore store = AppStoreScope.of(context);
    final List<BusinessOrder> orders = _tab == _OrdersTab.active
        ? store.activeOrders
        : store.completedOrders;

    return SafeArea(
      bottom: false,
      child: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 460),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 110),
            children: <Widget>[
              const CustomerPageHeader(title: 'My orders'),
              const SizedBox(height: 16),
              _Tabs(
                selected: _tab,
                onSelected: (_OrdersTab tab) => setState(() => _tab = tab),
              ),
              for (final BusinessOrder order in orders) ...<Widget>[
                _OrderCard(order: order),
                const SizedBox(height: 12),
              ],
              if (orders.isEmpty)
                _EmptyState(tab: _tab)
              else
                const _RescueHint(),
            ],
          ),
        ),
      ),
    );
  }
}

class _Tabs extends StatelessWidget {
  const _Tabs({required this.selected, required this.onSelected});

  final _OrdersTab selected;
  final ValueChanged<_OrdersTab> onSelected;

  static const Map<_OrdersTab, String> _labels = <_OrdersTab, String>{
    _OrdersTab.active: 'Active',
    _OrdersTab.completed: 'Completed',
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
          for (final MapEntry<_OrdersTab, String> entry in _labels.entries)
            Expanded(
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => onSelected(entry.key),
                child: Container(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  decoration: BoxDecoration(
                    border: Border(
                      bottom: BorderSide(
                        color: entry.key == selected
                            ? AppColors.forest
                            : Colors.transparent,
                        width: 2,
                      ),
                    ),
                  ),
                  child: Text(
                    entry.value,
                    textAlign: TextAlign.center,
                    style: AppTypography.style(
                      size: 12.5,
                      weight: entry.key == selected
                          ? FontWeight.w800
                          : FontWeight.w500,
                      color: entry.key == selected
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

class _OrderCard extends StatelessWidget {
  const _OrderCard({required this.order});

  final BusinessOrder order;

  @override
  Widget build(BuildContext context) {
    final bool completed = order.isCompleted;

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
              if (completed)
                const Icon(
                  Icons.check_circle_rounded,
                  size: 11,
                  color: AppColors.forest,
                )
              else
                Container(
                  width: 7,
                  height: 7,
                  decoration: const BoxDecoration(
                    color: AppColors.success,
                    shape: BoxShape.circle,
                  ),
                ),
              const SizedBox(width: 5),
              Text(
                completed ? 'COLLECTED' : 'READY FOR PICKUP',
                style: AppTypography.style(
                  size: 9,
                  weight: FontWeight.w800,
                  letterSpacing: 0.6,
                  color: completed ? AppColors.forest : const Color(0xFF4D8659),
                ),
              ),
              const Spacer(),
              Text(
                order.reference,
                style: AppTypography.style(
                  size: 8.5,
                  weight: FontWeight.w700,
                  letterSpacing: 0.4,
                  color: AppColors.muted,
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
                  order.imageAsset,
                  width: 50,
                  height: 50,
                  fit: BoxFit.cover,
                  cacheWidth: 130,
                  semanticLabel: order.itemName,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      order.shop,
                      style: AppTypography.style(
                        size: 13,
                        weight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      order.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: AppTypography.style(
                        size: 10.5,
                        color: AppColors.muted,
                      ),
                    ),
                  ],
                ),
              ),
              Text(
                order.priceLabel,
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
              color: completed ? AppColors.sageTint : const Color(0xFFFFF5E8),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: <Widget>[
                Icon(
                  completed
                      ? Icons.task_alt_rounded
                      : Icons.access_time_rounded,
                  size: 20,
                  color: completed ? AppColors.forest : const Color(0xFF9A6037),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        completed ? 'Collected' : 'Pickup',
                        style: AppTypography.style(
                          size: 9,
                          color: completed
                              ? AppColors.olive
                              : const Color(0xFF9A6037),
                        ),
                      ),
                      Text(
                        order.pickupWindow,
                        style: AppTypography.style(
                          size: 12,
                          weight: FontWeight.w700,
                          color: completed
                              ? AppColors.forest
                              : const Color(0xFF9A6037),
                        ),
                      ),
                    ],
                  ),
                ),
                Text(
                  order.timeAgo,
                  style: AppTypography.style(
                    size: 10,
                    weight: FontWeight.w700,
                    color: completed
                        ? AppColors.forest
                        : const Color(0xFF9A6037),
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
                order.code,
                style: AppTypography.style(
                  size: 20,
                  weight: FontWeight.w800,
                  letterSpacing: 3,
                ),
              ),
              const Spacer(),
              if (!completed)
                Material(
                  color: AppColors.white,
                  borderRadius: BorderRadius.circular(12),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(12),
                    onTap: () => _showQr(context),
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

  void _showQr(BuildContext context) {
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
              order.code,
              style: AppTypography.style(
                size: 34,
                weight: FontWeight.w800,
                letterSpacing: 8,
                color: AppColors.forest,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'Show this code at ${order.shop}.',
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
            'That’s ${AppStore.co2PerMeal}kg CO₂e kept out of '
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

  final _OrdersTab tab;

  @override
  Widget build(BuildContext context) {
    final bool completed = tab == _OrdersTab.completed;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 55, horizontal: 20),
      child: Column(
        children: <Widget>[
          Icon(
            completed ? Icons.task_alt_rounded : Icons.receipt_long_rounded,
            size: 28,
            color: AppColors.forest,
          ),
          const SizedBox(height: 10),
          Text(
            completed ? 'No completed orders yet' : 'No active orders',
            style: AppTypography.style(
              size: 14,
              weight: FontWeight.w800,
              color: AppColors.forestDeep,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            completed
                ? 'Validated pickups will be archived here.'
                : 'Rescue a deal and it will show up here.',
            textAlign: TextAlign.center,
            style: AppTypography.style(size: 11.5, color: AppColors.muted),
          ),
        ],
      ),
    );
  }
}
