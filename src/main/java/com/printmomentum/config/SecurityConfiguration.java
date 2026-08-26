package com.printmomentum.config;

import static com.printmomentum.domain.UserRoleEnum.admin;
import static com.printmomentum.domain.UserRoleEnum.user;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

	static final String[] WHITE_LIST_URL = {
			"/error",
			"/api/v1/health",
			"/api/v2/auth/login",
			"/api/v2/auth/refresh",
			"/api/v2/auth/logout"
	};

	private final JwtAuthenticationFilter jwtAuthFilter;
	private final AuthenticationProvider authenticationProvider;
	private final String allowedOrigins;

	public SecurityConfiguration(
			JwtAuthenticationFilter jwtAuthFilter,
			AuthenticationProvider authenticationProvider,
			@Value("${printmomentum.cors.allowed-origins}") String allowedOrigins) {
		this.jwtAuthFilter = jwtAuthFilter;
		this.authenticationProvider = authenticationProvider;
		this.allowedOrigins = allowedOrigins;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(request -> {
					var corsConfig = new CorsConfiguration();
					corsConfig.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
					corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
					corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Skip-Auth-Refresh"));
					corsConfig.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
					corsConfig.setAllowCredentials(true);
					return corsConfig;
				}))
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(req -> req.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(WHITE_LIST_URL)
						.permitAll()
						.requestMatchers("/api/v1/listings/**")
						.hasAnyRole(admin.name(), user.name())
						.requestMatchers("/api/v1/favorites/**")
						.hasAnyRole(admin.name(), user.name())
						.requestMatchers("/api/v1/shops/**")
						.hasAnyRole(admin.name(), user.name())
						.requestMatchers("/api/v1/query-stats")
						.hasAnyRole(admin.name(), user.name())
						.requestMatchers("/api/v1/user/register", "/api/v1/user/members", "/api/v1/user/members/**")
						.hasRole(admin.name())
						.requestMatchers("/api/v1/user/**")
						.hasAnyRole(admin.name(), user.name())
						.anyRequest()
						.authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
				.authenticationProvider(authenticationProvider)
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint((request, response, authException) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							JSONObject obj = new JSONObject();
							obj.put("detail", "Couldn't verify your Identity. Please log out and log in again!");
							response.getWriter().write(obj.toString());
						})
						.accessDeniedHandler((request, response, accessDeniedException) -> {
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							JSONObject obj = new JSONObject();
							obj.put("detail", "You do not have permission to access this resource.");
							response.getWriter().write(obj.toString());
						}))
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
