package org.highmed.hiveconnect.config;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

public class TenantContext {
    record TenantContextRecord(String tenantId, String organizationId, String userName) {
        public Map<String, String> toAdapterHeaderMap() {
            return Map.of("X-TenantId", tenantId, "X-OrganizationId", organizationId, "X-Auth-Request-Preferred-Username", userName);
        }
    }

    private static final ThreadLocal<TenantContextRecord> CONTEXT =
            ThreadLocal.withInitial(() -> new TenantContextRecord("0", "0", "anonymous"));

    public static void setContext(RequestDetails request) {
        setContext(request.getHeader("X-TenantId"), request.getHeader("X-OrganizationId"),
                request.getHeader("X-Auth-Request-Preferred-Username"), request.getHeader("X-UserInfo"));
    }

    static void setContext(String tenantId, String organizationId, String userName, String userInfo) {
        CONTEXT.set(new TenantContextRecord(
                getValue(tenantId, "0"),
                getValue(organizationId, "0"),
                getValue(getUserNameFromUserInfo(userInfo), getValue(userName, "anonymous"))
        ));
    }

    public static void removeContext() {
        CONTEXT.remove();
    }

    private static String getValue(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static String getTenantId() {
        return CONTEXT.get().tenantId();
    }

    public static String getOrganizationId() {
        return CONTEXT.get().organizationId();
    }

    public static String getUserName() {
        return CONTEXT.get().userName();
    }

    public static Map<String, String> getAdapterHeaderMap() {
        return CONTEXT.get().toAdapterHeaderMap();
    }

    public static void setTenantId(String tenant) {
        CONTEXT.set(new TenantContextRecord(tenant, CONTEXT.get().organizationId, CONTEXT.get().userName));
    }

    private static String getUserNameFromUserInfo(String userInfo) {
        try {
            return userInfo != null ? (String) new ObjectMapper().readValue(Base64.getUrlDecoder().decode(userInfo), Map.class).get("preferred_username") : null;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
