package ro.wasteless.media.api;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media/images")
public class MediaController {
  private final ImageUploadService uploads;

  public MediaController(ImageUploadService uploads) {
    this.uploads = uploads;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('BUSINESS')")
  public UploadResponse upload(@RequestPart("file") MultipartFile file) {
    return new UploadResponse(uploads.upload(file));
  }

  public record UploadResponse(String url) {}
}
