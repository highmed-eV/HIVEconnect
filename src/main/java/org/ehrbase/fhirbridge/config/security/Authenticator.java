package org.ehrbase.fhirbridge.config.security;

@FunctionalInterface
public interface Authenticator {
    boolean authenticate(String principal, String credentials);
}
