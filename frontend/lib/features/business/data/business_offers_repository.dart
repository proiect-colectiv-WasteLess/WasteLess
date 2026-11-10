import '../../../core/constants/app_assets.dart';
import '../domain/models/business_offer.dart';

/// Temporary in-memory source for the business's published offers.
///
/// Swapping this for an API client later only requires keeping
/// [loadOffers] intact.
class BusinessOffersRepository {
  const BusinessOffersRepository();

  List<BusinessOffer> loadOffers() {
    return const <BusinessOffer>[
      BusinessOffer(
        id: 'offer-croissant',
        title: 'Croissant rescue bag',
        imageAsset: AppAssets.offerCroissant,
        available: 12,
        price: 3.20,
        stockLevel: OfferStockLevel.low,
      ),
      BusinessOffer(
        id: 'offer-pastry',
        title: 'Pastry selection',
        imageAsset: AppAssets.offerPastry,
        available: 5,
        price: 2.00,
        stockLevel: OfferStockLevel.low,
      ),
    ];
  }
}
