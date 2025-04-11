package org.ehrbase.fhirbridge.web;

import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.classic.Level;
import jakarta.annotation.Nonnull;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

/**
 * API endpoint to get or set on the fly LogLevel.
 */
@RestController
@RequestMapping(
        path = "/management",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
public class ManagementController {


    @GetMapping("/log-level")
    public ResponseEntity<Level> getLogLevel() {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
        return ResponseEntity.ok(rootLogger.getLevel());
    }

    @PostMapping("/log-level/{logLevel}")
    public ResponseEntity<Level> setLogLevel(@Nonnull @PathVariable String logLevel) {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.valueOf(logLevel));//Default log level is DEBUG. If {logLevel} == Wrong Status
        return ResponseEntity.ok(rootLogger.getLevel());
    }
}
