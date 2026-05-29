package net.openid.conformance;

import net.openid.conformance.runner.InMemoryTestRunnerSupport;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.security.KeyManager;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		// The plans listing is the home page. `/` and the legacy `/index.html`
		// resolve to it via a server-side 302 (temporary) redirect, so existing
		// bookmarks and the OIDC/OTT login flow keep working after the
		// cts-dashboard launchpad was retired. The `/` redirect only takes effect
		// once static/index.html is deleted, because Spring Boot's
		// WelcomePageHandlerMapping outranks these view controllers while a
		// welcome page exists.
		registry.addRedirectViewController("/", "/plans.html").setStatusCode(HttpStatus.FOUND);
		registry.addRedirectViewController("/index.html", "/plans.html").setStatusCode(HttpStatus.FOUND);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/json-schemas/**")
			.addResourceLocations("classpath:json-schemas/");

		// Self-hosted font assets are content-addressed by filename (e.g.
		// Inter-Variable.woff2). The woff2 itself is already Brotli-compressed,
		// so no further server-side compression is configured. Long-lived
		// immutable caching means a font upgrade requires changing the filename.
		registry.addResourceHandler("/fonts/**")
			.addResourceLocations("classpath:/static/fonts/")
			.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
	}

	@Bean
	public HttpMessageConverters customConverters() {

		Collection<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

		// wire in the special GSON converter to the HTTP message outputs, will automatically handle all __wrapped_key_element structures added by GsonObjectToBsonDocumentConverter
		GsonHttpMessageConverter gsonHttpMessageConverter = new CollapsingGsonHttpMessageConverter();
		messageConverters.add(gsonHttpMessageConverter);

		return new HttpMessageConverters(true, messageConverters);
	}

	@Bean
	public TestRunnerSupport testRunnerSupport() {
		return new InMemoryTestRunnerSupport();
	}

	@Bean
	public MongoCustomConversions mongoCustomConversions() {
		return MongoConversionSupport.createMongoCustomConversions();
	}

	@Bean
	public KeyManager keyManager() {
		return new KeyManager();
	}
}
