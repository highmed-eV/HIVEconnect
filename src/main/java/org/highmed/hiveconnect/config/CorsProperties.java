/*
 * Copyright 2020-2021 the original author or authors.
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

package org.highmed.hiveconnect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * {@link ConfigurationProperties ConfigurationProperties} to configure CORS.
 *
 * @since 1.0.0
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "hive-connect.cors")
public class CorsProperties {

    private boolean allowCredentials = false;

    private List<String> allowedHeaders;

    private List<String> allowedMethods;

    private List<String> allowedOrigins;

}
