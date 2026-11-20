package ro.wasteless;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import ro.wasteless.common.api.DomainException;
import ro.wasteless.common.port.StoragePort;
import ro.wasteless.media.api.ImageUploadService;
import ro.wasteless.media.infrastructure.LocalImageStorage;

class MediaUploadTest {
  @TempDir Path directory;

  static byte[] png() {
    return new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0};
  }

  @Test
  void uploadUsesSafeGeneratedNameAndAbsoluteUrl() throws Exception {
    var storage = new LocalImageStorage(directory.toString(), "https://example.test");
    var url =
        new ImageUploadService(storage)
            .upload(new MockMultipartFile("file", "../../evil.html", "image/png", png()));
    assertThat(url).startsWith("https://example.test/media/images/").endsWith(".png");
    var key = url.substring(url.lastIndexOf('/') + 1);
    assertThat(Files.readAllBytes(storage.resolve(key))).isEqualTo(png());
    assertThatThrownBy(() -> storage.resolve("../evil.png"))
        .isInstanceOf(IllegalArgumentException.class);
    storage.delete(key);
    assertThat(Files.exists(storage.resolve(key))).isFalse();
  }

  @Test
  void mismatchedMagicUnsupportedAndOversizedFilesNeverReachStorage() {
    var storage = mock(StoragePort.class);
    var service = new ImageUploadService(storage);
    for (var file :
        java.util.List.of(
            new MockMultipartFile("file", "bad.jpg", "image/jpeg", png()),
            new MockMultipartFile("file", "bad.svg", "image/svg+xml", "<svg/>".getBytes()),
            new MockMultipartFile("file", "empty.png", "image/png", new byte[0]),
            new MockMultipartFile(
                "file", "huge.png", "image/png", new byte[ImageUploadService.MAX_BYTES + 1]))) {
      assertThatThrownBy(() -> service.upload(file)).isInstanceOf(DomainException.class);
    }
    verifyNoInteractions(storage);
    assertThat(
            ImageUploadService.extension(
                new byte[] {(byte) 255, (byte) 216, (byte) 255}, "image/jpeg"))
        .isEqualTo("jpg");
    assertThat(ImageUploadService.extension("RIFF1234WEBP".getBytes(), "image/webp"))
        .isEqualTo("webp");
  }
}
