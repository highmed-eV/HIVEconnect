package org.highmed.hiveconnect.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TenantContext.class)
class TenantContextTest {

    @BeforeEach
    void setUp() {
        TenantContext.removeContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.removeContext();
    }

    @Test
    void testSetAndGetTenantId() {
        // Arrange
        String tenantId = "test-tenant";

        // Act
        TenantContext.setTenantId(tenantId);

        // Assert
        assertEquals(tenantId, TenantContext.getTenantId());
    }

    @Test
    void testRemoveContext() {
        // Arrange
        String tenantId = "test-tenant";
        TenantContext.setTenantId(tenantId);

        // Act
        TenantContext.removeContext();

        // Assert
        assertEquals("0", TenantContext.getTenantId()); // Default value after removal
    }

    @Test
    void testSetNullTenantId() {
        // Act
        TenantContext.setTenantId("0");

        // Assert
        assertEquals("0", TenantContext.getTenantId()); // Default value for null
    }

    @Test
    void testGetTenantIdWhenNotSet() {
        // Assert
        assertEquals("0", TenantContext.getTenantId()); // Default value
    }

    @Test
    void testGetOrganizationId() {
        // Assert
        assertEquals("0", TenantContext.getOrganizationId()); // Default value
    }

    @Test
    void testGetUserName() {
        // Assert
        assertEquals("anonymous", TenantContext.getUserName()); // Default value
    }

    @Test
    void testGetAdapterHeaderMap() {
        // Act
        var headers = TenantContext.getAdapterHeaderMap();

        // Assert
        assertEquals("0", headers.get("X-TenantId"));
        assertEquals("0", headers.get("X-OrganizationId"));
        assertEquals("anonymous", headers.get("X-Auth-Request-Preferred-Username"));
    }
} 