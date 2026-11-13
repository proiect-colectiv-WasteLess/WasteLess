import '../../../core/constants/app_assets.dart';
import '../domain/models/deal.dart';

/// Temporary in-memory source for nearby deals.
///
/// Swapping this for an API client later only requires keeping [loadDeals]
/// intact.
class DealsRepository {
  const DealsRepository();

  List<Deal> loadDeals() {
    return const <Deal>[
      Deal(
        id: 'deal-bakery',
        name: 'Fresh bakery surprise bag',
        shop: 'Flour & Fold',
        oldPrice: 7.00,
        price: 3.50,
        distanceKm: 0.35,
        pickupStart: '17:30',
        pickupEnd: '19:00',
        left: 3,
        rating: 4.9,
        imageAsset: AppAssets.dealBakery,
        category: DealCategory.bakery,
      ),
      Deal(
        id: 'deal-bowl',
        name: 'Rainbow lunch bowl',
        shop: 'Green Table',
        oldPrice: 9.00,
        price: 4.50,
        distanceKm: 0.6,
        pickupStart: '18:00',
        pickupEnd: '20:00',
        left: 2,
        rating: 4.8,
        imageAsset: AppAssets.dealBowl,
        category: DealCategory.meals,
      ),
      Deal(
        id: 'deal-pastry',
        name: 'Pastry selection',
        shop: 'Maison Rue',
        oldPrice: 6.00,
        price: 2.80,
        distanceKm: 1.1,
        pickupStart: '16:30',
        pickupEnd: '18:30',
        left: 5,
        rating: 4.7,
        imageAsset: AppAssets.dealPastry,
        category: DealCategory.bakery,
      ),
      Deal(
        id: 'deal-harvest',
        name: 'Fresh harvest box',
        shop: 'Daily Market',
        oldPrice: 11.90,
        price: 5.90,
        distanceKm: 1.4,
        pickupStart: '19:00',
        pickupEnd: '20:30',
        left: 1,
        rating: 4.9,
        imageAsset: AppAssets.dealSalad,
        category: DealCategory.groceries,
      ),
    ];
  }
}
