package net.openid.conformance.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

public class JsonUtils {

	public static void configureJsonPathForGson() {
		Configuration.setDefaults(new Configuration.Defaults() {
			private final JsonProvider jsonProvider = new GsonJsonProvider(new GsonBuilder().serializeNulls().create());
			private final MappingProvider mappingProvider = new GsonMappingProvider();

			@Override
			public JsonProvider jsonProvider() {
				return jsonProvider;
			}

			@Override
			public MappingProvider mappingProvider() {
				return mappingProvider;
			}

			@Override
			public Set<Option> options() {
				return EnumSet.noneOf(Option.class);
			}
		});
	}

	public static Gson createBigDecimalAwareGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Double.class, (JsonSerializer<Double>)
			(src, typeOfSrc, context) -> new JsonPrimitive(new BigDecimal(src)));
		return builder
			.serializeNulls()
			.create();
	}

}
