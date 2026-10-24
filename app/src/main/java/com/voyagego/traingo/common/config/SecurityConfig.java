package com.voyagego.traingo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Open by default, closed by prefix (docs/decisions/0013-quyen-truy-cap-mac-dinh-mo.md).
 *
 * Every private page or API lives under one of the prefixes below. Anything
 * else is public, so a visitor who mistypes an address sees the 404 page, not
 * the login page. The price: a private screen added outside these prefixes is
 * open to anyone. A new private screen goes under an existing prefix, or its
 * prefix is added here in the same change.
 *
 * Roles are not checked yet; the auth feature (#22) turns the two groups into
 * hasRole("ADMIN") and hasRole("CUSTOMER").
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(requests -> requests
				.requestMatchers("/admin/**", "/api/admin/**").authenticated()
				.requestMatchers("/booking/**", "/my-trips/**", "/tickets/**", "/api/trips/**").authenticated()
				// Also opens /error: without it a visitor whose request fails is
				// sent to the login page instead of the error page. Static files
				// (/css, /js, /fonts, /vendor) are open through this line too.
				.anyRequest().permitAll())
			.formLogin(Customizer.withDefaults())
			.logout(Customizer.withDefaults());
		return http.build();
	}

}
