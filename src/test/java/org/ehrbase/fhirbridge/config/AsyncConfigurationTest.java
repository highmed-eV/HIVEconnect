package org.ehrbase.fhirbridge.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AsyncConfiguration.class)
class AsyncConfigurationTest {

    @Autowired
    private AsyncConfiguration asyncConfiguration;

    @Test
    void testAsyncConfiguration() {
        assertNotNull(asyncConfiguration);
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfiguration.getAsyncExecutor();
        assertNotNull(executor);
        
        assertEquals(3, executor.getCorePoolSize());
        assertEquals(4, executor.getMaxPoolSize());
        assertEquals("asyn-task-thread-", executor.getThreadNamePrefix());
    }
} 