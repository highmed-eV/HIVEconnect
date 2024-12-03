// /*
//  * Copyright 2020-2021 the original author or authors.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      https://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package org.ehrbase.fhirbridge.engine.converter.generic;

// import org.apache.commons.lang3.StringUtils;
// import org.ehrbase.client.classgenerator.interfaces.RMEntity;
// import org.ehrbase.client.classgenerator.shareddefinition.Language;
// import org.ehrbase.fhirbridge.engine.converter.ResourceConverter;
// import org.hl7.fhir.r4.model.Resource;
// import org.springframework.lang.NonNull;

// /**
//  * @param <S> source type
//  * @param <T> target type
//  * @since 1.0.0
//  */
// @FunctionalInterface
// public interface RMEntityConverter<S extends Resource, T extends RMEntity> extends ResourceConverter<S, T> {

//     Language DEFAULT_LANGUAGE = Language.DE;

//     T convert(@NonNull S resource);

//     default Language resolveLanguageOrDefault(@NonNull S resource) {
//         for (Language language : Language.values()) {
//             if (StringUtils.equalsIgnoreCase(language.getCode(), resource.getLanguage())) {
//                 return language;
//             }
//         }
//         return DEFAULT_LANGUAGE;
//     }
// }
