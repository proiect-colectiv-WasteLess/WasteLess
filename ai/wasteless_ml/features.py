import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

NUMERIC = [
    "price",
    "originalPrice",
    "discount",
    "quantityTotal",
    "quantityAvailable",
    "pickupMinutes",
    "pickupHour",
    "pickupWeekday",
    "offerAgeMinutes",
    "businessSellThrough",
    "categoryHourSellThrough",
    "businessSize",
]
FEATURES = NUMERIC + ["category"]
LABELS = ["leftover_units", "unsold_flag", "sold_out_flag", "sell_through_ratio"]


def feature_frame(rows):
    start = pd.to_datetime(rows["pickupStart"], utc=True, format="mixed")
    end = pd.to_datetime(rows["pickupEnd"], utc=True, format="mixed")
    created = pd.to_datetime(rows["createdAt"], utc=True, format="mixed")
    scoring = pd.to_datetime(rows["scoredAt"], utc=True, format="mixed")
    numeric = rows.reindex(columns=NUMERIC).astype(float)
    original = numeric["originalPrice"].where(numeric["originalPrice"] > 0)
    numeric["discount"] = (1 - numeric["price"] / original).clip(0, 1)
    numeric["pickupMinutes"] = (end - start).dt.total_seconds() / 60
    numeric["pickupHour"] = start.dt.hour
    numeric["pickupWeekday"] = start.dt.dayofweek
    numeric["offerAgeMinutes"] = (scoring - created).dt.total_seconds() / 60
    numeric["category"] = rows["category"].fillna("__missing__")
    assert not set(numeric).intersection(LABELS)
    return numeric.replace([np.inf, -np.inf], np.nan)


def transformer():
    return ColumnTransformer(
        [
            (
                "numbers",
                make_pipeline(
                    SimpleImputer(strategy="median", keep_empty_features=True),
                    StandardScaler(),
                ),
                NUMERIC,
            ),
            (
                "category",
                OneHotEncoder(handle_unknown="ignore", sparse_output=False),
                ["category"],
            ),
        ]
    )
