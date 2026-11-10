import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../domain/models/business_offer.dart';

/// A published offer with quick view, edit, pause and delete actions.
class OfferCard extends StatelessWidget {
  const OfferCard({
    super.key,
    required this.offer,
    required this.onView,
    required this.onEdit,
    required this.onTogglePause,
    required this.onDelete,
  });

  final BusinessOffer offer;
  final VoidCallback onView;
  final VoidCallback onEdit;
  final VoidCallback onTogglePause;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final bool paused = offer.isPaused;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.divider),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: AppColors.cardShadow,
            blurRadius: 16,
            offset: Offset(0, 7),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Opacity(
                opacity: paused ? 0.55 : 1,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(14),
                  child: Image.asset(
                    offer.imageAsset,
                    width: 70,
                    height: 70,
                    fit: BoxFit.cover,
                    cacheWidth: 140,
                    semanticLabel: offer.title,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Expanded(
                          child: Text(
                            offer.title,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: AppTypography.style(
                              size: 14.5,
                              weight: FontWeight.w700,
                              letterSpacing: -0.2,
                              height: 1.25,
                              color: AppColors.ink,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        _OfferBadge(offer: offer),
                      ],
                    ),
                    const SizedBox(height: 5),
                    Row(
                      children: <Widget>[
                        Flexible(
                          child: Text(
                            offer.availabilityLabel,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: AppTypography.style(
                              size: 11.5,
                              weight: FontWeight.w500,
                              color: AppColors.muted,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Text(
                          offer.priceLabel,
                          style: AppTypography.style(
                            size: 13.5,
                            weight: FontWeight.w800,
                            letterSpacing: -0.3,
                            color: paused ? AppColors.muted : AppColors.forest,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          const Divider(height: 1, thickness: 1, color: AppColors.divider),
          const SizedBox(height: 10),
          Row(
            children: <Widget>[
              Expanded(
                child: _OfferAction(
                  label: 'View',
                  icon: Icons.visibility_outlined,
                  color: AppColors.forest,
                  onTap: onView,
                ),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: _OfferAction(
                  label: 'Edit',
                  icon: Icons.edit_outlined,
                  color: AppColors.forest,
                  onTap: onEdit,
                ),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: _OfferAction(
                  label: paused ? 'Resume' : 'Pause',
                  icon: paused
                      ? Icons.play_arrow_rounded
                      : Icons.pause_rounded,
                  color: AppColors.amberInk,
                  onTap: onTogglePause,
                ),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: _OfferAction(
                  label: 'Delete',
                  icon: Icons.delete_outline_rounded,
                  color: AppColors.danger,
                  onTap: onDelete,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _OfferBadge extends StatelessWidget {
  const _OfferBadge({required this.offer});

  final BusinessOffer offer;

  @override
  Widget build(BuildContext context) {
    final bool paused = offer.isPaused;
    final bool low = offer.stockLevel == OfferStockLevel.low;

    final Color background = paused
        ? AppColors.infoTint
        : low
        ? AppColors.amberTint
        : AppColors.sageTint;
    final Color foreground = paused
        ? AppColors.info
        : low
        ? AppColors.amberInk
        : AppColors.forest;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 4),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(7),
      ),
      child: Text(
        paused ? 'PAUSED' : offer.stockLabel,
        style: AppTypography.style(
          size: 9,
          weight: FontWeight.w800,
          letterSpacing: 0.7,
          color: foreground,
        ),
      ),
    );
  }
}

class _OfferAction extends StatelessWidget {
  const _OfferAction({
    required this.label,
    required this.icon,
    required this.color,
    required this.onTap,
  });

  final String label;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.canvas,
      borderRadius: BorderRadius.circular(9),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: SizedBox(
          height: 33,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Icon(icon, size: 14, color: color),
              const SizedBox(width: 3),
              Flexible(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: AppTypography.style(
                    size: 11,
                    weight: FontWeight.w700,
                    color: color,
                    letterSpacing: -0.1,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
