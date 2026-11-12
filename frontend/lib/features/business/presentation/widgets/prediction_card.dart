import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../domain/models/waste_prediction.dart';

/// One item-level prediction with its AI recommendation and CTA.
class PredictionCard extends StatelessWidget {
  const PredictionCard({
    super.key,
    required this.prediction,
    required this.onCreateOffer,
  });

  final WastePrediction prediction;
  final VoidCallback onCreateOffer;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 14, 14, 4),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.divider),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: AppColors.cardShadow,
            blurRadius: 14,
            offset: Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Container(
                width: 42,
                height: 42,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: _emojiBackground,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Image.asset(
                  prediction.imageAsset,
                  width: 28,
                  height: 28,
                  fit: BoxFit.contain,
                  cacheWidth: 84,
                  excludeFromSemantics: true,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      prediction.item,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: AppTypography.style(
                        size: 14.5,
                        weight: FontWeight.w700,
                        letterSpacing: -0.25,
                        height: 1.2,
                        color: AppColors.ink,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      prediction.detailsLabel,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: AppTypography.style(
                        size: 10,
                        weight: FontWeight.w500,
                        color: AppColors.muted,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 6),
              _RiskBadge(risk: prediction.risk),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 9),
            decoration: BoxDecoration(
              color: AppColors.lavenderTint,
              borderRadius: BorderRadius.circular(11),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                const Padding(
                  padding: EdgeInsets.only(top: 1),
                  child: Icon(
                    Icons.auto_awesome_rounded,
                    size: 12,
                    color: AppColors.lavenderInk,
                  ),
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        'AI RECOMMENDATION',
                        style: AppTypography.style(
                          size: 7,
                          weight: FontWeight.w800,
                          letterSpacing: 0.9,
                          color: AppColors.lavenderInk,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        prediction.recommendation,
                        style: AppTypography.style(
                          size: 10.5,
                          weight: FontWeight.w500,
                          height: 1.35,
                          color: AppColors.ink,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 6),
          const Divider(height: 1, thickness: 1, color: AppColors.divider),
          Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: onCreateOffer,
              borderRadius: BorderRadius.circular(8),
              child: SizedBox(
                height: 38,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Text(
                      'Create offer from prediction',
                      style: AppTypography.style(
                        size: 10.5,
                        weight: FontWeight.w700,
                        color: AppColors.forest,
                        letterSpacing: -0.1,
                      ),
                    ),
                    const SizedBox(width: 2),
                    const Icon(
                      Icons.chevron_right_rounded,
                      size: 15,
                      color: AppColors.forest,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Color get _emojiBackground => switch (prediction.risk) {
    PredictionRisk.high => AppColors.peachTint,
    PredictionRisk.medium => AppColors.sageTint,
    PredictionRisk.low => AppColors.creamDeep,
  };
}

class _RiskBadge extends StatelessWidget {
  const _RiskBadge({required this.risk});

  final PredictionRisk risk;

  @override
  Widget build(BuildContext context) {
    final Color background = switch (risk) {
      PredictionRisk.high => AppColors.peachTint,
      PredictionRisk.medium => AppColors.amberTint,
      PredictionRisk.low => AppColors.sageTint,
    };
    final Color foreground = switch (risk) {
      PredictionRisk.high => AppColors.danger,
      PredictionRisk.medium => AppColors.amberInk,
      PredictionRisk.low => AppColors.forest,
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 4),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(7),
      ),
      child: Text(
        switch (risk) {
          PredictionRisk.high => 'HIGH',
          PredictionRisk.medium => 'MEDIUM',
          PredictionRisk.low => 'LOW',
        },
        style: AppTypography.style(
          size: 8,
          weight: FontWeight.w800,
          letterSpacing: 0.55,
          color: foreground,
        ),
      ),
    );
  }
}
