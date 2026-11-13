/// How much stock is left for an offer, used to flag urgency in the UI.
enum OfferStockLevel { low, medium, high }

/// A surplus-food offer published by a business.
///
/// This is the single shared model behind the business dashboard and the
/// customer's deal feed, so any change made by the business is instantly
/// visible to the customer.
class BusinessOffer {
  const BusinessOffer({
    required this.id,
    required this.title,
    required this.imageAsset,
    required this.available,
    required this.price,
    required this.stockLevel,
    this.originalPrice,
    this.category = 'Bakery',
    this.description = '',
    this.pickupWindow = '17:30–19:00',
    this.isPaused = false,
  });

  final String id;
  final String title;
  final String imageAsset;
  final int available;

  /// Price the customer pays, in euros.
  final double price;

  /// Regular price before the rescue discount, in euros.
  final double? originalPrice;

  final OfferStockLevel stockLevel;
  final String category;
  final String description;

  /// Pickup window as `HH:mm–HH:mm`.
  final String pickupWindow;
  final bool isPaused;

  String get priceLabel => '€${price.toStringAsFixed(2)}';

  String get availabilityLabel =>
      '$available ${available == 1 ? 'item' : 'items'} left';

  String get stockLabel => switch (stockLevel) {
    OfferStockLevel.low => 'LOW',
    OfferStockLevel.medium => 'MED',
    OfferStockLevel.high => 'HIGH',
  };

  double get savings {
    final double base = originalPrice ?? price;
    return (base - price).clamp(0, base);
  }

  int get discountPercent {
    final double base = originalPrice ?? price;
    if (base <= 0) {
      return 0;
    }
    return (savings / base * 100).round();
  }

  BusinessOffer copyWith({
    String? id,
    String? title,
    String? imageAsset,
    int? available,
    double? price,
    double? originalPrice,
    OfferStockLevel? stockLevel,
    String? category,
    String? description,
    String? pickupWindow,
    bool? isPaused,
  }) {
    return BusinessOffer(
      id: id ?? this.id,
      title: title ?? this.title,
      imageAsset: imageAsset ?? this.imageAsset,
      available: available ?? this.available,
      price: price ?? this.price,
      originalPrice: originalPrice ?? this.originalPrice,
      stockLevel: stockLevel ?? this.stockLevel,
      category: category ?? this.category,
      description: description ?? this.description,
      pickupWindow: pickupWindow ?? this.pickupWindow,
      isPaused: isPaused ?? this.isPaused,
    );
  }
}
