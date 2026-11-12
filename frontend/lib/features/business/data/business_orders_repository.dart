import '../domain/models/business_order.dart';

/// Temporary in-memory source for customer orders.
class BusinessOrdersRepository {
  const BusinessOrdersRepository();

  List<BusinessOrder> loadOrders() {
    return const <BusinessOrder>[
      BusinessOrder(
        id: 'WL-8042',
        quantity: 1,
        itemName: 'Bakery surprise bag',
        pickupWindow: 'Today, 17:30–19:00',
        code: '4862',
        price: 7.00,
        timeAgo: '6 min ago',
      ),
      BusinessOrder(
        id: 'WL-8041',
        quantity: 2,
        itemName: 'Bakery surprise bag',
        pickupWindow: 'Today, 17:30–19:00',
        code: '2914',
        price: 3.50,
        timeAgo: '14 min ago',
      ),
      BusinessOrder(
        id: 'WL-8039',
        quantity: 3,
        itemName: 'Bakery surprise bag',
        pickupWindow: 'Today, 17:30–19:00',
        code: '7305',
        price: 10.50,
        timeAgo: '22 min ago',
      ),
      BusinessOrder(
        id: 'WL-8031',
        quantity: 1,
        itemName: 'Pastry selection',
        pickupWindow: 'Today, 15:00–17:00',
        code: '1184',
        price: 2.00,
        timeAgo: '1 h ago',
        isCompleted: true,
      ),
      BusinessOrder(
        id: 'WL-8027',
        quantity: 2,
        itemName: 'Croissant rescue bag',
        pickupWindow: 'Yesterday, 17:30–19:00',
        code: '6402',
        price: 6.40,
        timeAgo: 'Yesterday',
        isCompleted: true,
      ),
    ];
  }
}
