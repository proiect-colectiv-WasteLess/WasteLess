import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_snack_bar.dart';
import '../../data/business_orders_repository.dart';
import '../../domain/models/business_order.dart';
import '../widgets/order_card.dart';
import '../widgets/order_tabs.dart';
import '../widgets/order_validation_sheet.dart';

/// Orders tab: active pickups and the completed-order archive.
class BusinessOrdersPage extends StatefulWidget {
  const BusinessOrdersPage({super.key});

  @override
  State<BusinessOrdersPage> createState() => _BusinessOrdersPageState();
}

class _BusinessOrdersPageState extends State<BusinessOrdersPage> {
  final BusinessOrdersRepository _repository = const BusinessOrdersRepository();
  late final List<BusinessOrder> _orders;
  int _selectedTab = 0;

  @override
  void initState() {
    super.initState();
    _orders = List<BusinessOrder>.of(_repository.loadOrders());
  }

  List<BusinessOrder> get _activeOrders => _orders
      .where((BusinessOrder order) => !order.isCompleted)
      .toList(growable: false);

  List<BusinessOrder> get _completedOrders => _orders
      .where((BusinessOrder order) => order.isCompleted)
      .toList(growable: false);

  Future<void> _validateOrder(BusinessOrder order) async {
    final bool confirmed = await showOrderValidationSheet(context, order);
    if (!mounted || !confirmed) {
      return;
    }

    setState(() {
      final int index = _orders.indexWhere(
        (BusinessOrder item) => item.id == order.id,
      );
      if (index != -1) {
        _orders[index] = order.copyWith(isCompleted: true);
      }
    });
    showAppSnackBar(context, 'Order #${order.id} completed.');
  }

  @override
  Widget build(BuildContext context) {
    final List<BusinessOrder> activeOrders = _activeOrders;
    final List<BusinessOrder> completedOrders = _completedOrders;
    final List<BusinessOrder> visibleOrders = _selectedTab == 0
        ? activeOrders
        : completedOrders;

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
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.fromLTRB(20, 10, 20, 10),
                  child: Text(
                    'Order management',
                    textAlign: TextAlign.center,
                    style: AppTypography.style(
                      size: 20,
                      weight: FontWeight.w800,
                      letterSpacing: -0.55,
                      color: AppColors.ink,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: OrderTabs(
                    activeCount: activeOrders.length,
                    selectedIndex: _selectedTab,
                    onSelected: (int index) =>
                        setState(() => _selectedTab = index),
                  ),
                ),
                const SizedBox(height: 14),
                Expanded(
                  child: visibleOrders.isEmpty
                      ? _OrdersEmptyState(completed: _selectedTab == 1)
                      : ListView.separated(
                          physics: const ClampingScrollPhysics(),
                          padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                          itemCount: visibleOrders.length,
                          separatorBuilder: (_, _) =>
                              const SizedBox(height: 10),
                          itemBuilder: (BuildContext context, int index) {
                            final BusinessOrder order = visibleOrders[index];
                            return OrderCard(
                              order: order,
                              onValidate: order.isCompleted
                                  ? null
                                  : () => _validateOrder(order),
                            );
                          },
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

class _OrdersEmptyState extends StatelessWidget {
  const _OrdersEmptyState({required this.completed});

  final bool completed;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Container(
              width: 58,
              height: 58,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: AppColors.sageTint,
                borderRadius: BorderRadius.circular(18),
              ),
              child: Icon(
                completed ? Icons.task_alt_rounded : Icons.receipt_long_rounded,
                size: 27,
                color: AppColors.forest,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              completed ? 'No completed orders yet' : 'No active orders',
              textAlign: TextAlign.center,
              style: AppTypography.style(
                size: 17,
                weight: FontWeight.w800,
                letterSpacing: -0.4,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              completed
                  ? 'Validated pickups will be archived here.'
                  : 'New customer orders will show up here.',
              textAlign: TextAlign.center,
              style: AppTypography.style(
                size: 12.5,
                height: 1.45,
                color: AppColors.muted,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
