# Inference load smoke

50 requests/s for 60 s, 50 offers per request.

Local run on Windows 11 with an AMD Ryzen 5 5600X, Python 3.12.16, model 0.1.0 and k6 1.7.0. The Uvicorn service used one OpenMP thread, one OpenBLAS thread and LOG_LEVEL=WARNING. The CI workflow also runs this scenario against the Linux container.

| p50 | p95 | p99 |
| --- | --- | --- |
| 9.57 ms | 12.17 ms | 14.97 ms |

Response checks passed: 3001. Failed: 0.
