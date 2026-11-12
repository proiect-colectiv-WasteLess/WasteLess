import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_snack_bar.dart';
import '../../data/waste_predictions_repository.dart';
import '../../domain/models/waste_prediction.dart';
import '../widgets/ai_assistant_banner.dart';
import '../widgets/prediction_card.dart';
import '../widgets/prediction_summary_card.dart';

/// AI Insights tab: today's surplus predictions and discount suggestions.
class BusinessAiInsightsPage extends StatelessWidget {
  const BusinessAiInsightsPage({super.key});

  static const WastePredictionsRepository _repository =
      WastePredictionsRepository();

  @override
  Widget build(BuildContext context) {
    final List<WastePrediction> predictions = _repository.loadPredictions();
    final int predictedSurplus = predictions.fold<int>(
      0,
      (int total, WastePrediction prediction) =>
          total + prediction.predictedUnsold,
    );

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.white,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: SafeArea(
        bottom: false,
        child: Align(
          alignment: Alignment.topCenter,
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: ListView(
              physics: const ClampingScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(20, 10, 20, 28),
              children: <Widget>[
                Text(
                  'AI Insights',
                  textAlign: TextAlign.center,
                  style: AppTypography.style(
                    size: 20,
                    weight: FontWeight.w800,
                    letterSpacing: -0.55,
                    color: AppColors.ink,
                  ),
                ),
                const SizedBox(height: 14),
                const AiAssistantBanner(),
                const SizedBox(height: 8),
                PredictionSummaryCard(predictedSurplus: predictedSurplus),
                const SizedBox(height: 20),
                Row(
                  children: <Widget>[
                    Text(
                      "Today's predictions",
                      style: AppTypography.style(
                        size: 17.5,
                        weight: FontWeight.w800,
                        letterSpacing: -0.5,
                        color: AppColors.ink,
                      ),
                    ),
                    const Spacer(),
                    Text(
                      'Updated 2m ago',
                      style: AppTypography.style(
                        size: 9.5,
                        weight: FontWeight.w500,
                        color: AppColors.muted,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                for (final WastePrediction prediction
                    in predictions) ...<Widget>[
                  PredictionCard(
                    prediction: prediction,
                    onCreateOffer: () => showAppSnackBar(
                      context,
                      'Offer draft for ${prediction.item} is coming soon.',
                    ),
                  ),
                  const SizedBox(height: 10),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
