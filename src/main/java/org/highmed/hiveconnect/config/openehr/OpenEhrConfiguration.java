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

package org.highmed.hiveconnect.config.openehr;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.highmed.hiveconnect.openehr.DefaultTemplateProvider;
import org.highmed.hiveconnect.openfhir.openfhirclient.OpenFHIRAdapter;
import org.highmed.hiveconnect.security.oauth2.AccessTokenService;
import org.highmed.hiveconnect.security.oauth2.TokenAuthenticationInterceptor;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClient;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.HIVEconnectDefaultRestClient;
import org.ehrbase.openehr.sdk.webtemplate.templateprovider.TemplateProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.net.URI;

import static org.highmed.hiveconnect.config.openehr.OpenEhrProperties.SecurityType.BASIC;
import static org.highmed.hiveconnect.config.openehr.OpenEhrProperties.SecurityType.OAUTH2;

/**
 * {@link Configuration} for openEHR.
 *
 * @author Renaud Subiger
 * @since 1.6
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenEhrProperties.class)
@Slf4j
public class OpenEhrConfiguration {

    @PostConstruct
    public void initialize() {
        log.info("Running HIVE Connect using openEHR");
    }

    @Bean
    public OpenEhrClient openEhrClient(OpenEhrClientConfig configuration, TemplateProvider templateProvider,
                                       @Qualifier("openEhrHttpClient") HttpClient httpClient) {
        return new HIVEconnectDefaultRestClient(configuration, templateProvider, httpClient);
    }

    @Bean
    public OpenEhrClientConfig configuration(OpenEhrProperties openEhrProperties) {
        log.info("Creating OpenEhrClientConfig with configured url : {}", openEhrProperties.getUrl());
        return new OpenEhrClientConfig(URI.create(openEhrProperties.getUrl()));
    }

    @Bean
    public DefaultTemplateProvider templateProvider(CacheManager cacheManager) {
        return new DefaultTemplateProvider(cacheManager);
    }

    @Bean
    public OperationalTemplateUploader operationalTemplateInitializer(OpenEhrClient openEhrClient,
                                                                      OpenFHIRAdapter openFHIRAdapter,
                                                                      DefaultTemplateProvider templateProvider) {
        return new OperationalTemplateUploader(openEhrClient, openFHIRAdapter, templateProvider);
    }

    @Bean(name = "openEhrHttpClient")
    public HttpClient httpClient(ObjectProvider<AccessTokenService> accessTokenService, OpenEhrProperties properties) {
        var builder = HttpClientBuilder.create();

        var security = properties.getSecurity();
        if (security.getType() == BASIC) {
            var credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(security.getUser().getName(), security.getUser().getPassword()));
            builder.setDefaultCredentialsProvider(credentialsProvider);
        } else if (security.getType() == OAUTH2) {
            builder.addInterceptorFirst(new TokenAuthenticationInterceptor(accessTokenService.getIfAvailable()));
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "hive-connect.openehr.security.type", havingValue = "oauth2")
    public AccessTokenService accessTokenService(OpenEhrProperties properties) {
        var oauth2 = properties.getSecurity().getOauth2();
        return new AccessTokenService(oauth2.getTokenUrl(), oauth2.getClientId(), oauth2.getClientSecret());
    }
}
