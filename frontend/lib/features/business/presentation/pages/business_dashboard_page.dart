import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/router/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_spacing.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_snack_bar.dart';
import '../../data/business_offers_repository.dart';
import '../../domain/models/business_offer.dart';
import '../widgets/business_header.dart';
import '../widgets/create_offer_button.dart';
import '../widgets/offer_card.dart';
import '../widgets/offer_dialogs.dart';
import '../widgets/revenue_card.dart';
import '../widgets/stat_tile.dart';

/// Home tab of the business area: performance snapshot plus offer management.
class BusinessDashboardPage extends StatefulWidget {
  const BusinessDashboardPage({super.key});

  @override
  State<BusinessDashboardPage> createState() => _BusinessDashboardPageState();
}

class _BusinessDashboardPageState extends State<BusinessDashboardPage> {
  final BusinessOffersRepository _repository = const BusinessOffersRepository();
  late final List<BusinessOffer> _offers;

  @override
  void initState() {
    super.initState();
    _offers = List<BusinessOffer>.of(_repository.loadOffers());
  }

  int get _activeOfferCount =>
      _offers.where((BusinessOffer offer) => !offer.isPaused).length;

  void _createOffer() {
    Navigator.of(context).pushNamed(AppRoutes.createOffer);
  }

  void _openNotifications() {
    showAppSnackBar(context, "You're all caught up.");
  }

  Future<void> _viewOffer(BusinessOffer offer) async {
    final OfferDetailsAction? action = await showOfferDetails(context, offer);
    if (!mounted || action == null) {
      return;
    }

    switch (action) {
      case OfferDetailsAction.edit:
        await _editOffer(offer);
        break;
      case OfferDetailsAction.delete:
        await _deleteOffer(offer);
        break;
    }
  }

  Future<void> _editOffer(BusinessOffer offer) async {
    final BusinessOffer? updated = await showEditOfferDialog(context, offer);
    if (!mounted || updated == null) {
      return;
    }

    _replaceOffer(updated);
    showAppSnackBar(context, '“${updated.title}” updated.');
  }

  Future<void> _deleteOffer(BusinessOffer offer) async {
    final bool confirmed = await confirmDeleteOffer(context, offer);
    if (!mounted || !confirmed) {
      return;
    }

    setState(() => _offers.removeWhere((item) => item.id == offer.id));
    showAppSnackBar(context, '“${offer.title}” deleted.');
  }

  void _togglePause(BusinessOffer offer) {
    final BusinessOffer updated = offer.copyWith(isPaused: !offer.isPaused);
    _replaceOffer(updated);
    showAppSnackBar(
      context,
      updated.isPaused
          ? '“${offer.title}” paused.'
          : '“${offer.title}” is live again.',
    );
  }

  void _replaceOffer(BusinessOffer updated) {
    setState(() {
      final int index = _offers.indexWhere(
        (BusinessOffer item) => item.id == updated.id,
      );
      if (index != -1) {
        _offers[index] = updated;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
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
            constraints: const BoxConstraints(
              maxWidth: AppSpacing.contentMaxWidth,
            ),
            child: ListView(
              physics: const ClampingScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(
                AppSpacing.pageGutter,
                8,
                AppSpacing.pageGutter,
                28,
              ),
              children: <Widget>[
                BusinessHeader(onNotificationsPressed: _openNotifications),
                const SizedBox(height: 20),
                const RevenueCard(),
                const SizedBox(height: 14),
                _StatsGrid(activeOffers: _activeOfferCount),
                const SizedBox(height: 18),
                CreateOfferButton(onPressed: _createOffer),
                const SizedBox(height: 26),
                _OffersHeader(count: _offers.length),
                const SizedBox(height: 12),
                if (_offers.isEmpty)
                  const _EmptyOffers()
                else
                  for (final BusinessOffer offer in _offers) ...<Widget>[
                    OfferCard(
                      offer: offer,
                      onView: () => _viewOffer(offer),
                      onEdit: () => _editOffer(offer),
                      onTogglePause: () => _togglePause(offer),
                      onDelete: () => _deleteOffer(offer),
                    ),
                    const SizedBox(height: 12),
                  ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _StatsGrid extends StatelessWidget {
  const _StatsGrid({required this.activeOffers});

  final int activeOffers;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        Row(
          children: <Widget>[
            const Expanded(
              child: StatTile(
                icon: Icons.shopping_bag_outlined,
                value: '42',
                label: 'Products sold',
                iconBackground: AppColors.sageTint,
                iconColor: AppColors.forest,
              ),
            ),
            const SizedBox(width: 12),
            const Expanded(
              child: StatTile(
                icon: Icons.eco_outlined,
                value: '18.6kg',
                label: 'Waste avoided',
                iconBackground: AppColors.glowTint,
                iconColor: AppColors.forest,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: <Widget>[
            Expanded(
              child: StatTile(
                icon: Icons.local_offer_outlined,
                value: '$activeOffers',
                label: 'Active offers',
                iconBackground: AppColors.amberTint,
                iconColor: AppColors.amberInk,
              ),
            ),
            const SizedBox(width: 12),
            const Expanded(
              child: StatTile(
                icon: Icons.euro_rounded,
                value: '€31.60',
                label: 'Commission',
                iconBackground: AppColors.infoTint,
                iconColor: AppColors.info,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _OffersHeader extends StatelessWidget {
  const _OffersHeader({required this.count});

  final int count;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        Text(
          'Active Offers',
          style: AppTypography.style(
            size: 19,
            weight: FontWeight.w800,
            letterSpacing: -0.45,
          ),
        ),
        const SizedBox(width: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
          decoration: BoxDecoration(
            color: AppColors.sageTint,
            borderRadius: BorderRadius.circular(8),
          ),
          child: Text(
            '$count',
            style: AppTypography.style(
              size: 11,
              weight: FontWeight.w800,
              color: AppColors.forest,
            ),
          ),
        ),
      ],
    );
  }
}

class _EmptyOffers extends StatelessWidget {
  const _EmptyOffers();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 30),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.divider),
      ),
      child: Column(
        children: <Widget>[
          Container(
            width: 46,
            height: 46,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppColors.sageTint,
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Icon(
              Icons.eco_rounded,
              size: 22,
              color: AppColors.forest,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            'No active offers yet',
            textAlign: TextAlign.center,
            style: AppTypography.style(
              size: 15,
              weight: FontWeight.w700,
              letterSpacing: -0.2,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Create an offer to start rescuing food today.',
            textAlign: TextAlign.center,
            style: AppTypography.style(
              size: 12.5,
              height: 1.45,
              color: AppColors.muted,
            ),
          ),
        ],
      ),
    );
  }
}
