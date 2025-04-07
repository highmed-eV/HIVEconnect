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

package org.ehrbase.fhirbridge.openehr;

import org.ehrbase.openehr.sdk.webtemplate.templateprovider.TemplateProvider;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.beans.BeansException;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Renaud Subiger
 * @since 1.6
 */
@Component
public class DefaultTemplateProvider implements TemplateProvider, ApplicationContextAware {

    private final CacheManager cacheManager;
    private ApplicationContext applicationContext;

    public DefaultTemplateProvider(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> find(String templateId) {
        var cache = cacheManager.getCache("templateCache");
        if (cache != null) {
            var template = cache.get(templateId, OPERATIONALTEMPLATE.class);
            return Optional.of(template);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Set<String> getTemplateIds() {
        var templateIds = new HashSet<String>();
        var cache = cacheManager.getCache("templateCache");
        if (cache instanceof ConcurrentMapCache) {
            ConcurrentMap<Object, Object> nativeCache = ((ConcurrentMapCache) cache).getNativeCache();
            templateIds.addAll(nativeCache.keySet().stream()
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.toSet()));
        }
        return templateIds;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
