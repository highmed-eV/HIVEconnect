package org.ehrbase.fhirbridge.camel.component.ehr.composition;

import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.ehrbase.fhirbridge.camel.component.ehr.EhrConfiguration;
import org.ehrbase.fhirbridge.config.DebugProperties;
// import org.ehrbase.fhirbridge.openehr.openehrclient.OpenEhrClient;
import org.ehrbase.client.openehrclient.OpenEhrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositionEndpointTest {

    @Mock
    private CompositionComponent component;

    @Mock
    private EhrConfiguration configuration;

    private CompositionEndpoint compositionEndpoint;

    @BeforeEach
    void setUp() {
        compositionEndpoint = new CompositionEndpoint("ehr-composition://test", component, configuration);
    }

    @Test
    void createProducer() {
        Producer producer = compositionEndpoint.createProducer();
        assertNotNull(producer);
        assertTrue(producer instanceof CompositionProducer);
    }

    @Test
    void createConsumerThrowException() {
        Processor processor = mock(Processor.class);
        assertThrows(UnsupportedOperationException.class, () -> compositionEndpoint.createConsumer(processor));
    }

    @Test
    void testIsSingleton() {
        assertTrue(compositionEndpoint.isSingleton());
    }

    @Test
    void setAndGetName() {
        compositionEndpoint.setName("testName");
        assertEquals("testName", compositionEndpoint.getName());
    }

    @Test
    void setAndGetOperation() {
        CompositionOperation operation = CompositionOperation.find;
        compositionEndpoint.setOperation(operation);
        assertEquals(operation, compositionEndpoint.getOperation());
    }

    @Test
    void setAndGetExpectedType() {
        compositionEndpoint.setExpectedType(String.class);
        assertEquals(String.class, compositionEndpoint.getExpectedType());
    }

    @Test
    void setAndGetOpenEhrClient() {
        OpenEhrClient openEhrClient = mock(OpenEhrClient.class);
        when(configuration.getOpenEhrClient()).thenReturn(openEhrClient);
        compositionEndpoint.setOpenEhrClient(openEhrClient);
        assertEquals(openEhrClient, compositionEndpoint.getOpenEhrClient());
    }

    @Test
    void setAndGetProperties() {
        DebugProperties properties = mock(DebugProperties.class);
        compositionEndpoint.setProperties(properties);
        assertEquals(properties, compositionEndpoint.getProperties());
    }
}

