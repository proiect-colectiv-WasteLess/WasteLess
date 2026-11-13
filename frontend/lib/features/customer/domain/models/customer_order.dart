import 'deal.dart';

enum OrderStatus { active, completed, cancelled }

/// A rescue the customer has paid for and still has to collect.
class CustomerOrder {
  const CustomerOrder({
    required this.id,
    required this.deal,
    required this.quantity,
    required this.pickupCode,
    this.status = OrderStatus.active,
  });

  /// Short reference such as `WL-8042`.
  final String id;
  final Deal deal;
  final int quantity;
  final String pickupCode;
  final OrderStatus status;

  double get total => deal.price * quantity;
}
