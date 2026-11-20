package ro.wasteless.media.api;

import java.nio.file.Files;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.common.api.*;
import ro.wasteless.media.infrastructure.LocalImageStorage;

@RestController
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "app.storage-mode", havingValue = "local", matchIfMissing = true)
public class LocalMediaController {
  private final LocalImageStorage storage;

  public LocalMediaController(LocalImageStorage storage) {
    this.storage = storage;
  }

  @GetMapping("/media/images/{name}")
  public ResponseEntity<Resource> image(@PathVariable String name) {
    java.nio.file.Path path;
    try {
      path = storage.resolve(name);
    } catch (IllegalArgumentException e) {
      throw new DomainException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Image not found");
    }
    if (!Files.isRegularFile(path))
      throw new DomainException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Image not found");
    var type =
        name.endsWith(".png") ? "image/png" : name.endsWith(".webp") ? "image/webp" : "image/jpeg";
    return ResponseEntity.ok()
        .header("X-Content-Type-Options", "nosniff")
        .contentType(MediaType.parseMediaType(type))
        .body(new FileSystemResource(path));
  }
}
