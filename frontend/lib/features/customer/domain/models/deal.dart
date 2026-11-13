enum DealCategory { bakery, meals, groceries, desserts, fruitAndVeg, drinks }

/// A surplus-food offer a customer can rescue.
class Deal {
  const Deal({
    required this.id,
    required this.name,
    required this.shop,
    required this.oldPrice,
    required this.price,
    required this.distanceKm,
    required this.pickupStart,
    required this.pickupEnd,
    required this.left,
    required this.rating,
    required this.imageAsset,
    required this.category,
  });

  final String id;
  final String name;
  final String shop;
  final double oldPrice;
  final double price;
  final double distanceKm;

  /// Pickup window boundaries as `HH:mm`.
  final String pickupStart;
  final String pickupEnd;
  final int left;
  final double rating;
  final String imageAsset;
  final DealCategory category;

  int get discountPercent => ((1 - price / oldPrice) * 100).round();

  double get saving => oldPrice - price;

  String get discountLabel => '-$discountPercent%';

  String get priceLabel => formatEuro(price);

  String get oldPriceLabel => formatEuro(oldPrice);

  String get savingLabel => formatEuro(saving);

  String get pickupWindow => '$pickupStart–$pickupEnd';

  int get pickupStartHour => int.parse(pickupStart.split(':').first);

  String get distanceLabel => distanceKm < 1
      ? '${(distanceKm * 1000).round()}m'
      : '${distanceKm.toStringAsFixed(1)}km';
}

String formatEuro(double value) => '€${value.toStringAsFixed(2)}';
