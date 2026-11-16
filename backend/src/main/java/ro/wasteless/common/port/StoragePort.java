package ro.wasteless.common.port;

public interface StoragePort {
  String store(String filename, String contentType, java.io.InputStream content);

  void delete(String key);
}
