package org.highmed.hiveconnect.openfhir.openfhirclient;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.highmed.hiveconnect.camel.CamelConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenFHIRAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Exchange exchange;

    @InjectMocks
    private OpenFHIRAdapter openFHIRAdapter;

    private String inputResource;
    private String openEhrJson;

    @BeforeEach
    void setUp() {
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
        inputResource = "{ \"resourceType\": \"Patient\", \"id\": \"123\" }";
        openEhrJson = "{ \"openEhr\": \"converted data\" }";
    }

    @Test
    void convertToOpenEHRSuccess() {
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, inputResource);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(openEhrJson);

        String result = openFHIRAdapter.convertToOpenEHR(exchange);
        assertEquals(openEhrJson, result);
        verify(restTemplate, times(1)).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void convertToOpenEHRFailure() {
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, inputResource);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection error"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> openFHIRAdapter.convertToOpenEHR(exchange));
        assertEquals("Error in FHIR to openEHR conversion", thrown.getMessage());
    }
}

