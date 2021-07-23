package net.openid.conformance.condition.client;

import com.google.gson.*;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;
import net.openid.conformance.util.field.*;
import net.openid.conformance.validation.Match;
import net.openid.conformance.validation.RegexMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import springfox.documentation.spring.web.json.Json;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.openid.conformance.testmodule.OIDFJSON.*;

public abstract class AbstractJsonAssertingCondition extends AbstractCondition {

	private static Gson GSON = JsonUtils.createBigDecimalAwareGson();

	private static final Logger logger = LoggerFactory.getLogger(AbstractJsonAssertingCondition.class);

	private static final Pattern JSONPATH_PRETTIFIER = Pattern.compile("(\\$\\.data\\.|\\$\\.data\\[\\d\\]\\.)(?<path>.+)");
	public static final String ROOT_PATH = "$.data";

	public abstract Environment evaluate(Environment environment);

	protected JsonObject bodyFrom(Environment environment) {
		String entityString = environment.getString("resource_endpoint_response");
		return GSON.fromJson(entityString, JsonObject.class);
	}

	protected JsonObject headersFrom(Environment environment) {
		return environment.getObject("resource_endpoint_response_headers");
	}

	protected void assertStatus(int expected, Environment environment) {
		int actual = statusFrom(environment);
		if (expected != actual) {
			throw error(String.format("Expected HTTP response code to be %d but it was %d", expected, actual));
		}
	}

	protected int statusFrom(Environment environment) {
		JsonObject responseCode = environment.getObject("resource_endpoint_response_code");
		JsonElement code = responseCode.get("code");
		return OIDFJSON.getInt(code);
	}

	protected void assertHasField(JsonObject jsonObject, String path) {
		findByPath(jsonObject, path);
	}

	protected void assertHasStringField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getString(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a string", jsonObject);
		}
	}

	//need to think about better impl then the current one
	protected void assertField(JsonObject jsonObject, Field field) {
		if (field.isOptional() && (!ifExists(jsonObject, field.getPath()) || findByPath(jsonObject, field.getPath()).isJsonNull())) {
			return;
		}
		if (field instanceof StringField || field instanceof DatetimeField) {
			assertHasStringField(jsonObject, field.getPath());
			String value = getJsonValueAsString(jsonObject, field.getPath());
			assertPatternAndMaxMinLength(value, field);
			if (field instanceof DatetimeField) {
				assertPatternAndTimeRange(value, (DatetimeField) field);
			}
		} else if (field instanceof IntField) {
			assertHasIntField(jsonObject, field.getPath());
			String value = getJsonValueAsString(jsonObject, field.getPath());
			assertPatternAndMaxMinLength(value, field);
		} else if (field instanceof BooleanField) {
			assertHasBooleanField(jsonObject, field.getPath());
		} else if (field instanceof LatitudeField) {
			assertHasStringField(jsonObject, field.getPath());
			assertLatitude(jsonObject, field);
			String value = getJsonValueAsString(jsonObject, field.getPath());
			assertPatternAndMaxMinLength(value, field);
		} else if (field instanceof LongitudeField) {
			assertHasStringField(jsonObject, field.getPath());
			assertLongitude(jsonObject, field);
			String value = getJsonValueAsString(jsonObject, field.getPath());
			assertPatternAndMaxMinLength(value, field);
		} else if (field instanceof DoubleField) {
			assertHasDoubleField(jsonObject, field.getPath());
			String value = getJsonValueAsString(jsonObject, field.getPath());
			assertPatternAndMaxMinLength(value, field);
		} else if (field instanceof StringArrayField) {
			assertHasStringArrayField(jsonObject, field.getPath());
			JsonElement found = findByPath(jsonObject, field.getPath());
			OIDFJSON.getStringArray(found).forEach(v -> assertPatternAndMaxMinLength(v, field));
			assertMinAndMaxItems(found.getAsJsonArray(), field);
		} else if (field instanceof ArrayField) {
			JsonElement found = findByPath(jsonObject, field.getPath());
			assertMinAndMaxItems(found.getAsJsonArray(), field);
		}
	}

	protected void assertGeographicCoordinates(JsonObject body) {
		JsonObject geographicCoordinates = findByPath(body, "geographicCoordinates").getAsJsonObject();

		assertField(geographicCoordinates,
			new LatitudeField.Builder()
				.setOptional()
				.build());

		assertField(geographicCoordinates,
			new LongitudeField.Builder()
				.setOptional()
				.build());

	}

	protected void assertHasIntField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getInt(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not an int", jsonObject);
		}
	}

	protected void assertHasDoubleField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getDouble(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a double", jsonObject);
		}
	}

	protected void assertHasFloatField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getFloat(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a float", jsonObject);
		}
	}

	protected void assertHasLongField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getLong(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a long", jsonObject);
		}
	}

	protected void assertHasBooleanField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getBoolean(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a boolean", jsonObject);
		}
	}

	protected void assertHasStringArrayField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getStringArray(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not an array of strings", jsonObject);
		}
	}

	protected void assertHasCharField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getCharacter(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a character", jsonObject);
		}
	}

	protected void assertHasShortField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getShort(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a short", jsonObject);
		}
	}

	protected void assertHasByteField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getByte(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a byte", jsonObject);
		}
	}

	protected void assertJsonObject(JsonObject body, String pathToJsonObject, Consumer<JsonObject> consumer) {
		JsonObject object = (JsonObject) findByPath(body, pathToJsonObject);
		consumer.accept(object.getAsJsonObject());
	}

	protected void assertJsonField(JsonObject jsonObject, String path, String expected) {
		JsonElement actual = findByPath(jsonObject, path);
		String stringValue = getOrFail(() -> getString(actual));
		if (!stringValue.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, expected), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, Match match) {
		String stringValue = getJsonValueAsString(jsonObject, path);
		if (!match.matches(stringValue)) {
			throw error(String.format("Path %s did not match %s", path, match), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, String... expected) {
		JsonElement actual = findByPath(jsonObject, path);
		List<String> array = getOrFail(() -> getStringArray(actual));
		List<String> found = Arrays.stream(expected)
			.filter(s -> !array.contains(s))
			.collect(Collectors.toList());
		if (found.size() != 0) {
			throw error(String.format("Headers did not contain all of %s", String.join(" ", expected)), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, Number expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Number number = getOrFail(() -> getNumber(actual));
		if (!number.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, expected), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, Character expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Character c = getOrFail(() -> getCharacter(actual));
		if (!c.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, String.valueOf(expected)), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, boolean expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Boolean bool = getOrFail(() -> getBoolean(actual));
		if (!bool.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, String.valueOf(expected)), jsonObject);
		}
	}

	protected JsonElement findByPath(JsonObject jsonObject, String path) {

		Matcher matcher = JSONPATH_PRETTIFIER.matcher(path);
		String elementName = "data";
		if (matcher.matches()) {
			elementName = matcher.group("path");
		} else {
			elementName = path;
		}

		try {
			logQuerying(elementName);
			JsonElement element = JsonPath.parse(jsonObject).read(path);
			logElementFound(elementName);
			return element;
		} catch (PathNotFoundException e) {
			throw error(createElementNotFoundMessage(path), jsonObject);
		}
	}

	private void logElementFound(String elementName) {
		logSuccess(createElementFoundMessage(elementName));
	}

	private void logQuerying(String elementName) {
		log(createQueryMessage(elementName));
	}

	private void assertLatitude(JsonObject jsonObject, Field doubleField) {
		JsonElement found = findByPath(jsonObject, doubleField.getPath());
		try {
			String rawValue = getString(found);
			double latitude = Double.parseDouble(rawValue);
			if (latitude > 90.0 || latitude < -90.0) {
				throw error(createCoordinateIsNotWithinAllowedAreaMessage(doubleField.getPath()), jsonObject);
			}
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + doubleField.getPath() + " was not a string", jsonObject);
		} catch (NumberFormatException nfe) {
			throw error("Field at " + doubleField.getPath() + " could not be parsed to a double", jsonObject);
		}
	}

	private void assertLongitude(JsonObject jsonObject, Field doubleField) {
		JsonElement found = findByPath(jsonObject, doubleField.getPath());
		try {
			String rawValue = getString(found);
			double Longitude = Double.parseDouble(rawValue);
			if (Longitude > 180.0 || Longitude < -180.0) {
				throw error(createCoordinateIsNotWithinAllowedAreaMessage(doubleField.getPath()), jsonObject);
			}
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + doubleField.getPath() + " was not a string", jsonObject);
		} catch (NumberFormatException nfe) {
			throw error("Field at " + doubleField.getPath() + " could not be parsed to a double", jsonObject);
		}
	}

	public String createQueryMessage(String elementName) {
		return String.format("Looking up %s on the %s API response", elementName, getApiName());
	}

	public String createElementFoundMessage(String elementName) {
		return String.format("Successfully validated the %s element on the %s API response", elementName, getApiName());
	}

	public String createElementNotFoundMessage(String elementName) {
		return String.format("Unable to find element %s on the %s API response", elementName, getApiName());
	}

	public String createFieldValueNotMatchPatternMessage(String elementName) {
		return String.format("Value from element %s doesn't match the required pattern on the %s API response",
			elementName, getApiName());
	}

	public String createFieldValueNotMatchEnumerationMessage(String elementName) {
		return String.format("Value from element %s does not match any given enumeration on the " +
			"%s API response", elementName, getApiName());
	}

	public String createFieldValueIsMoreThanMaxLengthMessage(String elementName) {
		return String.format("Value from element %s is more than the required maxLength on the " +
			"%s API response", elementName, getApiName());
	}

	public String createArrayIsMoreThanMaxItemsMessage(String elementName) {
		return String.format("Array from element %s is more than the required maxItems on the " +
			"%s API response", elementName, getApiName());
	}

	public String createArrayIsLessThanMaxItemsMessage(String elementName) {
		return String.format("Array from element %s is less than the required minItems on the " +
			"%s API response", elementName, getApiName());
	}

	public String createFieldValueIsLessThanMinLengthMessage(String elementName) {
		return String.format("Value from element %s is less than the required minLength " +
			"on the %s API response", elementName, getApiName());
	}

	public String createFieldValueIsMoreThanMaximum(String elementName) {
		return String.format("Value from element %s is more than the required maximum " +
			"on the %s API response", elementName, getApiName());
	}

	public String createFieldValueIsOlderThanLimit(String elementName) {
		return String.format("Value from element %s is a date older then the required limit " +
			"on the %s API response", elementName, getApiName());
	}

	public String createFieldValueIsLessThanMinimum(String elementName) {
		return String.format("Value from element %s is less than the required minimum " +
			"on the %s API response", elementName, getApiName());
	}

	public String createCoordinateIsNotWithinAllowedAreaMessage(String elementName) {
		return String.format("The %s does not enter to coordinate area. " +
			"It is not latitude or longitude", elementName, getApiName());
	}

	private final String getApiName() {
		Class<?> clazz = getClass();
		ApiName apiName = clazz.getDeclaredAnnotation(ApiName.class);
		return apiName == null ? clazz.getSimpleName() : apiName.value();
	}

	protected void assertJsonArrays(JsonObject body, String pathToJsonArray, Consumer<JsonObject> consumer) {
		JsonElement jsonElement = findByPath(body, pathToJsonArray);
		JsonArray array = (JsonArray) jsonElement;
		array.forEach(jsonObject -> consumer.accept(jsonObject.getAsJsonObject()));
	}

	protected void assertOptionalJsonArrays(JsonObject body, String pathToJsonArray, Consumer<JsonObject> consumer) {
		try {
			JsonArray array = (JsonArray) findByPath(body, pathToJsonArray);
			array.forEach(jsonObject -> consumer.accept(jsonObject.getAsJsonObject()));
		} catch (ConditionError error) {
			logger.error("The optional field not found. " + error.getMessage());
		}
	}

	private boolean ifExists(JsonObject jsonObject, String path) {
		try {
			JsonPath.read(jsonObject, path);
			return true;
		} catch (PathNotFoundException e) {
			return false;
		}
	}

	private String getJsonValueAsString(JsonObject jsonObject, String path) {
		JsonElement actual = findByPath(jsonObject, path);
		String stringValue = "";
		try {
			stringValue = getString(actual);
		} catch (UnexpectedJsonTypeException e) {
			try {
				stringValue = String.valueOf(getNumber(actual));
			} catch (UnexpectedJsonTypeException ex) {
				throw error(String.format("Path %s was not a string or number", path), jsonObject);
			}
		}
		return stringValue;
	}

	private void assertPatternAndTimeRange(String stringFieldValue, DatetimeField field) {
		if (!field.getPattern().isEmpty()) {
			assertRegexMatchesField(stringFieldValue, field.getPath(),
				RegexMatch.regex(field.getPattern()));
		}
		if (field.getDaysOlderAccepted() > 0) {
			assertDaysOlderAccepted(stringFieldValue, field.getPath(), field.getDaysOlderAccepted());
		}
	}

	private void assertPatternAndMaxMinLength(String stringFieldValue, Field field) {
		if (!field.getPattern().isEmpty()) {
			assertRegexMatchesField(stringFieldValue, field.getPath(),
				RegexMatch.regex(field.getPattern()));
		}
		if (field.getMaxLength() > 0) {
			assertMaxLength(stringFieldValue, field.getPath(), field.getMaxLength());
		}
		if (field.getMinLength() > 0) {
			assertMinLength(stringFieldValue, field.getPath(), field.getMinLength());
		}
		if (!field.getEnums().isEmpty()) {
			assertValueFromEnum(stringFieldValue, field.getEnums(), field.getPath());
		}
		if (field.getMaxValue() > 0) {
			assertMaxValue(stringFieldValue, field.getPath(), field.getMaxValue());
		}
	}

	private void assertValueFromEnum(String fieldValue, Set<String> enums, String path) {
		if (!enums.contains(fieldValue)) {
			throw error(createFieldValueNotMatchEnumerationMessage(path));
		}
	}

	private void assertMinAndMaxItems(JsonArray array, Field field) {
		if (array.size() < field.getMinItems()) {
			throw error(createArrayIsLessThanMaxItemsMessage(field.getPath()));
		}

		if (field.getMaxItems() != 0 && array.size() > field.getMaxItems()) {
			throw error(createArrayIsMoreThanMaxItemsMessage(field.getPath()));
		}
	}

	private void assertRegexMatchesField(String value, String path, Match match) {
		if (!match.matches(value)) {
			throw error(createFieldValueNotMatchPatternMessage(path),
				args("path", path, "value", value));
		}
	}

	private void assertMaxLength(String stringValue, String path, int maxLength) {
		if (stringValue.length() > maxLength) {
			throw error(createFieldValueIsMoreThanMaxLengthMessage(path));
		}
	}

	private void assertMinLength(String stringValue, String path, int minLength) {
		if (stringValue.length() < minLength) {
			throw error(createFieldValueIsLessThanMinLengthMessage(path));
		}
	}

	private void assertMaxValue(String stringValue, String path, int maxValue) {
		if (Integer.parseInt(stringValue) > maxValue) {
			throw error(createFieldValueIsMoreThanMaximum(path));
		}
	}

	private void assertDaysOlderAccepted(String stringValue, String path, int daysOlderAccepted) {
		if (Instant.parse(stringValue).isAfter(Instant.now().plus(daysOlderAccepted, ChronoUnit.DAYS))) {
			throw error(createFieldValueIsOlderThanLimit(path));
		}
	}

	private <T> T getOrFail(Lambda<T> lambda) {
		try {
			return lambda.execute();
		} catch (UnexpectedJsonTypeException u) {
			throw error("Wrong datatype being verified in json", u);
		}
	}

	@FunctionalInterface
	interface Lambda<T> {
		T execute();
	}
}
