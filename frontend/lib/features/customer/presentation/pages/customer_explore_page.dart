import 'package:flutter/material.dart';

import '../../../../core/router/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_snack_bar.dart';
import '../../data/deals_repository.dart';
import '../../domain/models/deal.dart';
import '../widgets/customer_page_header.dart';
import '../widgets/customer_search_field.dart';
import '../widgets/deal_card.dart';

class CustomerExplorePage extends StatefulWidget {
  const CustomerExplorePage({super.key});

  @override
  State<CustomerExplorePage> createState() => _CustomerExplorePageState();
}

class _CustomerExplorePageState extends State<CustomerExplorePage> {
  static const Map<String, double> _radii = <String, double>{
    '500 m': 0.5,
    '1 km': 1,
    '3 km': 3,
    '5 km': 5,
    '10 km': 10,
  };
  static const List<String> _categories = <String>[
    'All',
    'Bakery',
    'Meals',
    'Groceries',
    'Desserts',
    'Fruit & veg',
    'Drinks',
  ];
  static const List<String> _discounts = <String>[
    'Any discount',
    '20%+ off',
    '30%+ off',
    '50%+ off',
    '70%+ off',
  ];
  static const List<String> _sorts = <String>[
    'Nearest',
    'Biggest discount',
    'Lowest price',
    'Ending soon',
  ];
  static const List<String> _quickFilters = <String>[
    'Under €5',
    'Pickup today',
    'Before 19:00',
  ];

  final List<Deal> _deals = const DealsRepository().loadDeals();
  String _query = '';
  String _radius = '1 km';
  String _category = 'All';
  String _discount = 'Any discount';
  String _sort = 'Nearest';
  String _quick = '';

  static DealCategory? _categoryOf(String label) {
    switch (label) {
      case 'Bakery':
        return DealCategory.bakery;
      case 'Meals':
        return DealCategory.meals;
      case 'Groceries':
        return DealCategory.groceries;
      case 'Desserts':
        return DealCategory.desserts;
      case 'Fruit & veg':
        return DealCategory.fruitAndVeg;
      case 'Drinks':
        return DealCategory.drinks;
    }
    return null;
  }

  List<Deal> get _results {
    final String query = _query.trim().toLowerCase();
    final double radius = _radii[_radius] ?? 10;
    final int discountLimit =
        int.tryParse(RegExp(r'\d+').firstMatch(_discount)?.group(0) ?? '') ?? 0;
    final DealCategory? category = _categoryOf(_category);

    final List<Deal> list = _deals.where((Deal d) {
      if (!'${d.name} ${d.shop}'.toLowerCase().contains(query)) {
        return false;
      }
      if (d.distanceKm > radius) {
        return false;
      }
      if (category != null && d.category != category) {
        return false;
      }
      if (d.discountPercent < discountLimit) {
        return false;
      }
      if (_quick == 'Under €5' && d.price >= 5) {
        return false;
      }
      if (_quick == 'Before 19:00' && d.pickupStartHour >= 19) {
        return false;
      }
      return true;
    }).toList();

    list.sort((Deal a, Deal b) {
      switch (_sort) {
        case 'Biggest discount':
          return b.discountPercent.compareTo(a.discountPercent);
        case 'Lowest price':
          return a.price.compareTo(b.price);
        case 'Ending soon':
          return a.pickupStart.compareTo(b.pickupStart);
        default:
          return a.distanceKm.compareTo(b.distanceKm);
      }
    });
    return list;
  }

  @override
  Widget build(BuildContext context) {
    final List<Deal> results = _results;

    return SafeArea(
      bottom: false,
      child: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 460),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 110),
            children: <Widget>[
              const CustomerPageHeader(title: 'Explore'),
              const SizedBox(height: 16),
              CustomerSearchField(
                hint: 'Search food, products or businesses',
                onChanged: (String v) => setState(() => _query = v),
              ),
              const SizedBox(height: 13),
              _LocationPanel(
                onChange: () =>
                    showAppSnackBar(context, 'Location picker is coming soon.'),
              ),
              const _FilterLabel('Search radius'),
              _ChipRow(
                options: _radii.keys.toList(),
                selected: _radius,
                onSelected: (String v) => setState(() => _radius = v),
              ),
              const _FilterLabel('Food category'),
              _ChipRow(
                options: _categories,
                selected: _category,
                onSelected: (String v) => setState(() => _category = v),
              ),
              const SizedBox(height: 14),
              Row(
                children: <Widget>[
                  Expanded(
                    child: _SelectField(
                      label: 'Discount',
                      value: _discount,
                      options: _discounts,
                      onChanged: (String v) => setState(() => _discount = v),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _SelectField(
                      label: 'Sort by',
                      value: _sort,
                      options: _sorts,
                      onChanged: (String v) => setState(() => _sort = v),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _ChipRow(
                options: _quickFilters,
                selected: _quick,
                warm: true,
                onSelected: (String v) =>
                    setState(() => _quick = _quick == v ? '' : v),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 22, bottom: 12),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: <Widget>[
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            '${results.length} delicious deals',
                            style: AppTypography.style(
                              size: 16,
                              weight: FontWeight.w800,
                              letterSpacing: -0.4,
                              color: AppColors.ink,
                            ),
                          ),
                          Text(
                            'within $_radius',
                            style: AppTypography.style(
                              size: 11,
                              color: AppColors.muted,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Text(
                      'Sorted by ${_sort.toLowerCase()}',
                      style: AppTypography.style(
                        size: 11,
                        color: AppColors.muted,
                      ),
                    ),
                  ],
                ),
              ),
              if (results.isEmpty)
                const _NoResults()
              else
                for (final Deal deal in results) ...<Widget>[
                  DealCard(
                    deal: deal,
                    onTap: () => Navigator.of(
                      context,
                    ).pushNamed(AppRoutes.customerProduct, arguments: deal),
                  ),
                  const SizedBox(height: 11),
                ],
            ],
          ),
        ),
      ),
    );
  }
}

class _LocationPanel extends StatelessWidget {
  const _LocationPanel({required this.onChange});

  final VoidCallback onChange;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.sageTint,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: <Widget>[
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: AppColors.forest,
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Icon(
              Icons.location_on_outlined,
              size: 20,
              color: AppColors.white,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  'SHOWING DEALS NEAR YOU',
                  style: AppTypography.style(
                    size: 8.5,
                    weight: FontWeight.w800,
                    letterSpacing: 0.8,
                    color: AppColors.olive,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'Cluj-Napoca, Romania',
                  style: AppTypography.style(
                    size: 12.5,
                    weight: FontWeight.w700,
                    color: AppColors.forestDeep,
                  ),
                ),
              ],
            ),
          ),
          Material(
            color: AppColors.white,
            borderRadius: BorderRadius.circular(9),
            child: InkWell(
              borderRadius: BorderRadius.circular(9),
              onTap: onChange,
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 8,
                ),
                child: Text(
                  'Change',
                  style: AppTypography.style(
                    size: 10,
                    weight: FontWeight.w800,
                    color: AppColors.forest,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _FilterLabel extends StatelessWidget {
  const _FilterLabel(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 16, bottom: 9),
      child: Text(
        text,
        style: AppTypography.style(
          size: 12,
          weight: FontWeight.w800,
          color: AppColors.forestDeep,
        ),
      ),
    );
  }
}

class _ChipRow extends StatelessWidget {
  const _ChipRow({
    required this.options,
    required this.selected,
    required this.onSelected,
    this.warm = false,
  });

  final List<String> options;
  final String selected;
  final ValueChanged<String> onSelected;

  /// Uses the amber quick-filter styling instead of the forest one.
  final bool warm;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 36,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: options.length,
        separatorBuilder: (_, _) => const SizedBox(width: 7),
        itemBuilder: (BuildContext context, int i) {
          final String option = options[i];
          final bool active = option == selected;
          final Color fill = active
              ? (warm ? AppColors.amber : AppColors.forest)
              : (warm ? AppColors.amberTint.withValues(alpha: 0.4) : AppColors.white);
          final Color border = active
              ? (warm ? AppColors.amber : AppColors.forest)
              : (warm ? AppColors.amberTint : AppColors.divider);
          final Color text = active
              ? (warm ? AppColors.forestDeep : AppColors.white)
              : (warm ? AppColors.clay : AppColors.muted);

          return GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: () => onSelected(option),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 160),
              padding: const EdgeInsets.symmetric(horizontal: 13),
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: fill,
                borderRadius: BorderRadius.circular(11),
                border: Border.all(color: border),
              ),
              child: Text(
                option,
                style: AppTypography.style(
                  size: 11.5,
                  weight: active && warm ? FontWeight.w800 : FontWeight.w500,
                  color: text,
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

class _SelectField extends StatelessWidget {
  const _SelectField({
    required this.label,
    required this.value,
    required this.options,
    required this.onChanged,
  });

  final String label;
  final String value;
  final List<String> options;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          label,
          style: AppTypography.style(
            size: 10,
            weight: FontWeight.w700,
            color: AppColors.muted,
          ),
        ),
        const SizedBox(height: 5),
        Container(
          height: 42,
          padding: const EdgeInsets.symmetric(horizontal: 11),
          decoration: BoxDecoration(
            color: AppColors.white,
            borderRadius: BorderRadius.circular(11),
            border: Border.all(color: AppColors.divider),
          ),
          child: DropdownButtonHideUnderline(
            child: DropdownButton<String>(
              value: value,
              isExpanded: true,
              borderRadius: BorderRadius.circular(14),
              dropdownColor: AppColors.white,
              icon: const Icon(
                Icons.keyboard_arrow_down_rounded,
                size: 20,
                color: AppColors.muted,
              ),
              style: AppTypography.style(
                size: 12,
                weight: FontWeight.w600,
                color: AppColors.forestDeep,
              ),
              items: <DropdownMenuItem<String>>[
                for (final String o in options)
                  DropdownMenuItem<String>(value: o, child: Text(o)),
              ],
              onChanged: (String? v) {
                if (v != null) {
                  onChanged(v);
                }
              },
            ),
          ),
        ),
      ],
    );
  }
}

class _NoResults extends StatelessWidget {
  const _NoResults();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 36, horizontal: 20),
      child: Column(
        children: <Widget>[
          const Icon(Icons.search_rounded, size: 30, color: AppColors.forest),
          const SizedBox(height: 10),
          Text(
            'No tasty matches yet',
            style: AppTypography.style(
              size: 14,
              weight: FontWeight.w800,
              color: AppColors.forestDeep,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Try another category or a wider search radius.',
            textAlign: TextAlign.center,
            style: AppTypography.style(size: 11.5, color: AppColors.muted),
          ),
        ],
      ),
    );
  }
}
