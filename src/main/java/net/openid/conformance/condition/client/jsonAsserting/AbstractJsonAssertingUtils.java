package net.openid.conformance.condition.client.jsonAsserting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.tools.checkEnums.EnumChecker;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.Field;
import net.openid.conformance.validation.Match;
import net.openid.conformance.validation.RegexMatch;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractJsonAssertingUtils extends AbstractCondition {

	private static final Pattern JSONPATH_PRETTIFIER = Pattern.compile("(\\$\\.data\\.|\\$\\.data\\[\\d\\]\\.)(?<path>.+)");
	protected String parentPath = "";
	protected String currentField = "";
	protected boolean logOnlyFailure;
	protected int totalElements;

	protected void assertHasField(JsonElement jsonObject, String path) {
		findByPath(jsonObject, path);
	}

	protected void assertHasStringField(JsonElement jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getString(found);
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error(String.format("Field at %s must be a string but %s was found", path,
					found.getClass().getSimpleName()), args("value", jsonObject));
		}
	}

	protected void assertHasNumberField(JsonElement jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getNumber(found);
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not an Number",
					args("path", getPath(), "currentElement", jsonObject));
		}
	}

	protected void assertHasIntField(JsonElement jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getInt(found);
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error("Field at " +  path + " was not an int",
					args("path", getPath(), "currentElement", jsonObject));
		}
	}

	protected void assertHasDoubleField(JsonElement jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getDouble(found);
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a double",
					args("path", getPath(), "currentElement", jsonObject));
		}
	}

	protected void assertHasBooleanField(JsonElement jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getBoolean(found);
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a boolean",
					args("path", getPath(), "currentElement", jsonObject));
		}
	}

	protected void assertHasStringArrayField(JsonElement jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getStringArray(found);
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not an array of strings",
					args("path", getPath(), "currentElement", found));
		}
	}

	protected void assertHasIntArrayField(JsonElement jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getIntArray(found);
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not an array of Integers",
					args("path", getPath(), "currentElement", found));
		}
	}

	protected void assertHasNumberArrayField(JsonElement jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getNumberArray(found);
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not an array of Numbers",
					args("path", getPath(), "currentElement", found));
		}
	}

	protected String getJsonValueAsString(JsonElement found, String path) {
		String stringValue = "";
		try {
			stringValue = OIDFJSON.getString(found);
		} catch (OIDFJSON.UnexpectedJsonTypeException e) {
			try {
				stringValue = String.valueOf(OIDFJSON.getNumber(found));
			} catch (OIDFJSON.UnexpectedJsonTypeException ex) {
				throw error(String.format("Path %s was not a string or number", path),
					args("path", getPath(), "currentElement", found));
			}
		}
		return stringValue;
	}

	protected String getDoubleValueAsString(JsonElement jsonObject, String path) {
		JsonElement actual = findDoubleByPath(jsonObject, path);
		String stringValue = "";
		try {
			stringValue = String.valueOf(OIDFJSON.getNumber(actual));
		} catch (OIDFJSON.UnexpectedJsonTypeException ex) {
			throw error(String.format("Path %s was not a number", path),
				args("path", getPath(), "currentElement", jsonObject));
		}
		return stringValue;
	}

	protected void assertPatternAndTimeRange(String stringFieldValue, DatetimeField field,
										  JsonElement jsonObject) {
		if (!field.getPattern().isEmpty()) {
			assertRegexMatchesField(stringFieldValue, field.getPath(), field.getPattern());
		}
		if (field.getDaysOlderAccepted() > 0) {
			assertDaysOlderAccepted(stringFieldValue, field.getPath(), field.getDaysOlderAccepted());
		}
		if (field.getSecondsOlderThanSeconds() > 0) {
			JsonElement found = findByPath(jsonObject, field.getSecondsOlderThanString());
			assertSecondsComparison(field.getSecondsOlderThanSeconds(), field.getPath(), stringFieldValue, getJsonValueAsString(found, field.getSecondsOlderThanString()));
		}
	}

	protected void assertPatternAndMaxMinLength(String stringFieldValue, Field field) {
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

	protected void assertCurrencyNotNa(String fieldValue, Field field){
		if(fieldValue.equalsIgnoreCase("NA")) {
			throw error(ErrorMessagesUtils.createCurrencyNotNaMessage(field.getPath(), getApiName()),
					args("path", getPath()));
		}
	}

	protected void assertValueFromEnum(String fieldValue, Set<String> enums, String path) {
		if (!enums.contains(fieldValue)) {
			throw error(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage(path, getApiName()),
				args( "path", getPath(), "value", fieldValue, "enums", enums));
		}
	}

	protected void assertMinAndMaxItems(JsonArray array, Field field) {
		if (array.size() < field.getMinItems()) {
			throw error(ErrorMessagesUtils.createArrayIsLessThanMaxItemsMessage(field.getPath(), getApiName()),
				args("path", getPath(), "required MinItems", field.getMinItems()));
		}

		if (field.getMaxItems() != 0 && array.size() > field.getMaxItems()) {
			throw error(ErrorMessagesUtils.createArrayIsMoreThanMaxItemsMessage(field.getPath(), getApiName()),
				args("path", getPath(), "required MaxItems", field.getMaxItems()));
		}
	}

	private void assertRegexMatchesField(String value, String path, String pattern) {
		Match match = RegexMatch.regex(pattern);
		if (!match.matches(value)) {
			throw error(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage(path, getApiName()),
				args("path", getPath(), "value", value, "pattern", pattern));
		}
	}

	private void assertMaxLength(String stringValue, String path, int maxLength) {
		if (stringValue.length() > maxLength) {
			throw error(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage(path, getApiName()),
				args("path", getPath(), "value", stringValue, "required MaxLength", maxLength));
		}
	}

	private void assertMinLength(String stringValue, String path, int minLength) {
		if (stringValue.length() < minLength) {
			throw error(ErrorMessagesUtils.createFieldValueIsLessThanMinLengthMessage(path, getApiName()),
				args("path", getPath(), "value", stringValue, "required MinLength", minLength));
		}
	}

	protected void assertMinAndMaxValue(String value, Field field) {
		if (field.getMinValue() != null && new BigDecimal(value).compareTo(field.getMinValue()) < 0) {
			throw error(ErrorMessagesUtils.createFieldValueIsLessThanMinimum(field.getPath(), getApiName()),
				args("value", value, "path", getPath(), "required MinValue", field.getMinValue()));

		}
		if (field.getMaxValue() != null && new BigDecimal(value).compareTo(field.getMaxValue()) > 0) {
			throw error(ErrorMessagesUtils.createFieldValueIsMoreThanMaximum(field.getPath(), getApiName()),
				args("path", getPath(), "value", value, "required MaxValue", field.getMaxValue()));
		}
	}

	private void assertDaysOlderAccepted(String stringValue, String path, int daysOlderAccepted) {
		if (Instant.parse(stringValue).isAfter(Instant.now().plus(daysOlderAccepted, ChronoUnit.DAYS))) {
			throw error(ErrorMessagesUtils.createFieldValueIsOlderThanLimit(path, getApiName()),
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
			throw error(ErrorMessagesUtils.createFieldIsntInSecondsRange(path, getApiName()));
		}
	}

	protected void assertLatitude(JsonElement found, Field doubleField) {
		try {
			String rawValue = OIDFJSON.getString(found);
			double latitude = Double.parseDouble(rawValue);
			if (latitude > 90.0 || latitude < -90.0) {
				throw error(ErrorMessagesUtils.createCoordinateIsNotWithinAllowedAreaMessage(doubleField.getPath(),
							getApiName()), args("path", getPath(), "value", rawValue));
			}
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error("Field at " + doubleField.getPath() + " was not a string",
				args("path", getPath(), "currentElement", found));
		} catch (NumberFormatException nfe) {
			throw error("Field at " + doubleField.getPath() + " could not be parsed to a double",
				args("path", getPath(), "currentElement", found));
		}
	}

	protected void assertLongitude(JsonElement found, Field doubleField) {
		try {
			String rawValue = OIDFJSON.getString(found);
			double Longitude = Double.parseDouble(rawValue);
			if (Longitude > 180.0 || Longitude < -180.0) {
				throw error(ErrorMessagesUtils.createCoordinateIsNotWithinAllowedAreaMessage(doubleField.getPath(),
						getApiName()), args("path", getPath(), "value", rawValue));
			}
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error("Field at " + doubleField.getPath() + " was not a string",
					args("path", getPath(), "currentElement", found));
		} catch (NumberFormatException nfe) {
			throw error("Field at " + doubleField.getPath() + " could not be parsed to a double", args(
				"path", getPath(), "currentElement", found));
		}
	}

	protected JsonElement findByPath(JsonElement jsonObject, String path) {
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
			throw error(ErrorMessagesUtils.createElementNotFoundMessage(path, getApiName()),
					args("path", getPath(), "currentElement", jsonObject));
		}
	}

	private void logElementFound(String elementName) {
		if (!logOnlyFailure) {
			logSuccess(ErrorMessagesUtils.createElementFoundMessage(elementName, getApiName()));
		}
	}

	private void logQuerying(String elementName) {
		if (!logOnlyFailure) {
			log(ErrorMessagesUtils.createQueryMessage(elementName, getApiName()));
		}
	}

	private JsonElement findDoubleByPath(JsonElement jsonObject, String path) {
		JsonObject doubleObject = jsonObject.getAsJsonObject();
		if (doubleObject.has(path)) {
			JsonElement element = doubleObject.get(path);
			return element;
		} else {
			throw error(ErrorMessagesUtils.createElementNotFoundMessage(path, getApiName()),
					args("path", getPath(), "currentElement", jsonObject));
		}
	}

	protected String getPath() {
		return this.parentPath + this.currentField;
	}

	public final String getApiName() {
		Class<?> clazz = getClass();
		ApiName apiName = clazz.getDeclaredAnnotation(ApiName.class);
		return apiName == null ? clazz.getSimpleName() : apiName.value();
	}
}
