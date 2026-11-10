import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_typography.dart';
import '../../domain/models/business_offer.dart';

/// What the user chose in the offer details sheet.
enum OfferDetailsAction { edit, delete }

Future<OfferDetailsAction?> showOfferDetails(
  BuildContext context,
  BusinessOffer offer,
) {
  return showModalBottomSheet<OfferDetailsAction>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    backgroundColor: Colors.transparent,
    barrierColor: AppColors.forestDeep.withValues(alpha: 0.45),
    builder: (BuildContext context) => _OfferDetailsSheet(offer: offer),
  );
}

Future<BusinessOffer?> showEditOfferDialog(
  BuildContext context,
  BusinessOffer offer,
) {
  return showDialog<BusinessOffer>(
    context: context,
    barrierColor: AppColors.forestDeep.withValues(alpha: 0.45),
    builder: (BuildContext context) => _EditOfferDialog(offer: offer),
  );
}

Future<bool> confirmDeleteOffer(
  BuildContext context,
  BusinessOffer offer,
) async {
  final bool? result = await showDialog<bool>(
    context: context,
    barrierColor: AppColors.forestDeep.withValues(alpha: 0.45),
    builder: (BuildContext context) => AlertDialog(
      backgroundColor: AppColors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      title: Text(
        'Delete offer?',
        style: AppTypography.style(
          size: 18,
          weight: FontWeight.w800,
          letterSpacing: -0.4,
        ),
      ),
      content: Text(
        '“${offer.title}” will be removed from your active offers.',
        style: AppTypography.style(
          size: 13.5,
          height: 1.5,
          color: AppColors.muted,
        ),
      ),
      actionsPadding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: Text(
            'Cancel',
            style: AppTypography.style(
              size: 13.5,
              weight: FontWeight.w700,
              color: AppColors.muted,
            ),
          ),
        ),
        FilledButton(
          onPressed: () => Navigator.of(context).pop(true),
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.danger,
            foregroundColor: AppColors.white,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
          ),
          child: Text(
            'Delete offer',
            style: AppTypography.style(
              size: 13,
              weight: FontWeight.w700,
              color: AppColors.white,
            ),
          ),
        ),
      ],
    ),
  );

  return result ?? false;
}

class _OfferDetailsSheet extends StatelessWidget {
  const _OfferDetailsSheet({required this.offer});

  final BusinessOffer offer;

  @override
  Widget build(BuildContext context) {
    final bool paused = offer.isPaused;

    return Container(
      decoration: const BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      padding: const EdgeInsets.fromLTRB(24, 10, 24, 20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          Center(
            child: Container(
              width: 42,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.divider,
                borderRadius: BorderRadius.circular(999),
              ),
            ),
          ),
          const SizedBox(height: 20),
          Center(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(20),
              child: Image.asset(
                offer.imageAsset,
                width: 88,
                height: 88,
                fit: BoxFit.cover,
                cacheWidth: 176,
                semanticLabel: offer.title,
              ),
            ),
          ),
          const SizedBox(height: 14),
          Text(
            offer.title,
            textAlign: TextAlign.center,
            style: AppTypography.style(
              size: 20,
              weight: FontWeight.w800,
              letterSpacing: -0.5,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              _StatusPill(offer: offer),
              const SizedBox(width: 8),
              Text(
                offer.priceLabel,
                style: AppTypography.style(
                  size: 15,
                  weight: FontWeight.w800,
                  color: AppColors.forest,
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            decoration: BoxDecoration(
              color: AppColors.canvas,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              children: <Widget>[
                _DetailRow(label: 'Available', value: offer.availabilityLabel),
                const Divider(height: 1, thickness: 1, color: AppColors.divider),
                _DetailRow(label: 'Stock level', value: _stockDescription),
                const Divider(height: 1, thickness: 1, color: AppColors.divider),
                _DetailRow(
                  label: 'Status',
                  value: paused ? 'Paused' : 'Active',
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          Row(
            children: <Widget>[
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => Navigator.of(
                    context,
                  ).pop(OfferDetailsAction.edit),
                  icon: const Icon(Icons.edit_outlined, size: 17),
                  label: const Text('Edit offer'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.forest,
                    side: const BorderSide(color: AppColors.divider),
                    minimumSize: const Size.fromHeight(46),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(13),
                    ),
                    textStyle: AppTypography.style(
                      size: 13.5,
                      weight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: TextButton.icon(
                  onPressed: () => Navigator.of(
                    context,
                  ).pop(OfferDetailsAction.delete),
                  icon: const Icon(Icons.delete_outline_rounded, size: 17),
                  label: const Text('Delete'),
                  style: TextButton.styleFrom(
                    foregroundColor: AppColors.danger,
                    minimumSize: const Size.fromHeight(46),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(13),
                    ),
                    textStyle: AppTypography.style(
                      size: 13.5,
                      weight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  String get _stockDescription {
    final String level = switch (offer.stockLevel) {
      OfferStockLevel.low => 'Low',
      OfferStockLevel.medium => 'Medium',
      OfferStockLevel.high => 'High',
    };
    return '$level · ${offer.availabilityLabel}';
  }
}

class _StatusPill extends StatelessWidget {
  const _StatusPill({required this.offer});

  final BusinessOffer offer;

  @override
  Widget build(BuildContext context) {
    final bool paused = offer.isPaused;
    final bool low = offer.stockLevel == OfferStockLevel.low;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: paused
            ? AppColors.infoTint
            : low
            ? AppColors.amberTint
            : AppColors.sageTint,
        borderRadius: BorderRadius.circular(7),
      ),
      child: Text(
        paused ? 'PAUSED' : offer.stockLabel,
        style: AppTypography.style(
          size: 9,
          weight: FontWeight.w800,
          letterSpacing: 0.7,
          color: paused
              ? AppColors.info
              : low
              ? AppColors.amberInk
              : AppColors.forest,
        ),
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: <Widget>[
          Text(
            label,
            style: AppTypography.style(
              size: 12.5,
              weight: FontWeight.w500,
              color: AppColors.muted,
            ),
          ),
          const Spacer(),
          Text(
            value,
            style: AppTypography.style(
              size: 12.5,
              weight: FontWeight.w700,
              color: AppColors.ink,
            ),
          ),
        ],
      ),
    );
  }
}

class _EditOfferDialog extends StatefulWidget {
  const _EditOfferDialog({required this.offer});

  final BusinessOffer offer;

  @override
  State<_EditOfferDialog> createState() => _EditOfferDialogState();
}

class _EditOfferDialogState extends State<_EditOfferDialog> {
  late final TextEditingController _titleController;
  late final TextEditingController _availableController;
  late final TextEditingController _priceController;
  String? _error;

  @override
  void initState() {
    super.initState();
    _titleController = TextEditingController(text: widget.offer.title);
    _availableController = TextEditingController(
      text: widget.offer.available.toString(),
    );
    _priceController = TextEditingController(
      text: widget.offer.price.toStringAsFixed(2),
    );
  }

  @override
  void dispose() {
    _titleController.dispose();
    _availableController.dispose();
    _priceController.dispose();
    super.dispose();
  }

  void _save() {
    final String title = _titleController.text.trim();
    final int? available = int.tryParse(_availableController.text.trim());
    final double? price = double.tryParse(
      _priceController.text.trim().replaceAll(',', '.'),
    );

    if (title.isEmpty ||
        available == null ||
        available < 0 ||
        price == null ||
        price <= 0) {
      setState(
        () => _error = 'Add a name, stock count and a price above zero.',
      );
      return;
    }

    Navigator.of(context).pop(
      widget.offer.copyWith(
        title: title,
        available: available,
        price: price,
        stockLevel: _stockFor(available),
      ),
    );
  }

  OfferStockLevel _stockFor(int available) {
    if (available <= 10) {
      return OfferStockLevel.low;
    }
    if (available <= 25) {
      return OfferStockLevel.medium;
    }
    return OfferStockLevel.high;
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      insetPadding: const EdgeInsets.symmetric(horizontal: 22, vertical: 24),
      title: Text(
        'Edit offer',
        style: AppTypography.style(
          size: 18,
          weight: FontWeight.w800,
          letterSpacing: -0.4,
        ),
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            _DialogField(
              label: 'Offer name',
              controller: _titleController,
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: 12),
            Row(
              children: <Widget>[
                Expanded(
                  child: _DialogField(
                    label: 'Items left',
                    controller: _availableController,
                    keyboardType: TextInputType.number,
                    textInputAction: TextInputAction.next,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _DialogField(
                    label: 'Price (€)',
                    controller: _priceController,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    textInputAction: TextInputAction.done,
                    onSubmitted: (_) => _save(),
                  ),
                ),
              ],
            ),
            if (_error != null) ...<Widget>[
              const SizedBox(height: 10),
              Text(
                _error!,
                style: AppTypography.style(
                  size: 11.5,
                  weight: FontWeight.w600,
                  color: AppColors.danger,
                ),
              ),
            ],
          ],
        ),
      ),
      actionsPadding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(
            'Cancel',
            style: AppTypography.style(
              size: 13.5,
              weight: FontWeight.w700,
              color: AppColors.muted,
            ),
          ),
        ),
        FilledButton(
          onPressed: _save,
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.forest,
            foregroundColor: AppColors.white,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
          ),
          child: Text(
            'Save changes',
            style: AppTypography.style(
              size: 13,
              weight: FontWeight.w700,
              color: AppColors.white,
            ),
          ),
        ),
      ],
    );
  }
}

class _DialogField extends StatelessWidget {
  const _DialogField({
    required this.label,
    required this.controller,
    this.keyboardType,
    this.textInputAction,
    this.onSubmitted,
  });

  final String label;
  final TextEditingController controller;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final ValueChanged<String>? onSubmitted;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      keyboardType: keyboardType,
      textInputAction: textInputAction,
      onSubmitted: onSubmitted,
      style: AppTypography.style(
        size: 14,
        weight: FontWeight.w600,
        color: AppColors.ink,
      ),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: AppTypography.style(
          size: 12.5,
          weight: FontWeight.w500,
          color: AppColors.muted,
        ),
        filled: true,
        fillColor: AppColors.canvas,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 12,
          vertical: 13,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.divider),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.forest, width: 1.4),
        ),
      ),
    );
  }
}
