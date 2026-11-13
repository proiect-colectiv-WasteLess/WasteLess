import 'package:flutter/widgets.dart';

import '../../features/business/domain/models/business_offer.dart';
import '../../features/business/domain/models/business_order.dart';
import '../../features/customer/domain/models/deal.dart';
import '../constants/app_assets.dart';

/// Single in-memory source of truth shared by the customer and business
/// experiences.
///
/// The store is created once above [MaterialApp] and survives navigation
/// between roles, so offers, orders and the basket keep their state when a
/// user logs out and back in. Everything resets when the app process stops.
class AppStore extends ChangeNotifier {
  AppStore({List<BusinessOffer>? offers, List<BusinessOrder>? orders})
    : _offers = List<BusinessOffer>.of(offers ?? _seedOffers),
      _orders = List<BusinessOrder>.of(orders ?? _seedOrders);

  /// The single hardcoded business account.
  static const String businessName = 'Fresh Bakery';
  static const String businessEmail = 'freshbakery@wasteless.app';

  /// The single hardcoded customer account.
  static const String customerName = 'Noa Martins';
  static const String customerEmail = 'noa@wasteless.app';

  /// CO2e kept out of the atmosphere per rescued meal, in kilograms.
  static const double co2PerMeal = 1.2;

  final List<BusinessOffer> _offers;
  final List<BusinessOrder> _orders;

  int _nextOfferNumber = 1;
  int _nextOrderNumber = 8043;
  int _pickupSeed = 2917;

  Deal? _basketDeal;
  int _basketQuantity = 0;

  // ---------------------------------------------------------------------------
  // Offers
  // ---------------------------------------------------------------------------

  List<BusinessOffer> get offers => List<BusinessOffer>.unmodifiable(_offers);

  List<BusinessOffer> get activeOffers => _offers
      .where((BusinessOffer offer) => !offer.isPaused)
      .toList(growable: false);

  /// Offers the customer can currently order.
  List<BusinessOffer> get customerOffers => _offers
      .where((BusinessOffer offer) => !offer.isPaused && offer.available > 0)
      .toList(growable: false);

  /// Customer-facing deals derived from the shared offers.
  List<Deal> get deals {
    final List<BusinessOffer> live = customerOffers;
    return <Deal>[for (int i = 0; i < live.length; i++) _toDeal(live[i], i)];
  }

  BusinessOffer? offerById(String id) {
    final int index = _offers.indexWhere(
      (BusinessOffer offer) => offer.id == id,
    );
    return index == -1 ? null : _offers[index];
  }

  /// Publishes a new offer and makes it immediately visible to the customer.
  BusinessOffer createOffer({
    required String title,
    required String category,
    required String description,
    required double originalPrice,
    required double price,
    required int quantity,
    required String pickupWindow,
    String? imageAsset,
  }) {
    final BusinessOffer offer = BusinessOffer(
      id: 'offer-${_nextOfferNumber++}',
      title: title,
      imageAsset: imageAsset ?? AppAssets.offerCroissant,
      available: quantity,
      price: price,
      originalPrice: originalPrice,
      stockLevel: stockLevelFor(quantity),
      category: category,
      description: description,
      pickupWindow: pickupWindow,
    );
    _offers.insert(0, offer);
    notifyListeners();
    return offer;
  }

  void updateOffer(BusinessOffer updated) {
    final int index = _offers.indexWhere(
      (BusinessOffer offer) => offer.id == updated.id,
    );
    if (index == -1) {
      return;
    }
    _offers[index] = updated;
    notifyListeners();
  }

  void deleteOffer(String id) {
    final int index = _offers.indexWhere(
      (BusinessOffer offer) => offer.id == id,
    );
    if (index == -1) {
      return;
    }
    _offers.removeAt(index);
    notifyListeners();
  }

  void toggleOfferPaused(String id) {
    final int index = _offers.indexWhere(
      (BusinessOffer offer) => offer.id == id,
    );
    if (index == -1) {
      return;
    }
    final BusinessOffer offer = _offers[index];
    _offers[index] = offer.copyWith(isPaused: !offer.isPaused);
    notifyListeners();
  }

  static OfferStockLevel stockLevelFor(int available) {
    if (available <= 10) {
      return OfferStockLevel.low;
    }
    if (available <= 25) {
      return OfferStockLevel.medium;
    }
    return OfferStockLevel.high;
  }

  Deal _toDeal(BusinessOffer offer, int index) {
    final List<String> parts = offer.pickupWindow
        .split(RegExp(r'[–-]'))
        .map((String part) => part.trim())
        .where((String part) => part.isNotEmpty)
        .toList(growable: false);

    return Deal(
      id: offer.id,
      name: offer.title,
      shop: businessName,
      oldPrice: offer.originalPrice ?? offer.price,
      price: offer.price,
      distanceKm: 0.35 + index * 0.28,
      pickupStart: parts.isNotEmpty ? parts.first : '17:30',
      pickupEnd: parts.length > 1 ? parts[1] : '19:00',
      left: offer.available,
      rating: 4.9,
      imageAsset: offer.imageAsset,
      category: _categoryOf(offer.category),
      description: offer.description,
    );
  }

  static DealCategory _categoryOf(String category) {
    return switch (category.toLowerCase()) {
      'bakery' => DealCategory.bakery,
      'restaurant' => DealCategory.meals,
      'grocery' => DealCategory.groceries,
      'cafe' => DealCategory.drinks,
      _ => DealCategory.meals,
    };
  }

  // ---------------------------------------------------------------------------
  // Orders
  // ---------------------------------------------------------------------------

  List<BusinessOrder> get orders => List<BusinessOrder>.unmodifiable(_orders);

  List<BusinessOrder> get activeOrders => _orders
      .where((BusinessOrder order) => !order.isCompleted)
      .toList(growable: false);

  List<BusinessOrder> get completedOrders => _orders
      .where((BusinessOrder order) => order.isCompleted)
      .toList(growable: false);

  /// Places an order for [quantity] units of the offer with [offerId].
  ///
  /// Decrements the offer's remaining stock and adds the order to the shared
  /// list shown by both roles. Returns `null` when the offer is gone, paused
  /// or does not have enough stock.
  BusinessOrder? placeOrder(String offerId, int quantity) {
    final int index = _offers.indexWhere(
      (BusinessOffer offer) => offer.id == offerId,
    );
    if (index == -1) {
      return null;
    }

    final BusinessOffer offer = _offers[index];
    if (offer.isPaused || quantity < 1 || offer.available < quantity) {
      return null;
    }

    final int remaining = offer.available - quantity;
    _offers[index] = offer.copyWith(
      available: remaining,
      stockLevel: stockLevelFor(remaining),
    );

    final BusinessOrder order = BusinessOrder(
      id: 'WL-${_nextOrderNumber++}',
      quantity: quantity,
      itemName: offer.title,
      shop: businessName,
      imageAsset: offer.imageAsset,
      pickupWindow: 'Today, ${offer.pickupWindow}',
      code: _nextPickupCode(),
      price: offer.price * quantity,
      timeAgo: 'Just now',
    );
    _orders.insert(0, order);
    notifyListeners();
    return order;
  }

  void completeOrder(String id) {
    final int index = _orders.indexWhere(
      (BusinessOrder order) => order.id == id,
    );
    if (index == -1) {
      return;
    }
    _orders[index] = _orders[index].copyWith(isCompleted: true);
    notifyListeners();
  }

  String _nextPickupCode() {
    _pickupSeed = (_pickupSeed * 37 + 11) % 9000 + 1000;
    return '$_pickupSeed';
  }

  // ---------------------------------------------------------------------------
  // Customer basket
  // ---------------------------------------------------------------------------

  Deal? get basketDeal => _basketDeal;

  int get basketQuantity => _basketQuantity;

  bool get hasBasket => _basketDeal != null && _basketQuantity > 0;

  double get basketTotal =>
      hasBasket ? _basketDeal!.price * _basketQuantity : 0;

  double get basketOriginalTotal =>
      hasBasket ? _basketDeal!.oldPrice * _basketQuantity : 0;

  double get basketSavings => basketOriginalTotal - basketTotal;

  /// Adds one bag of [deal]. Adding a different deal replaces the basket.
  void addToBasket(Deal deal) {
    if (_basketDeal?.id == deal.id) {
      _basketQuantity += 1;
    } else {
      _basketDeal = deal;
      _basketQuantity = 1;
    }
    notifyListeners();
  }

  void setBasketQuantity(int quantity) {
    if (!hasBasket) {
      return;
    }
    final int max = offerById(_basketDeal!.id)?.available ?? quantity;
    final int upperBound = max < 1 ? 1 : max;
    _basketQuantity = quantity.clamp(1, upperBound);
    notifyListeners();
  }

  /// Turns the basket into a shared order and empties it.
  BusinessOrder? placeBasketOrder() {
    final Deal? deal = _basketDeal;
    if (deal == null || _basketQuantity < 1) {
      return null;
    }
    final BusinessOrder? order = placeOrder(deal.id, _basketQuantity);
    if (order == null) {
      return null;
    }
    _basketDeal = null;
    _basketQuantity = 0;
    notifyListeners();
    return order;
  }

  // ---------------------------------------------------------------------------
  // Seed data
  // ---------------------------------------------------------------------------

  static const List<BusinessOffer> _seedOffers = <BusinessOffer>[
    BusinessOffer(
      id: 'offer-croissant',
      title: 'Croissant rescue bag',
      imageAsset: AppAssets.offerCroissant,
      available: 12,
      price: 3.20,
      originalPrice: 6.40,
      stockLevel: OfferStockLevel.low,
      category: 'Bakery',
      description: 'A surprise mix of our flaky, freshly baked croissants.',
      pickupWindow: '17:30–19:00',
    ),
    BusinessOffer(
      id: 'offer-pastry',
      title: 'Pastry selection',
      imageAsset: AppAssets.offerPastry,
      available: 5,
      price: 2.00,
      originalPrice: 6.00,
      stockLevel: OfferStockLevel.low,
      category: 'Bakery',
      description: 'A hand-picked box of today’s sweet and savoury pastries.',
      pickupWindow: '16:30–18:30',
    ),
  ];

  static const List<BusinessOrder> _seedOrders = <BusinessOrder>[
    BusinessOrder(
      id: 'WL-8042',
      quantity: 1,
      itemName: 'Bakery surprise bag',
      shop: businessName,
      imageAsset: AppAssets.offerCroissant,
      pickupWindow: 'Today, 17:30–19:00',
      code: '4862',
      price: 7.00,
      timeAgo: '6 min ago',
    ),
    BusinessOrder(
      id: 'WL-8041',
      quantity: 2,
      itemName: 'Bakery surprise bag',
      shop: businessName,
      imageAsset: AppAssets.offerCroissant,
      pickupWindow: 'Today, 17:30–19:00',
      code: '2914',
      price: 3.50,
      timeAgo: '14 min ago',
    ),
    BusinessOrder(
      id: 'WL-8039',
      quantity: 3,
      itemName: 'Bakery surprise bag',
      shop: businessName,
      imageAsset: AppAssets.offerCroissant,
      pickupWindow: 'Today, 17:30–19:00',
      code: '7305',
      price: 10.50,
      timeAgo: '22 min ago',
    ),
    BusinessOrder(
      id: 'WL-8031',
      quantity: 1,
      itemName: 'Pastry selection',
      shop: businessName,
      imageAsset: AppAssets.offerPastry,
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
      shop: businessName,
      imageAsset: AppAssets.offerCroissant,
      pickupWindow: 'Yesterday, 17:30–19:00',
      code: '6402',
      price: 6.40,
      timeAgo: 'Yesterday',
      isCompleted: true,
    ),
  ];
}

/// Exposes the shared [AppStore] to the widget tree.
///
/// [of] subscribes the caller to store changes; [read] only fetches the store
/// and is safe inside callbacks and async continuations.
class AppStoreScope extends InheritedNotifier<AppStore> {
  const AppStoreScope({
    super.key,
    required AppStore store,
    required super.child,
  }) : super(notifier: store);

  static AppStore of(BuildContext context) {
    final AppStoreScope? scope = context
        .dependOnInheritedWidgetOfExactType<AppStoreScope>();
    assert(scope != null, 'No AppStoreScope found in the widget tree.');
    return scope!.notifier!;
  }

  static AppStore read(BuildContext context) {
    final AppStoreScope? scope = context
        .getInheritedWidgetOfExactType<AppStoreScope>();
    assert(scope != null, 'No AppStoreScope found in the widget tree.');
    return scope!.notifier!;
  }
}
