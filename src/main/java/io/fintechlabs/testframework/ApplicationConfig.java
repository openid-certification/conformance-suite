/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import com.google.common.collect.Lists;

import io.fintechlabs.testframework.logging.GsonArrayToBsonArrayConverter;
import io.fintechlabs.testframework.logging.GsonObjectToBsonDocumentConverter;
import io.fintechlabs.testframework.logging.GsonPrimitiveToBsonValueConverter;
import io.fintechlabs.testframework.runner.InMemoryTestRunnerSupport;
import io.fintechlabs.testframework.runner.TestRunnerSupport;
import io.fintechlabs.testframework.security.KeyManager;

/**
 * @author jricher
 *
 */
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
	@SuppressWarnings("unchecked")
	public CustomConversions mongoCustomConversions() {
		return new CustomConversions(Lists.newArrayList(
			new GsonPrimitiveToBsonValueConverter(),
			new GsonObjectToBsonDocumentConverter(),
			new GsonArrayToBsonArrayConverter()));
	}

	@Bean
	public KeyManager keyManager() {
		return new KeyManager();
	}

}
