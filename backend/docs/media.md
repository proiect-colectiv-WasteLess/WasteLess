# Offer images (WAS-40)

`POST /api/v1/media/images`, BUSINESS only, accepts a multipart `file` containing JPEG, PNG or WebP, up to 5 MiB. The MIME type must match the header signature. Original filenames are ignored. The response is `{url}`; use that absolute URL as `imageAsset` in offer writes and as `imageAsset` in Flutter responses. The frontend still needs to support network images when replacing AppStore data access.

Local development/test storage uses `STORAGE_PATH` and `PUBLIC_BASE_URL` (default http://localhost:8080). Generated images are publicly readable at `/media/images/{uuid}.{extension}` in dev/test only. Never enable the dev profile in production. Local storage must be on a trusted application-owned directory.

Production defaults to `STORAGE_MODE=s3`. Set `S3_BUCKET`, `S3_PUBLIC_URL` (public bucket/CDN base URL), `AWS_REGION` and optionally `S3_ENDPOINT` for an S3-compatible server. Credentials use the AWS default provider chain (workload role preferred). Configure public image reads/CDN separately; the application does not create buckets, permissions or credentials. A production bucket is not available in this workspace, so real S3 connectivity has not been validated.

## Cleanup policy

Keep images referenced by any offer (including soft-deleted offers) or historical order snapshot. Never remove an image in the offer deletion transaction: that would break order history. Operations must run a weekly orphan sweep: list only application-generated `images/{uuid}.{jpg,png,webp}` objects/files older than seven days, compare absolute URLs against both `offers.image_url` and `orders.image_url`, then remove only unreferenced objects. Recheck references immediately before deletion. The seven-day grace protects uploads awaiting offer publication; a stale upload URL must be uploaded again after cleanup. Record counts only. This is an operational policy, not an automatically deployed cleanup job; assign and schedule the sweep with the platform owner before production rollout.

Do not apply an unconditional bucket age rule to all images: live offers and financial history may reference old images. Preserve the same object URLs when switching from local storage to S3, or migrate the references before retiring local files.
