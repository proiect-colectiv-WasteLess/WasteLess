/// How much stock is left for an offer, used to flag urgency in the UI.
enum OfferStockLevel { low, medium, high }

/// A surplus-food offer published by a business.
class BusinessOffer {
  const BusinessOffer({
    required this.id,
    required this.title,
    required this.imageAsset,
    required this.available,
    required this.price,
    required this.stockLevel,
    this.isPaused = false,
  });

  final String id;
  final String title;
  final String imageAsset;
  final int available;

  /// Unit price in euros.
  final double price;
  final OfferStockLevel stockLevel;
  final bool isPaused;

  String get priceLabel => '€${price.toStringAsFixed(2)}';

  String get availabilityLabel =>
      '$available ${available == 1 ? 'item' : 'items'} left';

  String get stockLabel => switch (stockLevel) {
    OfferStockLevel.low => 'LOW',
    OfferStockLevel.medium => 'MED',
    OfferStockLevel.high => 'HIGH',
  };

  BusinessOffer copyWith({
    String? id,
    String? title,
    String? imageAsset,
    int? available,
    double? price,
    OfferStockLevel? stockLevel,
    bool? isPaused,
  }) {
    return BusinessOffer(
      id: id ?? this.id,
      title: title ?? this.title,
      imageAsset: imageAsset ?? this.imageAsset,
      available: available ?? this.available,
      price: price ?? this.price,
      stockLevel: stockLevel ?? this.stockLevel,
      isPaused: isPaused ?? this.isPaused,
    );
  }
}
