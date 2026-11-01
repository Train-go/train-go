package com.voyagego.traingo.common.config;

import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Open by default, closed by prefix (docs/decisions/0013-quyen-truy-cap-mac-dinh-mo.md).
 *
 * Every private page or API lives under one of the prefixes below. Anything
 * else is public, so a visitor who mistypes an address sees the 404 page, not
 * the login page. The price: a private screen added outside these prefixes is
 * open to anyone. A new private screen goes under an existing prefix, or its
 * prefix is added here in the same change.
 */
@Configuration
public class SecurityConfig {
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * A bean so the register page can discard the saved destination after it
	 * signs the new customer in, the same cache the login success handler reads.
	 */
	@Bean
	RequestCache requestCache() {
		var api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
		var requestCache = new HttpSessionRequestCache();
		// API polling must not replace the page the customer was booking.
		requestCache.setRequestMatcher(request -> "GET".equals(request.getMethod()) && !api.matches(request));
		requestCache.setMatchingRequestParameterName(null);
		return requestCache;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, RequestCache requestCache) throws Exception {
		var api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
		http
			.authorizeHttpRequests(requests -> requests
				// The 403 page of a customer who opened /admin/** must render.
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
				.requestMatchers("/booking/**", "/my-trips/**", "/tickets/**").hasRole("CUSTOMER")
				.requestMatchers("/profile/**").authenticated()
				// Also opens /error, /login, /register, the trip pages, /api/trips/**
				// (the seat map is readable before login) and the static files.
				.anyRequest().permitAll())
			.requestCache(cache -> cache.requestCache(requestCache))
			.exceptionHandling(errors -> errors
				.defaultAuthenticationEntryPointFor((request, response, exception) ->
					apiError(response, 401, "Vui lòng đăng nhập."), api)
				.accessDeniedHandler((request, response, exception) -> {
					if (api.matches(request)) {
						apiError(response, 403, "Bạn không có quyền truy cập.");
					} else {
						response.sendError(403);
					}
				}))
			.formLogin(form -> form.loginPage("/login")
				.usernameParameter("email")
				.failureHandler(new EmailRetainingLoginFailureHandler())
				.successHandler(new RoleBasedLoginSuccessHandler(requestCache)))
			.logout(logout -> logout.logoutSuccessUrl("/"));
		return http.build();
	}

	private static void apiError(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("{\"message\":\"" + message + "\",\"fields\":{}}");
	}
}
