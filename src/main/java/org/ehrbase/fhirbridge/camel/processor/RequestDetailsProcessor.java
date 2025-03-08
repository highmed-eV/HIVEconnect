package org.ehrbase.fhirbridge.camel.processor;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RequestDetailsProcessor implements Processor {
    
    private static final Logger log = LoggerFactory.getLogger(RequestDetailsProcessor.class);
    
    @Override
    public void process(Exchange exchange) {
        // Get RequestDetails from the body
        ServletRequestDetails requestDetails = exchange.getIn().getBody(ServletRequestDetails.class);
        
        if (requestDetails != null) {
            // Get all values using ternary operators for null checks
            RestOperationTypeEnum operationType = requestDetails.getRestOperationType();
            String resourceName = requestDetails.getResourceName();
            String requestType = requestDetails.getRequestType() != null ? requestDetails.getRequestType().name() : null;
            String resourceId = requestDetails.getId() != null ? requestDetails.getId().getIdPart() : null;
            
            // Store in headers for route decision making
            exchange.getIn().setHeader("FHIR_OPERATION_TYPE", operationType != null ? operationType.name() : null);
            exchange.getIn().setHeader("FHIR_RESOURCE_TYPE", resourceName);
            exchange.getIn().setHeader("HTTP_METHOD", requestType);
            exchange.getIn().setHeader("RESOURCE_ID", resourceId);
            
            // Store the entire RequestDetails object for later use if needed
            exchange.setProperty("RequestDetails", requestDetails);
            
            // Debug logging
            log.debug("Operation Type: {}", operationType);
            log.debug("Resource Name: {}", resourceName);
            log.debug("Request Type: {}", requestType);
            log.debug("Resource ID: {}", resourceId);
        } else {
            log.warn("RequestDetails is null in the exchange body");
        }
        
        // Log all headers for debugging
        exchange.getIn().getHeaders().forEach((key, value) -> 
            log.debug("Header: {} = {}", key, value));
    }
} 