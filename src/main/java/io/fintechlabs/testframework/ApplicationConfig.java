package io.fintechlabs.testframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.fintechlabs.testframework.ui.ServerInfoTemplate;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import com.google.common.collect.Lists;

import io.fintechlabs.testframework.logging.GsonArrayToBsonArrayConverter;
import io.fintechlabs.testframework.logging.GsonObjectToBsonDocumentConverter;
import io.fintechlabs.testframework.logging.GsonPrimitiveToBsonValueConverter;
import io.fintechlabs.testframework.runner.InMemoryTestRunnerSupport;
import io.fintechlabs.testframework.runner.TestRunnerSupport;
import io.fintechlabs.testframework.security.KeyManager;
import io.fintechlabs.testframework.variant.VariantConverters;

@Configuration
public class ApplicationConfig {
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
		List<Converter<?, ?>> converters = Lists.newArrayList(
			new GsonPrimitiveToBsonValueConverter(),
			new GsonObjectToBsonDocumentConverter(),
			new GsonArrayToBsonArrayConverter());
		converters.addAll(VariantConverters.getConverters());
		return new MongoCustomConversions(converters);
	}

	@Bean
	public KeyManager keyManager() {
		return new KeyManager();
	}

	@Bean
	public ServerInfoTemplate serverInfoTemplate() {
		return new ServerInfoTemplate();
	}
}
