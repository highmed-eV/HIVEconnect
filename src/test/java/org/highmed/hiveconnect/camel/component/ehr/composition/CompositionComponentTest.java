package org.highmed.hiveconnect.camel.component.ehr.composition;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.Registry;
import org.highmed.hiveconnect.config.DebugProperties;

import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositionComponentTest {

    @Mock
    private CamelContext camelContext;

    @Mock
    private Registry registry;

    @Mock
    private OpenEhrClient openEhrClient;

    @Mock
    private DebugProperties debugProperties;

    @InjectMocks
    private CompositionComponent compositionComponent;

    @BeforeEach
    void setUp() {
        when(camelContext.getRegistry()).thenReturn(registry);
    }

    @Test
    void doInitAutoWiresOpenEhrClient() throws Exception {
        when(registry.findByType(OpenEhrClient.class)).thenReturn(Collections.singleton(openEhrClient));
        compositionComponent.doInit();
        assertNotNull(compositionComponent.getOpenEhrClient());
    }

    @Test
    void doInitMultipleOpenEhrClientsNotAutoWired() throws Exception {
        when(registry.findByType(OpenEhrClient.class)).thenReturn(Set.of(openEhrClient, mock(OpenEhrClient.class)));
        compositionComponent.doInit();
        assertNull(compositionComponent.getOpenEhrClient());
    }

    @Test
    void testCreateEndpointSuccess() throws Exception {
        when(registry.lookupByNameAndType("debugProperties", DebugProperties.class)).thenReturn(debugProperties);

        Endpoint endpoint = compositionComponent.createEndpoint("composition://test", "test", Map.of());

        assertNotNull(endpoint);
        assertTrue(endpoint instanceof CompositionEndpoint);
    }



}

