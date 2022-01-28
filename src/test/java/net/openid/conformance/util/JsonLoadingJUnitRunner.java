package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class JsonLoadingJUnitRunner extends BlockJUnit4ClassRunner {

	public static final String WRAPPED = "__WRAPPED_JsonArray__";

	public JsonLoadingJUnitRunner(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

	@Override
	protected Statement methodInvoker(FrameworkMethod method, Object test) {

		UseResurce useResurce = Optional.ofNullable(method.getAnnotation(UseResurce.class))
			.orElseGet(() -> {
				return super.getTestClass().getJavaClass().getAnnotation(UseResurce.class);
			});
		if(useResurce == null) {
			return super.methodInvoker(method, test);
		}
		String resource = useResurce.value();
		String rawJson;
		String resourceOverride = System.getProperty("resource.override");
		if(resourceOverride != null) {
			try {
				rawJson = Files.readString(Path.of(resourceOverride), Charset.defaultCharset());
			} catch (IOException exception) {
				return new FailingStatement("Unable to override JSON document with %s in test %s", resourceOverride, method.getName());
			}
		} else {
			try {
				rawJson = IOUtils.resourceToString(resource, Charset.defaultCharset(), getClass().getClassLoader());
			} catch (IOException exception) {
				return new FailingStatement("Unable to load JSON document %s in test %s", resource, method.getName());
			}

		}

		JsonObject jsonObject = null;
		if (new JsonParser().parse(rawJson).isJsonArray()) {
			jsonObject = convertArrayToJsonObject(rawJson);
		} else {
			jsonObject = new JsonParser().parse(rawJson).getAsJsonObject();
		}
		if(jsonObject == null) {
			return new FailingStatement("Unable to load JSON document %s in test %s", resource, method.getName());
		}

		Field[] fields = FieldUtils.getAllFields(getTestClass().getJavaClass());
		Optional<Field> perhaps = Arrays.stream(fields)
			.filter(f -> f.getType().isAssignableFrom(JsonObject.class))
			.findFirst();
		if(perhaps.isPresent()) {
			Field field = perhaps.get();
			field.setAccessible(true);
			try {
				field.set(test, jsonObject);
			} catch (IllegalAccessException e) {
				return new FailingStatement("Unable to set JsonObject field %s in test %s", field.getName(), method.getName());
			}
		} else {
			return new FailingStatement("No suitable JsonObject field present on %s", method.getName());
		}
		Statement statement =  super.methodInvoker(method, test);
		return withBefores(method, test, statement);
	}

	private JsonObject convertArrayToJsonObject(String rawJson) {
		JsonObject result = new JsonObject();
		result.add(WRAPPED, new JsonParser().parse(rawJson));
		return result;
	}

	private static class FailingStatement extends Statement {

		private final String message;

		FailingStatement(String message, Object...args) {
			this.message = String.format(message, args);
		}

		@Override
		public void evaluate() throws Throwable {
			throw new AssertionError(message);
		}
	}

}
