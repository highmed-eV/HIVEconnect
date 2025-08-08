package org.highmed.hiveconnect.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {BasicSecurityConfiguration.class})
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = {
    "hive-connect.security.type=basic",
    "hive-connect.security.user.name=test-user",
    "hive-connect.security.user.password=test-password"
})
class BasicSecurityConfigurationTest {

    @Autowired
    private BasicSecurityConfiguration configuration;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void testBasicSecurityConfigurationBeans() {
        assertNotNull(configuration);
        assertNotNull(passwordEncoder);
        assertNotNull(userDetailsService);
    }

    @Test
    void testPasswordEncoder() {
        String rawPassword = "test-password";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void testUserDetailsService() {
        var userDetails = userDetailsService.loadUserByUsername("test-user");
        assertNotNull(userDetails);
        assertEquals("test-user", userDetails.getUsername());
        assertTrue(passwordEncoder.matches("test-password", userDetails.getPassword()));
    }
} 