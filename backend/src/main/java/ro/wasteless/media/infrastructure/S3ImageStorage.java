package ro.wasteless.media.infrastructure;

import java.io.*;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ro.wasteless.common.port.StoragePort;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Component
@ConditionalOnProperty(name = "app.storage-mode", havingValue = "s3")
public class S3ImageStorage implements StoragePort, AutoCloseable {
  private final S3Client client;
  private final String bucket;
  private final String publicUrl;

  public S3ImageStorage(
      @Value("${app.s3.bucket}") String bucket,
      @Value("${app.s3.public-url}") String publicUrl,
      @Value("${app.s3.region:eu-central-1}") String region,
      @Value("${app.s3.endpoint:}") String endpoint) {
    if (bucket.isBlank()
        || publicUrl.isBlank()
        || !java.util.Set.of("http", "https").contains(URI.create(publicUrl).getScheme()))
      throw new IllegalArgumentException(
          "S3_BUCKET and an absolute S3_PUBLIC_URL are required for S3 storage");
    this.bucket = bucket;
    this.publicUrl = publicUrl.replaceAll("/+$", "");
    var builder = S3Client.builder().region(Region.of(region));
    if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
    client =
        builder.build(); // Standard AWS credentials chain; never persist credentials in application
    // config.
  }

  @Override
  public String store(String filename, String type, InputStream content) {
    validate(filename);
    try {
      byte[] bytes = content.readNBytes(5 * 1024 * 1024 + 1);
      if (bytes.length > 5 * 1024 * 1024) throw new IllegalArgumentException("Image exceeds 5 MB");
      client.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key("images/" + filename)
              .contentType(type)
              .build(),
          RequestBody.fromBytes(bytes));
      return publicUrl + "/images/" + filename;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void delete(String key) {
    validate(key);
    client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key("images/" + key).build());
  }

  private static void validate(String key) {
    if (key == null || !key.matches("[a-f0-9-]{36}\\.(jpg|png|webp)"))
      throw new IllegalArgumentException("Invalid media key");
  }

  @Override
  public void close() {
    client.close();
  }
}
