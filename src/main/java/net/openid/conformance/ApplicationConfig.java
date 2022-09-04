package net.openid.conformance;

import com.google.common.collect.Lists;
import net.openid.conformance.logging.GsonArrayToBsonArrayConverter;
import net.openid.conformance.logging.GsonObjectToBsonDocumentConverter;
import net.openid.conformance.logging.GsonPrimitiveToBsonValueConverter;
import net.openid.conformance.runner.InMemoryTestRunnerSupport;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.security.KeyManager;
import net.openid.conformance.ui.ServerInfoTemplate;
import net.openid.conformance.variant.VariantConverters;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
