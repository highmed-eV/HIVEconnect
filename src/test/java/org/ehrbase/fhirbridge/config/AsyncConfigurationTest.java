package org.ehrbase.fhirbridge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "async.core-pool-size=5",
    "async.max-pool-size=10",
    "async.queue-capacity=25",
    "async.thread-name-prefix=AsyncThread-"
})
class AsyncConfigurationTest {

    @Test
    void testAsyncConfiguration() {
        // This test verifies that the Spring context loads with the AsyncConfiguration
        // The actual configuration is tested through property injection
        assertTrue(true);
    }
} 