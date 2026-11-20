from datetime import datetime
from enum import StrEnum

import numpy as np
from pydantic import AwareDatetime, BaseModel, ConfigDict, Field, model_validator

MAX_OFFERS = 50
BUDGET_SECONDS = 0.5
MAX_BODY_BYTES = 128 * 1024
RISK_THRESHOLDS = {
    "HIGH": {"probability": 0.60, "stockFraction": 0.50},
    "MEDIUM": {"probability": 0.30, "stockFraction": 0.25},
}


class Offer(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)

    offerId: str = Field(min_length=1, max_length=128)
    price: float = Field(ge=0)
    originalPrice: float | None = Field(default=None, ge=0)
    category: str = Field(min_length=1, max_length=100)
    quantityTotal: int = Field(ge=0, le=2**31 - 1)
    quantityAvailable: int = Field(ge=0, le=2**31 - 1)
    pickupStart: AwareDatetime
    pickupEnd: AwareDatetime
    createdAt: AwareDatetime
    scoredAt: AwareDatetime
    businessSellThrough: float | None = Field(default=None, ge=0, le=1)
    categoryHourSellThrough: float | None = Field(default=None, ge=0, le=1)
    businessSize: int | None = Field(default=None, ge=0)

    @model_validator(mode="after")
    def validate_consistency(self):
        if self.quantityAvailable > self.quantityTotal:
            raise ValueError("quantityAvailable must not exceed quantityTotal")
        if self.createdAt > self.scoredAt or self.pickupEnd <= self.pickupStart:
            raise ValueError("timestamps must describe a valid offer and scoring time")
        return self


class Batch(BaseModel):
    model_config = ConfigDict(extra="forbid")
    offers: list[Offer] = Field(max_length=MAX_OFFERS)


class Risk(StrEnum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


class Forecast(BaseModel):
    offerId: str
    expectedLeftoverUnits: float
    unsoldProbability: float
    riskLevel: Risk
    modelVersion: str
    generatedAt: datetime


class Predictions(BaseModel):
    predictions: list[Forecast]


def bound_predictions(leftovers, probability, stock):
    leftovers = np.clip(leftovers, 0, stock)
    probability = np.where(np.asarray(stock) == 0, 0, np.clip(probability, 0, 1))
    return leftovers, probability


def risk_level(leftovers, probability, stock):
    if stock == 0:
        return Risk.LOW
    for risk, limits in RISK_THRESHOLDS.items():
        if (
            probability >= limits["probability"]
            or leftovers / stock >= limits["stockFraction"]
        ):
            return Risk(risk)
    return Risk.LOW
