package net.openid.conformance.condition.client.jsonAsserting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;
import net.openid.conformance.util.field.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractJsonAssertingCondition extends AbstractJsonAssertingUtils {

	public static final String ROOT_PATH = "$.data";
	private static final Gson GSON = JsonUtils.createBigDecimalAwareGson();
	private int errorCount;
	private boolean dontStopOnFailure;

	@Override
	public abstract Environment evaluate(Environment environment);

	protected JsonElement bodyFrom(Environment environment) {
		initAdditionalProperties(environment);
		String resource = environment.getString("resource_endpoint_response");
		return GSON.fromJson(resource, JsonElement.class);
	}

	public void assertField(JsonElement jsonObject, Field field) {
		if (this.dontStopOnFailure) {
			try {
				assertElement(jsonObject, field);
			} catch (ConditionError ignored) {errorCount++;}
		} else {
			assertElement(jsonObject, field);
		}
	}

	private void assertElement(JsonElement jsonObject, Field field) {
		currentField = field.getPath();
		if (!ifExists(jsonObject, field.getPath())) {
			if (field.isOptional()) {
				return;
			} else {
				throw error(ErrorMessagesUtils.createElementNotFoundMessage(field.getPath(), getApiName()),
					args("currentElement", jsonObject));
			}
		}

		JsonElement elementByPath = findByPath(jsonObject, field.getPath());
		if (field.isNullable() && elementByPath.isJsonNull()) {
			return;
		}

		if (elementByPath.isJsonNull()) {
			throw error(ErrorMessagesUtils.createElementCantBeNullMessage(field.getPath(), getApiName()),
				args("path", getPath(), "currentElement", elementByPath));
		}

		if (field instanceof ObjectField) {
			assertObjectField(elementByPath, jsonObject, field);
		} else if (field instanceof ObjectArrayField) {
			assertArrayField(elementByPath, field);
		} else if (field instanceof StringField || field instanceof DatetimeField) {
			assertHasStringField(jsonObject, field.getPath());
			String value = getJsonValueAsString(elementByPath, field.getPath());
			assertPatternAndMaxMinLength(value, field);
			if (field instanceof DatetimeField) {
				assertPatternAndTimeRange(value, (DatetimeField) field, jsonObject);
			}
		} else if (field instanceof IntField) {
			assertHasIntField(jsonObject, field.getPath());
			String value = getJsonValueAsString(elementByPath, field.getPath());
			assertMinAndMaxValue(value, field);
			assertPatternAndMaxMinLength(value, field);
		} else if (field instanceof BooleanField) {
			assertHasBooleanField(jsonObject, field.getPath());
		} else if (field instanceof LatitudeField) {
			assertHasStringField(jsonObject, field.getPath());
			assertLatitude(elementByPath, field);
			String value = getJsonValueAsString(elementByPath, field.getPath());
			assertPatternAndMaxMinLength(value, field);
		} else if (field instanceof LongitudeField) {
			assertHasStringField(jsonObject, field.getPath());
			assertLongitude(elementByPath, field);
			String value = getJsonValueAsString(elementByPath, field.getPath());
			assertPatternAndMaxMinLength(value, field);
		} else if (field instanceof DoubleField) {
			assertHasDoubleField(jsonObject, field.getPath());
			String value = getDoubleValueAsString(jsonObject, field.getPath());
			assertMinAndMaxValue(value, field);
			assertPatternAndMaxMinLength(value, field);
		} else if (field instanceof StringArrayField) {
			assertHasStringArrayField(jsonObject, field.getPath());
			OIDFJSON.getStringArray(elementByPath).forEach(v ->
				assertPatternAndMaxMinLength(v, field));
				assertMinAndMaxItems(elementByPath.getAsJsonArray(), field);
		} else if (field instanceof IntArrayField) {
			assertHasIntArrayField(jsonObject, field.getPath());
			OIDFJSON.getNumberArray(elementByPath).forEach(v -> {
				assertMinAndMaxValue(v.toString(), field);
				assertPatternAndMaxMinLength(v.toString(), field);
			});
			assertMinAndMaxItems(elementByPath.getAsJsonArray(), field);
		} else if (field instanceof NumberField) {
			assertHasNumberField(jsonObject, field.getPath());
			String value = getJsonValueAsString(elementByPath, field.getPath());
			assertMinAndMaxValue(value, field);
			assertPatternAndMaxMinLength(value, field);
		} else if (field instanceof NumberArrayField) {
			assertHasNumberArrayField(jsonObject, field.getPath());
			OIDFJSON.getNumberArray(elementByPath).forEach(v -> {
				assertMinAndMaxValue(v.toString(), field);
				assertPatternAndMaxMinLength(v.toString(), field);
			});
			assertMinAndMaxItems(elementByPath.getAsJsonArray(), field);
		}
	}

	private void initAdditionalProperties(Environment env) {
		String statusString = env.getEffectiveKey("doNotStopOnFailure");
		if (statusString != null) {
			this.dontStopOnFailure = Boolean.parseBoolean(statusString);
		}
		String logOnlyFailureProp = env.getEffectiveKey("logOnlyFailure");
		if (statusString != null) {
			this.logOnlyFailure = Boolean.parseBoolean(logOnlyFailureProp);
		}
	}

	private void assertObjectField(JsonElement elementByPath, JsonElement baseObj, Field field) {
		if (!elementByPath.isJsonObject()) {
			throw error(ErrorMessagesUtils.createObjectClassCastExpMessage(field.getPath(), getApiName()),
				args("path",
				getPath(), "jsonElement", elementByPath));
		}
		parentPath += field.getPath() + ".";
		if (field.getValidator() == null) {
			logInfo(String.format("Field: '%s'. ObjectField. Validator property is empty and inner fields will not be validated", field.getPath()),
				args("path", field.getPath(), "jsonElement", elementByPath));
			parentPath = ".";
			return;
		}
		assertJsonObject(baseObj, field.getPath(), ((ObjectField) field).getValidator());
		parentPath = ".";
	}

	private void assertJsonObject(JsonElement body, String pathToJsonObject, Consumer<JsonObject> consumer) {
		JsonObject object = (JsonObject) findByPath(body, pathToJsonObject);
		consumer.accept(object.getAsJsonObject());
	}

	private void assertArrayField(JsonElement elementByPath, Field field) {
		ObjectArrayField objectArrayField = (ObjectArrayField) field;
		if (!elementByPath.isJsonArray()) {
			throw error(ErrorMessagesUtils.createArrayClassCastExpMessage(objectArrayField.getPath(), getApiName()));
		}
		JsonArray array = elementByPath.getAsJsonArray();
		parentPath +=  (parentPath.contains(field.getPath()))? "" : field.getPath() + ".";
		assertMinAndMaxItems(array, objectArrayField);
		if (field.getValidator() == null) {
			logInfo(String.format("Field: '%s'. ObjectArrayField. Validator property is empty and inner fields will not be validated", field.getPath()),
				args("path", field.getPath(), "jsonElement", elementByPath));
			parentPath = ".";
			return;
		}
		array.forEach(json -> ((ObjectArrayField) field).getValidator().accept(json.getAsJsonObject()));
		parentPath = ".";
	}

	public void assertGeographicCoordinates(JsonObject body) {
		assertField(body,
			new ObjectField
				.Builder("geographicCoordinates")
				.setOptional()
				.setValidator(geo -> {
					assertField(geo,
						new LatitudeField.Builder()
							.setOptional()
							.build());
					assertField(geo,
						new LongitudeField.Builder()
							.setOptional()
							.build());
				})
				.build());
	}

	protected void logFinalStatus() {
		logSuccess(ErrorMessagesUtils.createTotalElementsFoundMessage(totalElements, getApiName()));
		if (errorCount > 0) {
			throw error(ErrorMessagesUtils.createTotalErrorsFoundMessage(errorCount, getApiName()));
		}
	}

	private void logInfo(String msg, Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("msg", msg);
		copy.put("result", ConditionResult.INFO);
		if (!getRequirements().isEmpty()) {
			copy.put("requirements", getRequirements());
		}
		log(copy);
	}

	private boolean ifExists(JsonElement jsonObject, String path) {
		try {
			JsonPath.read(jsonObject, path);
			return true;
		} catch (PathNotFoundException e) {
			return false;
		}
	}
}
