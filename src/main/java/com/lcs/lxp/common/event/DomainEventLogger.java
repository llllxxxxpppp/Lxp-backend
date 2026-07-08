package com.lcs.lxp.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DomainEventLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainEventLogger.class);

    @EventListener
    public void handle(BaseDomainEvent event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Domain event published: type={}, eventId={}, occurredAt={}",
                    event.getClass().getSimpleName(), event.getEventId(), event.getOccurredAt());
        }
    }
}
