package com.voyagego.traingo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security chain. Public pages and static assets are open; everything
 * else requires an authenticated session. The user store, roles and the custom
 * login page arrive with the auth feature.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(requests -> requests
				.requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll()
				// Bootstrap is served from the application itself. Leaving /vendor/**
				// out of this list makes Spring Security answer stylesheet requests
				// with the login page, and the browser drops it without a warning,
				// so the page renders completely unstyled with nothing in the log.
				.requestMatchers("/vendor/**", "/favicon.ico").permitAll()
				// Redundant today, since anyRequest() already demands a session. It
				// is here so the admin area has one obvious line to tighten to
				// hasRole("ADMIN") when the auth feature lands, and so a test can
				// pin the rule down before then.
				.requestMatchers("/admin/**").authenticated()
				.anyRequest().authenticated())
			.formLogin(Customizer.withDefaults())
			.logout(Customizer.withDefaults());
		return http.build();
	}

}
