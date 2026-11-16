package ro.wasteless.order.service;

import org.slf4j.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

@Component
public class OrderEvents {
  private static final Logger log = LoggerFactory.getLogger(OrderEvents.class);
  private final ApplicationEventPublisher publisher;

  public OrderEvents(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  public void afterCommit(Object event) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Order event requires transaction");
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              publisher.publishEvent(event);
            } catch (RuntimeException e) {
              log.error(
                  "Committed order event listener failed for {}", event.getClass().getSimpleName());
            }
          }
        });
  }
}
