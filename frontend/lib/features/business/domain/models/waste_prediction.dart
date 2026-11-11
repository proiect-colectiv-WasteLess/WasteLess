/// How likely an item is to go unsold, used to flag urgency.
enum PredictionRisk { high, medium, low }

/// An AI-generated surplus prediction for a single menu item.
class WastePrediction {
  const WastePrediction({
    required this.id,
    required this.item,
    required this.imageAsset,
    required this.stock,
    required this.predictedUnsold,
    required this.risk,
    required this.recommendation,
    this.discountPercent = 0,
  });

  final String id;
  final String item;

  /// Illustration shown in the prediction card.
  final String imageAsset;
  final int stock;
  final int predictedUnsold;
  final PredictionRisk risk;
  final String recommendation;

  /// Discount suggested by the assistant, used when drafting an offer.
  final int discountPercent;

  String get detailsLabel =>
      'Stock: $stock  ·  Predicted unsold: $predictedUnsold';

  String get riskLabel => switch (risk) {
    PredictionRisk.high => 'HIGH',
    PredictionRisk.medium => 'MEDIUM',
    PredictionRisk.low => 'LOW',
  };
}
