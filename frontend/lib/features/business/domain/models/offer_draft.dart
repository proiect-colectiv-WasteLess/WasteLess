import '../../../../core/constants/app_assets.dart';
import 'waste_prediction.dart';

/// Initial values for the create-offer form.
///
/// A `null` draft opens the form completely empty, while
/// [OfferDraft.fromPrediction] fills it with the assistant's suggestion.
class OfferDraft {
  const OfferDraft({
    this.productName = '',
    this.category,
    this.description = '',
    this.originalPrice = '',
    this.wasteLessPrice = '',
    this.quantity = '',
    this.pickupWindow = '',
    this.photoAsset,
  });

  final String productName;
  final String? category;
  final String description;
  final String originalPrice;
  final String wasteLessPrice;
  final String quantity;
  final String pickupWindow;
  final String? photoAsset;

  factory OfferDraft.fromPrediction(WastePrediction prediction) {
    const double originalPrice = 8;
    final double wasteLessPrice =
        originalPrice * (1 - prediction.discountPercent / 100);

    return OfferDraft(
      productName: switch (prediction.item) {
        'Croissants' => 'Croissant rescue bag',
        'Sandwiches' => 'Sandwich rescue box',
        'Chocolate cake' => 'Chocolate cake rescue box',
        _ => '${prediction.item} rescue bag',
      },
      category: 'Bakery',
      description: switch (prediction.item) {
        'Croissants' =>
          'A surprise mix of our flaky, freshly baked croissants.',
        'Sandwiches' => 'A surprise selection of our freshly made sandwiches.',
        'Chocolate cake' => 'A rich chocolate cake, freshly baked today.',
        _ =>
          'A surprise selection of our freshly prepared '
              '${prediction.item.toLowerCase()}.',
      },
      originalPrice: originalPrice.toStringAsFixed(2),
      wasteLessPrice: wasteLessPrice.toStringAsFixed(2),
      quantity: '${prediction.predictedUnsold}',
      pickupWindow: switch (prediction.risk) {
        PredictionRisk.high => '16:00 – 19:00',
        PredictionRisk.medium => '14:30 – 17:30',
        PredictionRisk.low => '10:00 – 18:00',
      },
      photoAsset: switch (prediction.item) {
        'Croissants' => AppAssets.welcomeHero,
        'Sandwiches' => AppAssets.draftSandwich,
        'Chocolate cake' => AppAssets.draftCake,
        _ => null,
      },
    );
  }
}
