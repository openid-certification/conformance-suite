package net.openid.conformance.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

public class JsonAssertingConditionTests {

	@Test
	public void canAssertThatADocumentHasAField() {

		JsonObject object = new JsonObject();
		object.addProperty("data", "this is some data");

		Environment environment = new Environment();
		environment.putObject("response", object);

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {

			@Override
			public Environment evaluate(Environment env) {
				JsonObject object = env.getObject("response");

				assertHasField(object, "$.data");

				return env;
			}

		};
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.evaluate(environment);

	}

	@Test
	public void foundFieldIsLogged() {

		JsonObject object = OIDFJSON.toObject(new Gson().toJsonTree(Map.of(
			"data", Map.of(
				"nest", Map.of("field", "value")
			)
		)));

		TestInstanceEventLog log = mock(TestInstanceEventLog.class);

		Environment environment = new Environment();
		environment.putObject("response", object);

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {

			@Override
			public Environment evaluate(Environment env) {
				JsonObject object = env.getObject("response");

				assertHasField(object, "$.data.nest.field");

				return env;
			}

		};
		condition.setProperties("test", log, Condition.ConditionResult.FAILURE);

		condition.evaluate(environment);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(log).log(anyString(), captor.capture());

		Map<String, Object> args = captor.getValue();
		String message = String.valueOf(args.get("msg"));

		assertEquals("Successfully validated the nest.field element on the  API response", message);

	}

	@Test
	public void canAssertThatADocumentHasANestedField() {

		JsonObject object = OIDFJSON.toObject(new Gson().toJsonTree(Map.of(
			"data", Map.of(
					"nest", Map.of("field", "value")
			)
		)));

		Environment environment = new Environment();
		environment.putObject("response", object);

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {

			@Override
			public Environment evaluate(Environment env) {
				JsonObject object = env.getObject("response");

				assertHasField(object, "$.data.nest.field");

				return env;
			}

		};
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		condition.evaluate(environment);

	}

	@Test
	public void conditionFailsIfJsonNotValidated() {

		JsonObject object = OIDFJSON.toObject(new Gson().toJsonTree(Map.of(
			"data", Map.of(
				"nest", Map.of("field", "value")
			)
		)));

		Environment environment = new Environment();
		environment.putObject("response", object);

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {

			@Override
			public Environment evaluate(Environment env) {
				JsonObject object = env.getObject("response");

				assertHasField(object, "$.data.nest.other");

				return env;
			}

		};
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		assertThrows(ConditionError.class, () -> { condition.evaluate(environment);});

	}

	@Test
	public void canAssertThatAFieldHasValue() {

		JsonObject object = new JsonObject();
		object.addProperty("string", "this is some data");
		object.addProperty("int", 42);
		object.addProperty("double", 36d);
		object.addProperty("boolean", false);
		object.addProperty("char", 'x');

		Environment environment = new Environment();
		environment.putObject("response", object);

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {

			@Override
			public Environment evaluate(Environment env) {
				JsonObject object = env.getObject("response");

				assertJsonField(object, "$.string", "this is some data");
				assertJsonField(object, "$.int", 42);
				assertJsonField(object, "$.double", 36d);
				assertJsonField(object, "$.boolean", false);
				assertJsonField(object, "$.char", 'x');

				return env;
			}

		};
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE, "");

		condition.evaluate(environment);

	}

	@Test
	public void typeSafetiness() {
		JsonObject object = new JsonObject();
		object.addProperty("string", "this is some data");
		object.addProperty("int", 42);
		object.addProperty("double", 36d);
		object.addProperty("boolean", false);
		object.addProperty("char", 'x');

		Environment environment = new Environment();
		environment.putObject("response", object);

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {

			@Override
			public Environment evaluate(Environment env) {
				JsonObject object = env.getObject("response");

				assertThrows(ConditionError.class, () -> {
					assertJsonField(object, "$.int", "42");
				});
				assertThrows(ConditionError.class, () -> {
					assertJsonField(object, "$.double", "36d");
				});
				assertThrows(ConditionError.class, () -> {
					assertJsonField(object, "$.boolean", "false");
				});
				return env;
			}

		};
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE, "");

		condition.evaluate(environment);

	}

	@BeforeClass
	public static void setup() {
		Configuration.setDefaults(new Configuration.Defaults() {
			private final JsonProvider jsonProvider = new GsonJsonProvider();
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

}
