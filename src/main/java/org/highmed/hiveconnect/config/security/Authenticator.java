package org.highmed.hiveconnect.config.security;

@FunctionalInterface
public interface Authenticator {
    boolean authenticate(String principal, String credentials);
}
