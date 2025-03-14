package org.ehrbase.fhirbridge.fhir.camel;

import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

import org.hl7.fhir.r4.model.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(RequestDetailsLookupProcessor.BEAN_ID)
public class RequestDetailsLookupProcessor implements FhirRequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RequestDetailsLookupProcessor.class);
    
    public static final String BEAN_ID = "requestDetailsLookupProcessor";

    @Override
    public void process(Exchange exchange) throws Exception {
        // Get RequestDetails from the body
        ServletRequestDetails requestDetails = exchange.getIn().getBody(ServletRequestDetails.class);
        
        if (requestDetails == null) {
            throw new IllegalArgumentException("RequestDetails is null");
        }

        // Get operation type
        RestOperationTypeEnum operationType = requestDetails.getRestOperationType();
        if (operationType == null) {
            throw new IllegalArgumentException("Rest OperationType is null");
        }

        String httpMethod = requestDetails.getRequestType().name();
        String resourceName = requestDetails.getResourceName();
        String path = requestDetails.getRequestPath();
        String id = requestDetails.getId() != null ? requestDetails.getId().getIdPart() : null;
        Resource resource = null;
        // Handle resource based on HTTP method
        if (!"GET".equals(httpMethod)) {
            if (requestDetails.getResource() == null) {
                throw new IllegalArgumentException("Resource is null for non-GET request");
            }
            // Process resource only for non-GET requests when resource exists
            //Inut as String is required for logging: DebugProperties
            resource = (Resource) requestDetails.getResource();
            FhirContext fhirContext = FhirContext.forR4();
            String inputResource = fhirContext.newJsonParser().encodeResourceToString(resource);
            exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING, inputResource);

            //Set the Resource extracted from requestDetails 
            exchange.getIn().setBody(resource);
        }
    
        String remoteSystemId = requestDetails.getServletRequest().getRemoteAddr();
        // Store in headers for route decision making
        exchange.getIn().setHeader(CamelConstants.REQUESTDETAILS_OPERATION_TYPE, operationType != null ? operationType.name() : null);
        exchange.getIn().setHeader(CamelConstants.REQUEST_HTTP_METHOD, httpMethod);
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE_TYPE, resourceName);
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, resource);
        exchange.getIn().setHeader(CamelConstants.REQUESTDETAILS_ID, id);
        // exchange.getIn().setHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID, remoteSystemId);
        
        // Store the entire RequestDetails object for later use if needed
        exchange.setProperty("RequestDetails", requestDetails);
       
        // Log all headers for debugging
        exchange.getIn().getHeaders().forEach((key, value) -> 
            LOG.info("Header: " + key + " = " + value));
    
 
        switch (operationType.name()) {
            case "CREATE":
            case "TRANSACTION":
                exchange.getIn().setHeader(CamelConstants.REQUESTDETAILS_OPERATION_TYPE, "CREATE");
                break;
            case "READ":
            case "VREAD":
                exchange.getIn().setHeader(CamelConstants.REQUESTDETAILS_OPERATION_TYPE, "READ");
                break;
            case "UPDATE":
                exchange.getIn().setHeader(CamelConstants.REQUESTDETAILS_OPERATION_TYPE, "UPDATE");
                break;
            case "SEARCH_TYPE":
                exchange.getIn().setHeader(CamelConstants.REQUESTDETAILS_OPERATION_TYPE, "SEARCH");
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + requestDetails.getRestOperationType());
        }
    }
}
