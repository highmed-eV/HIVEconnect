/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.fhirbridge.config.openehr;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} to configure openEHR.
 *
 * @author Renaud Subiger
 * @since 1.6
 */
@ConfigurationProperties(prefix = "fhir-bridge.openehr")
@Setter
@Getter
public class OpenEhrProperties {

    private String url;

    private final Security security = new Security();

    private boolean updateTemplatesOnStartup = false;
    @Setter
    @Getter
    public static class Security {

        private SecurityType type = SecurityType.NONE;

        private final User user = new User();

        private final OAuth2 oauth2 = new OAuth2();

    }

    public enum SecurityType {

        NONE, BASIC, OAUTH2
    }
    @Setter
    @Getter
    public static class User {

        private String name;

        private String password;

    }
    @Setter
    @Getter
    public static class OAuth2 {

        private String tokenUrl;

        private String clientId;

        private String clientSecret;
    }
}
