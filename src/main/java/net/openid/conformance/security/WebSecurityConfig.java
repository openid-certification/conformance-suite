package net.openid.conformance.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
@EnableWebSecurity
class WebSecurityConfig {

	/**
	 * Exempt static asset paths from Spring Security's filter chain entirely.
	 *
	 * Without this, every request for /images/*, /css/*, /js/*, /vendor/*,
	 * /components/*, /fonts/*, /templates/*, and /favicon.ico runs through
	 * Spring Security's default HeadersConfigurer, which stamps
	 * "Cache-Control: no-cache, no-store, max-age=0, must-revalidate",
	 * "Pragma: no-cache", and "Expires: 0" on the response. Browsers then
	 * revalidate every static asset on every navigation, wasting bandwidth
	 * and defeating the browser cache entirely.
	 *
	 * WebSecurityCustomizer.ignoring() removes these paths from the security
	 * filter chain before any filter runs: no authentication check, no
	 * header writers, no session handling. Spring Boot's static resource
	 * handler still serves the files (with Last-Modified / ETag support for
	 * conditional 304 responses) and the anonymous-access permitAll() rules
	 * in WebSecurityOidcLoginConfig become redundant for these paths but
	 * stay in place as a defence-in-depth fallback.
	 *
	 * Paths that stay inside the security chain:
	 *  - /api/** — needs authentication + CSRF on mutating routes.
	 *  - /login.html, /logout.html — pages users see; should revalidate.
	 *  - /jwks**, /test/**, /.well-known/** — dynamic endpoints.
	 *  - /json-schemas/** — served via a custom ResourceHandler; left in
	 *    the chain for now because it's less obviously static-asset shaped.
	 */
	@Bean
	public WebSecurityCustomizer staticResourcesIgnoreCustomizer() {
		return web -> web.ignoring().requestMatchers(
			"/css/**",
			"/js/**",
			"/vendor/**",
			"/components/**",
			"/images/**",
			"/fonts/**",
			"/templates/**",
			"/favicon.ico"
		);
	}
}
