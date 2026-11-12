/// A customer order placed with the business.
class BusinessOrder {
  const BusinessOrder({
    required this.id,
    required this.quantity,
    required this.itemName,
    required this.pickupWindow,
    required this.code,
    required this.price,
    required this.timeAgo,
    this.isCompleted = false,
  });

  final String id;
  final int quantity;
  final String itemName;
  final String pickupWindow;
  final String code;
  final double price;
  final String timeAgo;
  final bool isCompleted;

  String get reference => 'ORDER #$id';

  String get title => '$quantity × $itemName';

  String get pickupLabel => 'Pickup: $pickupWindow · Code $code';

  String get priceLabel => '€${price.toStringAsFixed(2)}';

  BusinessOrder copyWith({bool? isCompleted}) {
    return BusinessOrder(
      id: id,
      quantity: quantity,
      itemName: itemName,
      pickupWindow: pickupWindow,
      code: code,
      price: price,
      timeAgo: timeAgo,
      isCompleted: isCompleted ?? this.isCompleted,
    );
  }
}
