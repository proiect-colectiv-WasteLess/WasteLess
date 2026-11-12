import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_spacing.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/app_back_button.dart';
import '../../../../core/widgets/app_snack_bar.dart';
import '../../domain/models/offer_draft.dart';

/// Form for publishing a new surplus-food offer.
///
/// Opened empty from the dashboard, or prefilled with an AI prediction.
class CreateOfferPage extends StatefulWidget {
  const CreateOfferPage({super.key, this.initialDraft});

  final OfferDraft? initialDraft;

  @override
  State<CreateOfferPage> createState() => _CreateOfferPageState();
}

class _CreateOfferPageState extends State<CreateOfferPage> {
  static const List<String> _categories = <String>[
    'Bakery',
    'Cafe',
    'Restaurant',
    'Grocery',
    'Other',
  ];

  late final TextEditingController _productNameController;
  late final TextEditingController _descriptionController;
  late final TextEditingController _originalPriceController;
  late final TextEditingController _wasteLessPriceController;
  late final TextEditingController _quantityController;
  late final TextEditingController _pickupWindowController;

  String? _category;
  String? _photoAsset;

  @override
  void initState() {
    super.initState();
    final OfferDraft? draft = widget.initialDraft;

    _productNameController = TextEditingController(
      text: draft?.productName ?? '',
    );
    _descriptionController = TextEditingController(
      text: draft?.description ?? '',
    );
    _originalPriceController = TextEditingController(
      text: draft?.originalPrice ?? '',
    );
    _wasteLessPriceController = TextEditingController(
      text: draft?.wasteLessPrice ?? '',
    );
    _quantityController = TextEditingController(text: draft?.quantity ?? '');
    _pickupWindowController = TextEditingController(
      text: draft?.pickupWindow ?? '',
    );

    _category = draft?.category;
    _photoAsset = draft?.photoAsset;

    _originalPriceController.addListener(_onPriceChanged);
    _wasteLessPriceController.addListener(_onPriceChanged);
  }

  @override
  void dispose() {
    _originalPriceController.removeListener(_onPriceChanged);
    _wasteLessPriceController.removeListener(_onPriceChanged);
    _productNameController.dispose();
    _descriptionController.dispose();
    _originalPriceController.dispose();
    _wasteLessPriceController.dispose();
    _quantityController.dispose();
    _pickupWindowController.dispose();
    super.dispose();
  }

  void _onPriceChanged() {
    setState(() {});
  }

  double? _parseMoney(TextEditingController controller) {
    final String raw = controller.text.trim().replaceAll(',', '.');
    return double.tryParse(raw);
  }

  double? get _originalPrice => _parseMoney(_originalPriceController);

  double? get _wasteLessPrice => _parseMoney(_wasteLessPriceController);

  double get _customerSavings {
    final double? original = _originalPrice;
    final double? price = _wasteLessPrice;
    if (original == null || price == null || original <= 0) {
      return 0;
    }
    return (original - price).clamp(0.0, original);
  }

  int get _savingsPercent {
    final double? original = _originalPrice;
    if (original == null || original <= 0) {
      return 0;
    }
    return (_customerSavings / original * 100).round();
  }

  double get _commission => (_wasteLessPrice ?? 0) * 0.1;

  double get _netReceive => (_wasteLessPrice ?? 0) - _commission;

  String _money(double value) => '€${value.toStringAsFixed(2)}';

  void _changePhoto() {
    showAppSnackBar(context, 'Photo upload is coming soon.');
  }

  void _publish() {
    final String name = _productNameController.text.trim();
    final double? original = _originalPrice;
    final double? price = _wasteLessPrice;
    final int? quantity = int.tryParse(_quantityController.text.trim());
    final bool valid =
        name.isNotEmpty &&
        _category != null &&
        original != null &&
        original > 0 &&
        price != null &&
        price > 0 &&
        price <= original &&
        quantity != null &&
        quantity > 0 &&
        _pickupWindowController.text.trim().isNotEmpty;

    if (!valid) {
      showAppSnackBar(context, 'Complete the offer details before publishing.');
      return;
    }

    showAppSnackBar(context, '“$name” published.');
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.cream,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: Scaffold(
        backgroundColor: AppColors.cream,
        body: SafeArea(
          bottom: false,
          child: Align(
            alignment: Alignment.topCenter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(
                maxWidth: AppSpacing.contentMaxWidth,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  const _PageHeader(),
                  Expanded(
                    child: ListView(
                      physics: const ClampingScrollPhysics(),
                      keyboardDismissBehavior:
                          ScrollViewKeyboardDismissBehavior.onDrag,
                      padding: const EdgeInsets.fromLTRB(16, 6, 16, 24),
                      children: <Widget>[
                        _PhotoHeader(
                          photoAsset: _photoAsset,
                          onChangePhoto: _changePhoto,
                        ),
                        const SizedBox(height: 14),
                        _FieldLabel('Product name'),
                        TextField(
                          controller: _productNameController,
                          textInputAction: TextInputAction.next,
                          style: _inputStyle,
                          decoration: _decoration(hint: 'e.g. Croissant bag'),
                        ),
                        const SizedBox(height: 12),
                        _FieldLabel('Category'),
                        _CategoryField(
                          value: _category,
                          categories: _categories,
                          onChanged: (String? value) =>
                              setState(() => _category = value),
                        ),
                        const SizedBox(height: 12),
                        _FieldLabel('Description'),
                        TextField(
                          controller: _descriptionController,
                          minLines: 2,
                          maxLines: 3,
                          textInputAction: TextInputAction.newline,
                          style: _inputStyle,
                          decoration: _decoration(
                            hint: 'What is inside this offer?',
                          ),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: <Widget>[
                                  _FieldLabel('Original price'),
                                  TextField(
                                    controller: _originalPriceController,
                                    keyboardType:
                                        const TextInputType.numberWithOptions(
                                          decimal: true,
                                        ),
                                    textInputAction: TextInputAction.next,
                                    style: _inputStyle,
                                    decoration: _decoration(
                                      hint: '0.00',
                                      prefixText: '€',
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: <Widget>[
                                  _FieldLabel('WasteLess price'),
                                  TextField(
                                    controller: _wasteLessPriceController,
                                    keyboardType:
                                        const TextInputType.numberWithOptions(
                                          decimal: true,
                                        ),
                                    textInputAction: TextInputAction.next,
                                    style: _inputStyle,
                                    decoration: _decoration(
                                      hint: '0.00',
                                      prefixText: '€',
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        _SavingsBreakdown(
                          customerSavings: _money(_customerSavings),
                          savingsPercent: _savingsPercent,
                          netReceive: _money(_netReceive),
                          commission: _money(_commission),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: <Widget>[
                                  _FieldLabel('Quantity'),
                                  TextField(
                                    controller: _quantityController,
                                    keyboardType: TextInputType.number,
                                    textInputAction: TextInputAction.next,
                                    style: _inputStyle,
                                    decoration: _decoration(hint: '0'),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: <Widget>[
                                  _FieldLabel('Pickup window'),
                                  TextField(
                                    controller: _pickupWindowController,
                                    textInputAction: TextInputAction.done,
                                    style: _inputStyle,
                                    decoration: _decoration(
                                      hint: '16:00 – 19:00',
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        bottomNavigationBar: _PublishBar(onPressed: _publish),
      ),
    );
  }

  TextStyle get _inputStyle => AppTypography.style(
    size: 13,
    weight: FontWeight.w600,
    color: AppColors.ink,
  );

  InputDecoration _decoration({String? hint, String? prefixText}) {
    return InputDecoration(
      isDense: true,
      filled: true,
      fillColor: AppColors.white,
      hintText: hint,
      hintStyle: AppTypography.style(
        size: 12.5,
        weight: FontWeight.w500,
        color: AppColors.mist,
      ),
      prefixText: prefixText,
      prefixStyle: AppTypography.style(
        size: 13,
        weight: FontWeight.w600,
        color: AppColors.ink,
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      border: _border(AppColors.divider),
      enabledBorder: _border(AppColors.divider),
      focusedBorder: _border(AppColors.forest, width: 1.4),
    );
  }

  OutlineInputBorder _border(Color color, {double width = 1}) {
    return OutlineInputBorder(
      borderRadius: BorderRadius.circular(11),
      borderSide: BorderSide(color: color, width: width),
    );
  }
}

class _PageHeader extends StatelessWidget {
  const _PageHeader();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(6, 4, 6, 0),
      child: SizedBox(
        height: 44,
        child: Stack(
          alignment: Alignment.center,
          children: <Widget>[
            const Align(
              alignment: Alignment.centerLeft,
              child: AppBackButton(),
            ),
            Text(
              'Create new offer',
              style: AppTypography.style(
                size: 18,
                weight: FontWeight.w800,
                letterSpacing: -0.5,
                color: AppColors.ink,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PhotoHeader extends StatelessWidget {
  const _PhotoHeader({required this.photoAsset, required this.onChangePhoto});

  final String? photoAsset;
  final VoidCallback onChangePhoto;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 158,
      child: Stack(
        fit: StackFit.expand,
        children: <Widget>[
          if (photoAsset != null)
            ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: Image.asset(
                photoAsset!,
                fit: BoxFit.cover,
                alignment: Alignment.center,
                cacheWidth: 900,
                semanticLabel: 'Offer photo',
              ),
            )
          else
            const _PhotoPlaceholder(),
          Positioned(
            right: 10,
            bottom: 10,
            child: Material(
              color: AppColors.white,
              shape: const StadiumBorder(),
              clipBehavior: Clip.antiAlias,
              child: InkWell(
                onTap: onChangePhoto,
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 11,
                    vertical: 7,
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      const Icon(
                        Icons.add_rounded,
                        size: 14,
                        color: AppColors.forest,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        photoAsset == null ? 'Add photo' : 'Change photo',
                        style: AppTypography.style(
                          size: 9.5,
                          weight: FontWeight.w700,
                          letterSpacing: -0.1,
                          color: AppColors.forest,
                        ),
                      ),
                    ],
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

class _PhotoPlaceholder extends StatelessWidget {
  const _PhotoPlaceholder();

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.creamDeep.withValues(alpha: 0.6),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.divider),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          const Icon(
            Icons.add_photo_alternate_outlined,
            size: 26,
            color: AppColors.muted,
          ),
          const SizedBox(height: 6),
          Text(
            'Add a cover photo',
            style: AppTypography.style(
              size: 10.5,
              weight: FontWeight.w600,
              color: AppColors.muted,
            ),
          ),
        ],
      ),
    );
  }
}

class _FieldLabel extends StatelessWidget {
  const _FieldLabel(this.label);

  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 5),
      child: Text(
        label,
        style: AppTypography.style(
          size: 10,
          weight: FontWeight.w700,
          letterSpacing: -0.05,
          color: AppColors.ink,
        ),
      ),
    );
  }
}

class _CategoryField extends StatelessWidget {
  const _CategoryField({
    required this.value,
    required this.categories,
    required this.onChanged,
  });

  final String? value;
  final List<String> categories;
  final ValueChanged<String?> onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 42,
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(11),
        border: Border.all(color: AppColors.divider),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: value,
          isExpanded: true,
          hint: Text(
            'Select a category',
            style: AppTypography.style(
              size: 12.5,
              weight: FontWeight.w500,
              color: AppColors.mist,
            ),
          ),
          icon: const Icon(
            Icons.keyboard_arrow_down_rounded,
            size: 20,
            color: AppColors.muted,
          ),
          style: AppTypography.style(
            size: 13,
            weight: FontWeight.w600,
            color: AppColors.ink,
          ),
          dropdownColor: AppColors.white,
          borderRadius: BorderRadius.circular(12),
          items: <DropdownMenuItem<String>>[
            for (final String category in categories)
              DropdownMenuItem<String>(value: category, child: Text(category)),
          ],
          onChanged: onChanged,
        ),
      ),
    );
  }
}

class _SavingsBreakdown extends StatelessWidget {
  const _SavingsBreakdown({
    required this.customerSavings,
    required this.savingsPercent,
    required this.netReceive,
    required this.commission,
  });

  final String customerSavings;
  final int savingsPercent;
  final String netReceive;
  final String commission;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(12, 11, 12, 10),
      decoration: BoxDecoration(
        color: AppColors.glowTint,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        children: <Widget>[
          _BreakdownRow(
            label: 'Customer saves',
            value: '$customerSavings · $savingsPercent%',
          ),
          const SizedBox(height: 6),
          _BreakdownRow(label: 'You receive after 10%', value: netReceive),
          const SizedBox(height: 7),
          Align(
            alignment: Alignment.centerLeft,
            child: Text(
              'WasteLess commission: $commission per sale',
              style: AppTypography.style(
                size: 7.5,
                weight: FontWeight.w500,
                color: AppColors.olive,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _BreakdownRow extends StatelessWidget {
  const _BreakdownRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        Text(
          label,
          style: AppTypography.style(
            size: 9.5,
            weight: FontWeight.w600,
            color: AppColors.forest.withValues(alpha: 0.72),
          ),
        ),
        const Spacer(),
        Text(
          value,
          style: AppTypography.style(
            size: 10.5,
            weight: FontWeight.w800,
            color: AppColors.forest,
          ),
        ),
      ],
    );
  }
}

class _PublishBar extends StatelessWidget {
  const _PublishBar({required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.cream,
      child: SafeArea(
        top: false,
        minimum: const EdgeInsets.only(bottom: 6),
        child: Center(
          heightFactor: 1,
          child: ConstrainedBox(
            constraints: const BoxConstraints(
              maxWidth: AppSpacing.contentMaxWidth,
            ),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
              child: _PublishButton(onPressed: onPressed),
            ),
          ),
        ),
      ),
    );
  }
}

class _PublishButton extends StatelessWidget {
  const _PublishButton({required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    const BorderRadius radius = BorderRadius.all(Radius.circular(13));

    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: radius,
        boxShadow: <BoxShadow>[
          BoxShadow(
            color: AppColors.forest.withValues(alpha: 0.22),
            blurRadius: 16,
            offset: const Offset(0, 7),
          ),
        ],
      ),
      child: Material(
        color: AppColors.forest,
        borderRadius: radius,
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onPressed,
          splashColor: AppColors.white.withValues(alpha: 0.14),
          child: SizedBox(
            height: 50,
            width: double.infinity,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Text(
                  'Publish offer',
                  style: AppTypography.style(
                    size: 13.5,
                    weight: FontWeight.w700,
                    letterSpacing: -0.1,
                    color: AppColors.white,
                  ),
                ),
                const SizedBox(width: 7),
                const Icon(
                  Icons.check_rounded,
                  size: 17,
                  color: AppColors.white,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
