package net.openid.conformance.ui;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Server-side wiring active only under the {@code legacy-ui} Spring profile — the temporary
 * "frozen old UI" escape hatch. The pre-redesign assets are vendored from tag
 * {@code release-v5.1.45} (under {@code classpath:/static-legacy/} and
 * {@code classpath:/templates-legacy/}) and selected via {@code application-legacy-ui.properties},
 * which repoints {@code spring.web.resources.static-locations} at the snapshot.
 *
 * <p>Static pages/assets follow that property automatically (see
 * {@link net.openid.conformance.ApplicationConfig#addResourceHandlers}). The three
 * <em>server-rendered</em> Thymeleaf pages the redesign restyled — {@code error.html},
 * {@code implicitCallback.html}, {@code resultCaptured.html} — are rendered from
 * {@code classpath:/templates/} and are NOT reached by the static-location swap, so the
 * resolver below serves the pre-redesign versions of just those three from
 * {@code classpath:/templates-legacy/}. Every other template is byte-identical to the redesign
 * and is left to Spring Boot's default resolver: {@code checkExistence=true} makes this resolver
 * fall through when a template is absent here, so backend-coupled pages (e.g. the
 * {@code self-contained-export/*} templates) keep tracking the live code rather than being frozen.</p>
 *
 * <p>To remove the escape hatch cleanly, delete this class, {@code src/main/resources/static-legacy/},
 * {@code src/main/resources/templates-legacy/}, {@code application-legacy-ui.properties}, and the
 * {@code @Profile("!legacy-ui")} on {@link HomeController}.</p>
 */
@Configuration
@Profile("legacy-ui")
public class LegacyUiConfig {

	/**
	 * Higher-priority Thymeleaf resolver for the three redesign-restyled pages. Spring Boot's
	 * auto-configured web {@code SpringTemplateEngine} collects all {@link org.thymeleaf.templateresolver.ITemplateResolver}
	 * beans and orders them; this one sits ahead of the default ({@code order = 0}) and only
	 * resolves templates that actually exist under {@code templates-legacy/}, falling through to
	 * the default {@code classpath:/templates/} resolver otherwise.
	 */
	@Bean
	public SpringResourceTemplateResolver legacyTemplateResolver() {
		SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
		resolver.setPrefix("classpath:/templates-legacy/");
		resolver.setSuffix(".html");
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCharacterEncoding("UTF-8");
		resolver.setCheckExistence(true);
		resolver.setOrder(0);
		resolver.setCacheable(true);
		return resolver;
	}
}
