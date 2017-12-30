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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import io.fintechlabs.testframework.logging.GsonObjectToBsonDocumentConverter;
import io.fintechlabs.testframework.logging.GsonPrimitiveToBsonValueConverter;
import io.fintechlabs.testframework.runner.InMemoryTestRunnerSupport;
import io.fintechlabs.testframework.runner.TestRunnerSupport;

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
        GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
        gsonHttpMessageConverter.setGson(getDbObjectCollapsingGson());
        messageConverters.add(gsonHttpMessageConverter);

        return new HttpMessageConverters(true, messageConverters);
    }
    
    /**
     * Special GSON converter that looks for and collapses __wrapped_key_element fields
	 * @return
	 */
	private Gson getDbObjectCollapsingGson() {
		return new GsonBuilder()
				.registerTypeHierarchyAdapter(DBObject.class, new JsonSerializer<DBObject>() {

					private Gson internalGson = new Gson();
					
					@Override
					public JsonElement serialize(DBObject src, Type typeOfSrc, JsonSerializationContext context) {
						// run the field conversion
						DBObject converted = (DBObject) convertStructureToField(src);
						// delegate to regular GSON for the real work
						return internalGson.toJsonTree(converted);
					}
					
					private Object convertStructureToField(Object source) {
						if (source instanceof List) {
							// if it's a list of some type, loop through it
							@SuppressWarnings("unchecked")
							List<Object> list = (List<Object>)source;
							List<Object> converted = list.stream()
									.map(this::convertStructureToField)
									.collect(Collectors.toList());
							return converted;
						} else if (source instanceof DBObject) {
							// if it's an object, need to look through all the fields and convert any weird ones
							DBObject dbo = (DBObject) source;
							DBObject converted = new BasicDBObject();
							for (String key : dbo.keySet()) {
								if (key.startsWith("__wrapped_key_element_")) {
									DBObject wrapped = (DBObject) dbo.get(key);
									converted.put((String) wrapped.get("key"), convertStructureToField(wrapped.get("value")));
								} else {
									converted.put(key, convertStructureToField(dbo.get(key)));
								}
							}
							return converted;
						} else {
							return source;
						}
					}
				})
				.create();
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
				new GsonObjectToBsonDocumentConverter()));
	}
}
