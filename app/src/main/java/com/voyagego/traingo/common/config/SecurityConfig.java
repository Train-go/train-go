package com.voyagego.traingo.common.config;

import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		var api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
		var requestCache = new HttpSessionRequestCache();
		// API polling must not replace the page the customer was booking.
		requestCache.setRequestMatcher(request -> "GET".equals(request.getMethod()) && !api.matches(request));
		requestCache.setMatchingRequestParameterName(null);
		http
			.authorizeHttpRequests(requests -> requests
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers("/", "/trips/**", "/api/trips/**", "/login", "/register", "/error").permitAll()
				.requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/design/**",
					"/vendor/**", "/favicon.ico").permitAll()
				.requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
				.requestMatchers("/booking/**", "/my-trips/**", "/tickets/**").hasRole("CUSTOMER")
				.anyRequest().authenticated())
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
			// Keep the generated page until the shared client shell is available.
			.formLogin(form -> form.usernameParameter("email")
				.failureUrl("/login?error")
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
