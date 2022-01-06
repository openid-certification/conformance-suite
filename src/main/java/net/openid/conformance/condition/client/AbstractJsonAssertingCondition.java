package net.openid.conformance.condition.client;

import com.google.gson.*;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.tools.checkEnums.EnumChecker;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;
import net.openid.conformance.util.field.*;
import net.openid.conformance.validation.Match;
import net.openid.conformance.validation.RegexMatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.annotation.meta.field;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.openid.conformance.testmodule.OIDFJSON.UnexpectedJsonTypeException;

public abstract class AbstractJsonAssertingCondition extends AbstractCondition {

	private static final Gson GSON = JsonUtils.createBigDecimalAwareGson();

	private static final Logger logger = LoggerFactory.getLogger(AbstractJsonAssertingCondition.class);

	private static final Pattern JSONPATH_PRETTIFIER = Pattern.compile("(\\$\\.data\\.|\\$\\.data\\[\\d\\]\\.)(?<path>.+)");
	public static final String ROOT_PATH = "$.data";
	private String parentPath = "";
	private String currentField = "";
	private JsonElement currentElement;
	private boolean logOnlyFailure;
	private boolean dontStopOnFailure;
	private int totalElements;
	private int errorCount;

	@Override
	public abstract Environment evaluate(Environment environment);

	protected JsonObject bodyFrom(Environment environment) {
		String resource = environment.getString("resource_endpoint_response");
		String entityString = (resource == null)? null : parseResource(resource);
		String statusString = environment.getEffectiveKey("doNotStopOnFailure");
		if (statusString != null) {
			this.dontStopOnFailure = Boolean.parseBoolean(statusString);
		}
		return  GSON.fromJson(entityString, JsonObject.class);
	}

	private String parseResource(String resource) {
		JsonElement jsonElement = new JsonParser().parse(resource);
		if (jsonElement.isJsonArray()) {
			JsonObject body = new JsonObject();
			body.add("data", jsonElement);
			return body.toString();
		}
		return resource;
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
			throw error(String.format("Field at %s must be a string but %s was found", path, found.getClass().getSimpleName()), jsonObject);
		}
	}

	protected void assertCurrencyType(JsonObject jsonObject, Field field) {
		assertHasStringField(jsonObject, field.getPath());
		String value = getJsonValueAsString(jsonObject, field.getPath());
		assertCurrencyNotNa(value, field);
		assertField(jsonObject, field);
	}

	public void assertField(JsonObject jsonObject, Field field) {
		if (dontStopOnFailure) {
			try {
				assertElement(jsonObject, field);
			} catch (ConditionError ignored) {errorCount++;}
		} else {
			assertElement(jsonObject, field);
		}
	}


	private void assertElement(JsonObject jsonObject, Field field) {
		this.currentElement = jsonObject;
		this.currentField = field.getPath();
		if (!ifExists(jsonObject, field.getPath())) {
			if (field.isOptional()){
				return;
			} else {
				throw error(createElementNotFoundMessage(field.getPath()), jsonObject);
			}
		}

		JsonElement elementByPath = findByPath(jsonObject, field.getPath());
		if (field.isNullable() && elementByPath.isJsonNull()) {
			return;
		}

		if (elementByPath.isJsonNull()) {
			throw error(createElementCantBeNullMessage(field.getPath()), args("path",
				getPath(), "jsonElement", currentElement));
		}

		if (field instanceof ObjectField) {
			assertObjectField(elementByPath, jsonObject, field);
		} else if (field instanceof ObjectArrayField) {
			assertArrayField(elementByPath, field);
		} else if (field instanceof StringField || field instanceof DatetimeField) {
			assertHasStringField(jsonObject, field.getPath());
			String value = getJsonValueAsString(jsonObject, field.getPath());
			assertPatternAndMaxMinLength(value, field);
			if (field instanceof DatetimeField) {
				assertPatternAndTimeRange(value, (DatetimeField) field, jsonObject);
			}
		} else if (field instanceof IntField) {
			assertHasIntField(jsonObject, field.getPath());
			String value = getJsonValueAsString(jsonObject, field.getPath());
			assertMinAndMaxValue(value, field);
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
			String value = getDoubleValueAsString(jsonObject, field.getPath());
			assertMinAndMaxValue(value, field);
			assertPatternAndMaxMinLength(value, field);
		} else if (field instanceof StringArrayField) {
			assertHasStringArrayField(jsonObject, field.getPath());
			OIDFJSON.getStringArray(elementByPath).forEach(v -> assertPatternAndMaxMinLength(v, field));
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
			String value = getJsonValueAsString(jsonObject, field.getPath());
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

	private void assertObjectField(JsonElement elementByPath, JsonObject baseObj, Field field) {
		if (!elementByPath.isJsonObject()) {
			throw error(createObjectClassCastExpMessage(field.getPath()), args("path",
				getPath(), "jsonElement", elementByPath));
		}
		this.parentPath += field.getPath() + ".";
		if (field.getValidator() == null) {
			logInfo(String.format("Field: '%s'. ObjectField. Validator property is empty and inner fields will not be validated", field.getPath()),
				args("path", field.getPath(), "jsonElement", elementByPath));
			this.parentPath = ".";
			return;
		}
		assertJsonObject(baseObj, field.getPath(), ((ObjectField) field).getValidator());
		this.parentPath = ".";
	}

	private void assertArrayField(JsonElement elementByPath, Field field) {
		ObjectArrayField objectArrayField = (ObjectArrayField) field;
		if (!elementByPath.isJsonArray()) {
			throw error(createArrayClassCastExpMessage(objectArrayField.getPath()));
		}
		JsonArray array = elementByPath.getAsJsonArray();
		this.parentPath +=  (this.parentPath.contains(field.getPath()))? "" : field.getPath() + ".";
		assertMinAndMaxItems(array, objectArrayField);
		if (field.getValidator() == null) {
			logInfo(String.format("Field: '%s'. ObjectArrayField. Validator property is empty and inner fields will not be validated", field.getPath()),
				args("path", field.getPath(), "jsonElement", elementByPath));
			this.parentPath = ".";
			return;
		}
		array.forEach(json -> ((ObjectArrayField) field).getValidator().accept(json.getAsJsonObject()));
		this.parentPath = ".";
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

	protected void assertHasNumberField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getNumber(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + " was not an Number",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasIntField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getInt(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + path + " was not an int",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasDoubleField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getDouble(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a double",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasFloatField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getFloat(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + path + " was not a float",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasLongField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getLong(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + path + " was not a long",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasBooleanField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getBoolean(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + " was not a boolean",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasStringArrayField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getStringArray(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + " was not an array of strings",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasIntArrayField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getIntArray(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + " was not an array of Integers",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasNumberArrayField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getNumberArray(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + " was not an array of Numbers",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasCharField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getCharacter(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + " was not a character",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasShortField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getShort(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + " was not a short",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertHasByteField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getByte(found);
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + " was not a byte",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	/**
	 Use assertField(JsonObject jsonObject, String path)
	 */
	@Deprecated
	protected void assertJsonObject(JsonObject body, String pathToJsonObject, Consumer<JsonObject> consumer) {
		JsonObject object = (JsonObject) findByPath(body, pathToJsonObject);
		consumer.accept(object.getAsJsonObject());
	}

	/**
	 Use assertField(JsonObject jsonObject, String path)
	 */
	@Deprecated
	public void assertJsonArrays(JsonObject body, String pathToJsonArray, Consumer<JsonObject> consumer) {
		JsonElement jsonElement = findByPath(body, pathToJsonArray);
		JsonArray array = (JsonArray) jsonElement;
		array.forEach(jsonObject -> consumer.accept(jsonObject.getAsJsonObject()));
	}


	protected void assertJsonField(JsonObject jsonObject, String path, String expected) {
		JsonElement actual = findByPath(jsonObject, path);
		String stringValue = getOrFail(() -> OIDFJSON.getString(actual));
		if (!stringValue.equals(expected)) {
			throw error(String.format("Path %s did not match %s", this.parentPath, expected),
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, Match match) {
		String stringValue = getJsonValueAsString(jsonObject, path);
		if (!match.matches(stringValue)) {
			throw error(String.format("Path %s did not match %s", this.parentPath, match),
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, String... expected) {
		JsonElement actual = findByPath(jsonObject, path);
		List<String> array = getOrFail(() -> OIDFJSON.getStringArray(actual));
		List<String> found = Arrays.stream(expected)
			.filter(s -> !array.contains(s))
			.collect(Collectors.toList());
		if (found.size() != 0) { //NOPMD
			throw error(String.format("Headers did not contain all of %s", String.join(" ", expected)), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, Number expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Number number = getOrFail(() -> OIDFJSON.getNumber(actual));
		if (!number.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, expected),
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, Character expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Character c = getOrFail(() -> OIDFJSON.getCharacter(actual));
		if (!c.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, String.valueOf(expected)),
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, boolean expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Boolean bool = getOrFail(() -> OIDFJSON.getBoolean(actual));
		if (!bool.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, String.valueOf(expected)),
				args("path", getPath(), "currentElement", this.currentElement));
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
			totalElements++;
			return element;
		} catch (PathNotFoundException e) {
			throw error(createElementNotFoundMessage(path),
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	protected JsonElement findDoubleByPath(JsonObject jsonObject, String path) {
		logQuerying(path);
		if (jsonObject.has(path)) {
			JsonElement element = jsonObject.get(path);
			logElementFound(path);
			totalElements++;
			return element;
		} else {
			throw error(createElementNotFoundMessage(path),
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	public void setLogOnlyFailure() {
		log("Log Only Failure Mode is ON");
		this.logOnlyFailure = true;
	}

	protected void logFinalStatus() {
		logSuccess(createTotalElementsFoundMessage(totalElements));
		if (errorCount > 0) {
			throw error(createTotalErrorsFoundMessage(errorCount));
		}
	}
	private void logElementFound(String elementName) {
		if (!logOnlyFailure) {
			logSuccess(createElementFoundMessage(elementName));
		}
	}

	private void logQuerying(String elementName) {
		if (!logOnlyFailure) {
			log(createQueryMessage(elementName));
		}
	}

	private void assertLatitude(JsonObject jsonObject, Field doubleField) {
		JsonElement found = findByPath(jsonObject, doubleField.getPath());
		try {
			String rawValue = OIDFJSON.getString(found);
			double latitude = Double.parseDouble(rawValue);
			if (latitude > 90.0 || latitude < -90.0) {
				throw error(createCoordinateIsNotWithinAllowedAreaMessage(doubleField.getPath()), jsonObject);
			}
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + " was not a string",
				args("path", getPath(), "currentElement", this.currentElement));
		} catch (NumberFormatException nfe) {
			throw error("Field at " + this.parentPath + " could not be parsed to a double",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	private void assertLongitude(JsonObject jsonObject, Field doubleField) {
		JsonElement found = findByPath(jsonObject, doubleField.getPath());
		try {
			String rawValue = OIDFJSON.getString(found);
			double Longitude = Double.parseDouble(rawValue);
			if (Longitude > 180.0 || Longitude < -180.0) {
				throw error(createCoordinateIsNotWithinAllowedAreaMessage(doubleField.getPath()), jsonObject);
			}
		} catch (UnexpectedJsonTypeException u) {
			throw error("Field at " + this.parentPath + " was not a string",
				args("path", getPath(), "currentElement", this.currentElement));
		} catch (NumberFormatException nfe) {
			throw error("Field at " + this.parentPath + " could not be parsed to a double",
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	public String createObjectClassCastExpMessage(String elementName) {
		return String.format("Class cast exception, expect JsonObject, but found JsonArray. " +
			"Field: %s on the %s API response",	elementName, getApiName());
	}

	public String createArrayClassCastExpMessage(String elementName) {
		return String.format("Class cast exception, expect JsonArray, but found JsonObject. " +
			"Field: %s on the %s API response",	elementName, getApiName());
	}
	public String createQueryMessage(String elementName) {
		return String.format("Looking up %s on the %s API response", elementName, getApiName());
	}
	public String createTotalElementsFoundMessage(int totalElements) {
		return String.format("Successfully validated %d elements on the %s API response",
			totalElements, getApiName());
	}
	public String createTotalErrorsFoundMessage(int totalElements) {
		return String.format("Found %d errors on the %s API response", totalElements, getApiName());
	}
	public String createElementCantBeNullMessage(String elementName) {
		return String.format("Field %s cant be null on the %s API response", elementName, getApiName());
	}

	public String createElementFoundMessage(String elementName) {
		return String.format("The %s element is present in the %s API response", elementName, getApiName());
	}

	public String createElementNotFoundMessage(String elementName) {
		return String.format("Unable to find element %s on the %s API response", elementName, getApiName());
	}

	public String createCurrencyNotNaMessage(String elementName) {
		return String.format("Value from element %s doesn't match the required pattern on the %s API response.\nThis is a known issue, please view this link for orientation on this issue: https://openbanking-brasil.github.io/areadesenvolvedor/#problemas-conhecidos-da-especificacao", elementName, getApiName());
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

	public String createFieldIsntInSecondsRange(String elementName) {
		return String.format("Value from element %s is older or younger then the required limit " +
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
			stringValue = OIDFJSON.getString(actual);
		} catch (UnexpectedJsonTypeException e) {
			try {
				stringValue = String.valueOf(OIDFJSON.getNumber(actual));
			} catch (UnexpectedJsonTypeException ex) {
				throw error(String.format("Path %s was not a string or number", this.parentPath),
					args("path", getPath(), "currentElement", this.currentElement));
			}
		}
		return stringValue;
	}

	private String getDoubleValueAsString(JsonObject jsonObject, String path) {
		JsonElement actual = findDoubleByPath(jsonObject, path);
		String stringValue = "";
		try {
			stringValue = String.valueOf(OIDFJSON.getNumber(actual));
		} catch (UnexpectedJsonTypeException ex) {
			throw error(String.format("Path %s was not a number", this.parentPath),
				args("path", getPath(), "currentElement", this.currentElement));
		}
		return stringValue;
	}

	private void assertPatternAndTimeRange(String stringFieldValue, DatetimeField field, JsonObject jsonObject) {
		if (!field.getPattern().isEmpty()) {
			assertRegexMatchesField(stringFieldValue, field.getPath(), field.getPattern());
		}
		if (field.getDaysOlderAccepted() > 0) {
			assertDaysOlderAccepted(stringFieldValue, field.getPath(), field.getDaysOlderAccepted());
		}
		if (field.getSecondsOlderThanSeconds() > 0) {
			assertSecondsComparison(field.getSecondsOlderThanSeconds(), field.getPath(), stringFieldValue, getJsonValueAsString(jsonObject, field.getSecondsOlderThanString()));
		}
	}

	private void assertPatternAndMaxMinLength(String stringFieldValue, Field field) {
		if (!field.getPattern().isEmpty()) {
			assertRegexMatchesField(stringFieldValue, field.getPath(), field.getPattern());
		}
		if (field.getMaxLength() > 0) {
			assertMaxLength(stringFieldValue, field.getPath(), field.getMaxLength());
		}
		if (field.getMinLength() > 0) {
			assertMinLength(stringFieldValue, field.getPath(), field.getMinLength());
		}
		if (!field.getEnums().isEmpty()) {
			String className = getClass().getSimpleName();
			if ( StringUtils.isEmpty(className)) {
				className = getClass().getName();
			}
			EnumChecker.getInstance().check(field, className);
			assertValueFromEnum(stringFieldValue, field.getEnums(), field.getPath());
		}
	}

	private void assertCurrencyNotNa(String fieldValue, Field field){
		if(fieldValue.equalsIgnoreCase("NA")){
			throw error(createCurrencyNotNaMessage(field.getPath()),
				args("path", getPath(), "currentElement", this.currentElement));
		}
	}

	private void assertValueFromEnum(String fieldValue, Set<String> enums, String path) {
		if (!enums.contains(fieldValue)) {
			throw error(createFieldValueNotMatchEnumerationMessage(path),
				args("path", getPath(), "value", fieldValue, "enums", enums));
		}
	}

	private void assertMinAndMaxItems(JsonArray array, Field field) {
		if (array.size() < field.getMinItems()) {
			throw error(createArrayIsLessThanMaxItemsMessage(field.getPath()), args("path",
				getPath(), "currentElement", this.currentElement));
		}

		if (field.getMaxItems() != 0 && array.size() > field.getMaxItems()) {
			throw error(createArrayIsMoreThanMaxItemsMessage(field.getPath()), args("path",
				getPath(), "currentElement", this.currentElement));
		}
	}

	private void assertRegexMatchesField(String value, String path, String pattern) {
		Match match = RegexMatch.regex(pattern);
		if (!match.matches(value)) {
			throw error(createFieldValueNotMatchPatternMessage(path),
				args("path", getPath(), "value", value, "pattern", pattern, "currentElement", currentElement));
		}
	}

	private void assertMaxLength(String stringValue, String path, int maxLength) {
		if (stringValue.length() > maxLength) {
			throw error(createFieldValueIsMoreThanMaxLengthMessage(path),
				args("path", getPath(), "value", stringValue, "required MaxLength", maxLength));
		}
	}

	private void assertMinLength(String stringValue, String path, int minLength) {
		if (stringValue.length() < minLength) {
			throw error(createFieldValueIsLessThanMinLengthMessage(path),
				args("path", getPath(), "value", stringValue, "required MinLength", minLength));
		}
	}

	private void assertMinAndMaxValue(String value, Field field) {
		if (field.getMinValue() != null && new BigDecimal(value).compareTo(field.getMinValue()) < 0) {
			throw error(createFieldValueIsLessThanMinimum(field.getPath()), args("value", value, "path", getPath(), "required MinValue", field.getMinValue()));

		}
		if (field.getMaxValue() != null && new BigDecimal(value).compareTo(field.getMaxValue()) > 0) {
			throw error(createFieldValueIsMoreThanMaximum(field.getPath()), args("path", getPath(), "value", value, "required MaxValue", field.getMaxValue()));
		}
	}

	private void assertDaysOlderAccepted(String stringValue, String path, int daysOlderAccepted) {
		if (Instant.parse(stringValue).isAfter(Instant.now().plus(daysOlderAccepted, ChronoUnit.DAYS))) {
			throw error(createFieldValueIsOlderThanLimit(path),
				args("path", getPath(), "value", stringValue));
		}
	}

	private void assertSecondsComparison(int secondsOlder, String path, String currentValue, String valueComparedTo) {
//		System.out.println("Current value: " + currentValue);
//		System.out.println("Value to compare to: " + valueComparedTo);
//		System.out.println("Path: " + path);
//		System.out.println("Seconds older: " + secondsOlder);
//		System.out.println();
		if (Instant.parse(currentValue).isAfter(Instant.parse(valueComparedTo).plus(secondsOlder+5, ChronoUnit.SECONDS)) || Instant.parse(currentValue).isBefore(Instant.parse(valueComparedTo).plus(secondsOlder, ChronoUnit.SECONDS))) {
			throw error(createFieldIsntInSecondsRange(path));
		}
	}

	private String getPath() {
		return this.parentPath + this.currentField;
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
