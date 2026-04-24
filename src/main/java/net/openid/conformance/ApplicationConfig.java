package net.openid.conformance;

import net.openid.conformance.runner.InMemoryTestRunnerSupport;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.security.KeyManager;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Collection;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/json-schemas/**")
			.addResourceLocations("classpath:json-schemas/");
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
