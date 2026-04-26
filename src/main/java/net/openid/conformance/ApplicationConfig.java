package net.openid.conformance;

import net.openid.conformance.runner.InMemoryTestRunnerSupport;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.security.KeyManager;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

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
