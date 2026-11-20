package ro.wasteless.media.infrastructure;

import java.io.*;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ro.wasteless.common.port.StoragePort;

@Component
@Profile("!prod")
@ConditionalOnProperty(name = "app.storage-mode", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements StoragePort {
  private final Path root;
  private final String baseUrl;

  public LocalImageStorage(
      @Value("${app.storage-path:./data/media}") String path,
      @Value("${app.public-base-url:http://localhost:8080}") String baseUrl) {
    root = Path.of(path).toAbsolutePath().normalize();
    this.baseUrl = baseUrl.replaceAll("/+$", "");
  }

  public Path resolve(String name) {
    if (name == null || !name.matches("[a-f0-9-]{36}\\.(jpg|png|webp)"))
      throw new IllegalArgumentException("Invalid media key");
    var path = root.resolve(name).normalize();
    if (!path.startsWith(root)) throw new IllegalArgumentException("Invalid media key");
    return path;
  }

  @Override
  public String store(String filename, String type, InputStream content) {
    var path = resolve(filename);
    try {
      Files.createDirectories(root);
      Files.copy(content, path);
      return baseUrl + "/media/images/" + filename;
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to store image", e);
    }
  }

  @Override
  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to delete image", e);
    }
  }
}
