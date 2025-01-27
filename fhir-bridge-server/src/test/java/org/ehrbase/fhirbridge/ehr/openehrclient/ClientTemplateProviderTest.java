package org.ehrbase.fhirbridge.ehr.openehrclient;

import org.ehrbase.fhirbridge.openehr.openehrclient.ClientTemplateProvider;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestClient;
import org.ehrbase.fhirbridge.openehr.openehrclient.TemplateEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientTemplateProviderTest {

    @Mock
    private DefaultRestClient restClient;

    @Mock
    private TemplateEndpoint templateEndpoint;

    @Mock
    private ClientTemplateProvider clientTemplateProvider;

    @BeforeEach
    void setUp() {
        when(restClient.templateEndpoint()).thenReturn(templateEndpoint);
        clientTemplateProvider = new ClientTemplateProvider(restClient);
    }

    @Test
    void findTemplateWithValidTemplateId() {
        String templateId = "valid-template-id";
        OPERATIONALTEMPLATE template = mock(OPERATIONALTEMPLATE.class);
        when(templateEndpoint.findTemplate(templateId)).thenReturn(Optional.of(template));
        Optional<OPERATIONALTEMPLATE> result = clientTemplateProvider.find(templateId);
        assertTrue(result.isPresent(), "Template should be present");
        assertEquals(template, result.get(), "The returned template should match the expected template");
    }

    @Test
    void findTemplateWithInvalidTemplateId() {
        String templateId = "invalid-template-id";
        when(templateEndpoint.findTemplate(templateId)).thenReturn(Optional.empty());
        Optional<OPERATIONALTEMPLATE> result = clientTemplateProvider.find(templateId);
        assertFalse(result.isPresent(), "Template should not be present");
    }
}

