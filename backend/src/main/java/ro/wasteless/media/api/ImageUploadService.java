package ro.wasteless.media.api;

import java.io.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ro.wasteless.common.api.*;
import ro.wasteless.common.port.StoragePort;

@Service
public class ImageUploadService {
  public static final int MAX_BYTES = 5 * 1024 * 1024;
  private final StoragePort storage;

  public ImageUploadService(StoragePort storage) {
    this.storage = storage;
  }

  public String upload(MultipartFile file) {
    if (file.isEmpty() || file.getSize() > MAX_BYTES)
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          HttpStatus.BAD_REQUEST,
          "Image must contain between 1 byte and 5 MB");
    try (var input = file.getInputStream()) {
      byte[] bytes = input.readNBytes(MAX_BYTES + 1);
      if (bytes.length > MAX_BYTES)
        throw new DomainException(
            ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Image exceeds 5 MB");
      var extension = extension(bytes, file.getContentType());
      return storage.store(
          UUID.randomUUID() + "." + extension,
          file.getContentType(),
          new ByteArrayInputStream(bytes));
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read image", e);
    }
  }

  public static String extension(byte[] b, String type) {
    if ("image/jpeg".equals(type)
        && b.length >= 3
        && (b[0] & 255) == 255
        && (b[1] & 255) == 216
        && (b[2] & 255) == 255) return "jpg";
    if ("image/png".equals(type)
        && b.length >= 8
        && Arrays.equals(Arrays.copyOf(b, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
      return "png";
    if ("image/webp".equals(type)
        && b.length >= 12
        && new String(b, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
        && new String(b, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP"))
      return "webp";
    throw new DomainException(
        ErrorCode.VALIDATION_FAILED,
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "Expected a JPEG, PNG or WebP image with matching content type");
  }
}
