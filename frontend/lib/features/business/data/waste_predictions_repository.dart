import '../../../core/constants/app_assets.dart';
import '../domain/models/waste_prediction.dart';

/// Temporary in-memory source for today's AI surplus predictions.
class WastePredictionsRepository {
  const WastePredictionsRepository();

  List<WastePrediction> loadPredictions() {
    return const <WastePrediction>[
      WastePrediction(
        id: 'prediction-croissants',
        item: 'Croissants',
        imageAsset: AppAssets.predictionCroissant,
        stock: 34,
        predictedUnsold: 12,
        risk: PredictionRisk.high,
        recommendation: 'Apply 35% discount after 16:00.',
      ),
      WastePrediction(
        id: 'prediction-sandwiches',
        item: 'Sandwiches',
        imageAsset: AppAssets.predictionSandwich,
        stock: 15,
        predictedUnsold: 5,
        risk: PredictionRisk.medium,
        recommendation: 'Apply 20% discount after 14:30.',
      ),
      WastePrediction(
        id: 'prediction-cake',
        item: 'Chocolate cake',
        imageAsset: AppAssets.predictionCake,
        stock: 6,
        predictedUnsold: 1,
        risk: PredictionRisk.low,
        recommendation: 'No discount necessary yet.',
      ),
    ];
  }
}
