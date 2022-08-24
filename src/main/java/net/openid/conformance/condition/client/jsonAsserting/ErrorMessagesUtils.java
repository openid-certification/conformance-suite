package net.openid.conformance.condition.client.jsonAsserting;

public class ErrorMessagesUtils {

	public static String createObjectClassCastExpMessage(String elementName, String apiName) {
		return String.format("Class cast exception, expect JsonObject, but found JsonArray. " +
			"Field: %s on the %s API response",	elementName, apiName);
	}

	public static String createArrayClassCastExpMessage(String elementName, String apiName) {
		return String.format("Class cast exception, expect JsonArray, but found JsonObject. " +
			"Field: %s on the %s API response",	elementName, apiName);
	}

	public static String createArrayMustNotBeEmptyMessage(String elementName, String apiName) {
		return String.format("Json Array must NOT be Empty." +
				"Field: %s on the %s API response",	elementName, apiName);
	}

	public static String createObjectLessRequiredMinProperties(String elementName, String apiName) {
		return String.format("Json Object: %s has less properties then required minProperties on the %s API response",	elementName, apiName);
	}

	public static String createQueryMessage(String elementName, String apiName) {
		return String.format("Looking up %s on the %s API response", elementName, apiName);
	}
	public static String createTotalElementsFoundMessage(int totalElements, String apiName) {
		return String.format("Successfully validated %d elements on the %s API response",
			totalElements, apiName);
	}
	public static String createTotalErrorsFoundMessage(int totalElements, String apiName) {
		return String.format("Found %d errors on the %s API response", totalElements, apiName);
	}
	public static String createElementCantBeNullMessage(String elementName, String apiName) {
		return String.format("Field %s cant be null on the %s API response", elementName, apiName);
	}

	public static String createElementFoundMessage(String elementName, String apiName) {
		return String.format("The %s element is present in the %s API response", elementName, apiName);
	}

	public static String createElementNotFoundMessage(String elementName, String apiName) {
		return String.format("Unable to find element %s on the %s API response", elementName, apiName);
	}

	public static String createCurrencyNotNaMessage(String elementName, String apiName) {
		return String.format("Value from element %s doesn't match the required pattern on the %s API response.\nThis is a known issue, please view this link for orientation on this issue: https://openbanking-brasil.github.io/areadesenvolvedor/#problemas-conhecidos-da-especificacao", elementName, apiName);
	}

	public static String createFieldValueNotMatchPatternMessage(String elementName, String apiName) {
		return String.format("Value from element %s doesn't match the required pattern on the %s API response",
			elementName, apiName);
	}

	public static String createFieldValueNotMatchEnumerationMessage(String elementName, String apiName) {
		return String.format("Value from element %s does not match any given enumeration on the " +
			"%s API response", elementName, apiName);
	}

	public static String createFieldValueIsMoreThanMaxLengthMessage(String elementName, String apiName) {
		return String.format("Value from element %s is more than the required maxLength on the " +
			"%s API response", elementName, apiName);
	}

	public static String createArrayIsMoreThanMaxItemsMessage(String elementName, String apiName) {
		return String.format("Array from element %s is more than the required maxItems on the " +
			"%s API response", elementName, apiName);
	}

	public static String createArrayIsLessThanMaxItemsMessage(String elementName, String apiName) {
		return String.format("Array from element %s is less than the required minItems on the " +
			"%s API response", elementName, apiName);
	}

	public static String createFieldValueIsLessThanMinLengthMessage(String elementName, String apiName) {
		return String.format("Value from element %s is less than the required minLength " +
			"on the %s API response", elementName, apiName);
	}

	public static String createFieldValueIsMoreThanMaximum(String elementName, String apiName) {
		return String.format("Value from element %s is more than the required maximum " +
			"on the %s API response", elementName, apiName);
	}

	public static String createFieldValueIsOlderThanLimit(String elementName, String apiName) {
		return String.format("Value from element %s is a date older then the required limit " +
			"on the %s API response", elementName, apiName);
	}

	public static String createFieldIsntInSecondsRange(String elementName, String apiName) {
		return String.format("Value from element %s is older or younger then the required limit " +
			"on the %s API response", elementName, apiName);
	}

	public static String createFieldValueIsLessThanMinimum(String elementName, String apiName) {
		return String.format("Value from element %s is less than the required minimum " +
			"on the %s API response", elementName, apiName);
	}

	public static String createCoordinateIsNotWithinAllowedAreaMessage(String elementName, String apiName) {
		return String.format("The %s does not enter to coordinate area. " +
			"It is not latitude or longitude", elementName, apiName);
	}
}
