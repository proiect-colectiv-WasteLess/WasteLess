import http from "k6/http";
import { check } from "k6";

const sample = JSON.parse(open("./fixtures/offer.json"));
const body = JSON.stringify({
  offers: Array.from({ length: 50 }, (_, index) => ({ ...sample, offerId: "load-" + index })),
});
export const options = {
  scenarios: {
    batch: {
      executor: "constant-arrival-rate",
      rate: 50,
      timeUnit: "1s",
      duration: "60s",
      preAllocatedVUs: 20,
      maxVUs: 40,
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<100"],
    http_req_failed: ["rate<0.01"],
    dropped_iterations: ["count<1"],
    checks: ["rate>0.99"],
  },
  summaryTrendStats: ["avg", "med", "p(95)", "p(99)", "max"],
};

export default function () {
  const response = http.post((__ENV.PREDICTION_URL || "http://localhost:8000") + "/predict", body, {
    headers: { "Content-Type": "application/json" },
    timeout: "500ms",
  });
  check(response, { "50 forecasts returned": (r) => r.status === 200 && r.json("predictions").length === 50 });
}

export function handleSummary(data) {
  const stats = data.metrics.http_req_duration.values;
  const checks = data.metrics.checks.values;
  const text = "# Inference load smoke\n\n50 requests/s for 60 s, 50 offers per request.\n\n" +
    "| p50 | p95 | p99 |\n| --- | --- | --- |\n" +
    "| " + stats.med.toFixed(2) + " ms | " + stats["p(95)"].toFixed(2) + " ms | " +
    stats["p(99)"].toFixed(2) + " ms |\n\n" +
    "Response checks passed: " + checks.passes + ". Failed: " + checks.fails + ".\n";
  return { stdout: text, "docs/load-report.md": text };
}
