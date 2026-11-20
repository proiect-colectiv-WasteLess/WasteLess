import numpy as np
import pandas as pd

from .contract import RISK_THRESHOLDS, bound_predictions


def fit(rows, features):
    training = features.assign(
        leftoverShare=(rows.leftover_units / rows.quantityAvailable.replace(0, np.nan)),
        unsold=rows.unsold_flag.astype(float),
    ).loc[rows.quantityAvailable.gt(0)]
    defaults = training[["leftoverShare", "unsold"]].mean().fillna(0.0).to_dict()
    groups = []
    for (category, hour), group in training.groupby(["category", "pickupHour"]):
        coefficients = (
            group[["leftoverShare", "unsold"]].sum() + 10 * pd.Series(defaults)
        ) / (len(group) + 10)
        groups.append(
            {"category": category, "pickupHour": int(hour), **coefficients.to_dict()}
        )
    return {
        "schemaVersion": 1,
        "riskThresholds": RISK_THRESHOLDS,
        "default": defaults,
        "groups": groups,
    }


def predict(table, features):
    index = {(item["category"], item["pickupHour"]): item for item in table["groups"]}
    coefficients = [
        index.get((category, hour), table["default"])
        for category, hour in zip(features.category, features.pickupHour, strict=True)
    ]
    stock = features.quantityAvailable.to_numpy()
    return bound_predictions(
        stock * np.asarray([item["leftoverShare"] for item in coefficients]),
        [item["unsold"] for item in coefficients],
        stock,
    )
