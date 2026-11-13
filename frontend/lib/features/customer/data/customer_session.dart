import 'package:flutter/foundation.dart';

import '../domain/models/customer_order.dart';
import '../domain/models/deal.dart';
import 'deals_repository.dart';

/// In-memory basket and order history for the signed-in customer.
///
/// Pushed routes sit outside the customer shell, so the state lives in a
/// shared notifier instead of a widget. Replace with a repository backed by
/// the API once it exists.
class CustomerSession extends ChangeNotifier {
  CustomerSession._() {
    // Sample pickup the mock shows before the customer places an order.
    _orders.add(
      CustomerOrder(
        id: 'WL-7921',
        deal: const DealsRepository().loadDeals().first,
        quantity: 1,
        pickupCode: '48 62',
      ),
    );
  }

  static final CustomerSession instance = CustomerSession._();

  /// CO2e kept out of the atmosphere per rescued meal, in kilograms.
  static const double co2PerMeal = 1.2;

  Deal? _deal;
  int _quantity = 0;
  final List<CustomerOrder> _orders = <CustomerOrder>[];
  int _nextOrderNumber = 8042;

  Deal? get basketDeal => _deal;

  int get basketQuantity => _quantity;

  bool get hasBasket => _deal != null && _quantity > 0;

  double get basketTotal => hasBasket ? _deal!.price * _quantity : 0;

  double get basketOriginalTotal => hasBasket ? _deal!.oldPrice * _quantity : 0;

  double get basketSavings => basketOriginalTotal - basketTotal;

  List<CustomerOrder> get orders => List<CustomerOrder>.unmodifiable(_orders);

  /// Adds one bag of [deal]. Adding a different deal replaces the basket.
  void addToBasket(Deal deal) {
    if (_deal?.id == deal.id) {
      _quantity += 1;
    } else {
      _deal = deal;
      _quantity = 1;
    }
    notifyListeners();
  }

  void setQuantity(int quantity) {
    if (!hasBasket) {
      return;
    }
    _quantity = quantity < 1 ? 1 : quantity;
    notifyListeners();
  }

  /// Turns the basket into an active order and empties it.
  CustomerOrder? placeOrder() {
    if (!hasBasket) {
      return null;
    }
    final CustomerOrder order = CustomerOrder(
      id: 'WL-${_nextOrderNumber++}',
      deal: _deal!,
      quantity: _quantity,
      pickupCode: '48 62',
    );
    _orders.insert(0, order);
    _deal = null;
    _quantity = 0;
    notifyListeners();
    return order;
  }
}
