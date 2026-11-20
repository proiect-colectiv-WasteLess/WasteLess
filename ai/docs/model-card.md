# Unsold offer model card

The model forecasts leftover units at the end of an offer's pickup window and the probability of a positive leftover count. It supports reporting to the business that owns the offer. The outputs should never drive automatic prices, discounts or promotions.

The shared transform uses the offer's price, original price, discount, category, stock, pickup window and time, offer age, optional business size and prior sell-through. See the feature audit and the per-version experiment.json for encoded feature importances.

The model is a CPU LightGBM regressor and classifier with Platt calibration. Training and validation simulate the publication-time snapshot. The version's evaluation includes time-based test metrics by slice, synthetic share and performance gates. Accuracy from synthetic examples cannot establish performance on real businesses.

Cold-start estimates are sensitive to synthetic demand assumptions and missing historical features. Weather and external promotions are outside the model. Business effects and pickup-time patterns can shift after training. The service reports estimates rather than guarantees.
