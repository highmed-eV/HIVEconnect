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

package org.ehrbase.fhirbridge.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {@link Configuration} for Spring Security using Basic Authentication.
 *
 * @author Renaud Subiger
 * @since 1.6
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity //MedBlocks
@ConditionalOnProperty(value = "fhir-bridge.security.type", havingValue = "basic")
public class BasicSecurityConfiguration  {

    private final Logger log = LoggerFactory.getLogger(BasicSecurityConfiguration.class);

    private final SecurityProperties properties;

    public BasicSecurityConfiguration(SecurityProperties properties) {
        this.properties = properties;
    }

    
    @Bean
     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        //  http.authorizeRequests().requestMatchers("/**").hasRole("USER").and().formLogin();
        log.info("#########securityFilterChain");
         http
            .authorizeRequests()
                .requestMatchers("/**")
                    .permitAll()
                .anyRequest()
                    .authenticated()
            .and().httpBasic()
            .and().sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.csrf().disable();
        return http.build();
     }

     @Bean
     public UserDetailsService userDetailsService() {
         UserDetails user = User.withDefaultPasswordEncoder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();
         UserDetails admin = User.withDefaultPasswordEncoder()
             .username("admin")
             .password("password")
             .roles("ADMIN", "USER")
             .build();
         return new InMemoryUserDetailsManager(user, admin);
     }

    //New spring security
    // @Bean
    // public WebSecurityCustomizer webSecurityCustomizer() {
    //     return (web) -> web.ignoring().antMatchers("/ignore1", "/ignore2");
    // }

    /**
     * @see WebSecurityConfigurerAdapter#configure(HttpSecurity)
     */
    // @Override //MedBlocks
    public void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .csrf()
                .disable()
            .authorizeRequests()
                .anyRequest().authenticated()
                .and()
            .httpBasic();
        // @formatter:on
    }

    /**
     * @see WebSecurityConfigurerAdapter#configure(AuthenticationManagerBuilder)
     */
    // @Override //MedBlocks
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // @formatter:off
        auth
            .inMemoryAuthentication()
                .withUser(properties.getUser().getName())
                    .password("{noop}" + properties.getUser().getPassword())
                    .roles("ROLE_USER");
        // @formatter:on
    }
}
