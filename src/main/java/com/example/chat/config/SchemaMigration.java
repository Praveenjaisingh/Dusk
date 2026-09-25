package com.example.chat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate 6 creates a CHECK constraint listing every value of an enum column
 * (messages.message_type) when it first creates the table. spring.jpa.hibernate.ddl-auto=update
 * never modifies an existing constraint, so on a database created before AUDIO / VOICE were
 * added to Message.MessageType, inserting a voice message would fail with
 * "violates check constraint messages_message_type_check".
 *
 * This drops that one constraint (idempotent; a no-op once it is gone). The application
 * already validates messageType, so nothing is lost. If you later adopt Flyway/Liquibase,
 * replace this class with a proper migration.
 */
@Component
public class SchemaMigration {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void dropMessageTypeCheckConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_message_type_check");
        } catch (Exception ex) {
            log.warn("Could not relax messages.message_type check constraint (safe to ignore on a fresh database): {}",
                    ex.getMessage());
        }
    }
}
