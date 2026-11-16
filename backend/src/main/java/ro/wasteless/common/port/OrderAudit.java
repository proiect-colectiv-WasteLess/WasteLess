package ro.wasteless.common.port;

import java.util.UUID;

public interface OrderAudit {
  void append(UUID order, String fromStatus, String toStatus, UUID actor, String reason);
}
