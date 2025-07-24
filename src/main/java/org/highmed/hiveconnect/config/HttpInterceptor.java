package org.highmed.hiveconnect.config;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Interceptor
public class HttpInterceptor {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());


    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void preHandle(RequestDetails request) {
        TenantContext.setContext(request);
        configureLogsAndTracing();

        log.info(" {} method called for user {} ", request.getRequestPath(), TenantContext.getUserName());
    }

    @Hook(Pointcut.SERVER_PROCESSING_COMPLETED)
    public void afterCompletion(RequestDetails request) {
        TenantContext.removeContext();
        MDC.remove("tenantId");
    }

    private static void configureLogsAndTracing() {
        MDC.put("tenantId", TenantContext.getTenantId());
        Span.fromContext(Context.current()).setAttribute("tenant.id", TenantContext.getTenantId());
    }

}
