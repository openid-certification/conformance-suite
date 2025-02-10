package net.openid.conformance.testmodule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Wrappers around the GSON getAsXXXX methods
 *
 * These should generally not be called directly from TestConditions (although there are a lot of historical uses)
 * as the errors they end up providing to the user are at best unhelpful - instead use AbstractCondition's
 * getStringFromEnvironment and similar.
 *
 * The 'getAs' methods automatically coerce types, for example if 'getAsNumber' finds a string, it will automatically
 * convert it to a number. This is not desirable behaviour when we're trying to write a conformance suite that
 * checks if the returned values are actually the correct type (for example it's pretty wrong to return 'expires_in'
 * as a string, it should always be a number.
 *
 * The 'getAs' methods should never be directly used in our code, these wrappers should always be used.
 *
 * 'get' (or 'getAs') methods in this class must NEVER do any type conversion; if type conversion is necessary call
 * the method 'forceConversionTo...'
 *
 * See https://gitlab.com/openid/conformance-suite/issues/398
 */
public final class OIDFJSON {

	public static Number getNumber(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new UnexpectedJsonTypeException("getNumber called on something that is not a number: " + json);
		}
		return json.getAsNumber();
	}

	public static byte getByte(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new UnexpectedJsonTypeException("getByte called on something that is not a number: " + json);
		}
		return json.getAsByte();
	}

	public static short getShort(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new UnexpectedJsonTypeException("getShort called on something that is not a number: " + json);
		}
		return json.getAsShort();
	}

	public static int getInt(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new UnexpectedJsonTypeException("getInt called on something that is not a number: " + json);
		}
		return json.getAsInt();
	}

	public static float getFloat(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new UnexpectedJsonTypeException("getFloat called on something that is not a number: " + json);
		}
		return json.getAsFloat();
	}

	public static double getDouble(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new UnexpectedJsonTypeException("getDouble called on something that is not a number: " + json);
		}
		return json.getAsDouble();
	}

	public static long getLong(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new UnexpectedJsonTypeException("getLong called on something that is not a number: " + json);
		}
		return json.getAsLong();
	}

	public static String getString(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
			throw new UnexpectedJsonTypeException("getString called on something that is not a string: " + json);
		}
		return json.getAsString();
	}

	public static boolean getBoolean(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isBoolean()) {
			throw new UnexpectedJsonTypeException("getBoolean called on something that is not a boolean: " + json);
		}
		return json.getAsBoolean();
	}

	public static String forceConversionToString(JsonElement json) {
		if (!json.isJsonPrimitive() || (!json.getAsJsonPrimitive().isNumber() && !json.getAsJsonPrimitive().isString())) {
			// I'm not 100% sure if bool/object conversions should be blocked; I suspect if we ever find a reason to
			// allow them then it's fine to do so, it's just not a path the current code uses.
			throw new UnexpectedJsonTypeException("forceConversionToString called on something that is neither a number nor a string: " + json);
		}

		return json.getAsString();
	}

	/**
	 * Uses JsonElement.getAsNumber() which will automatically convert to number
	 * as long as it is not JsonNull.
	 * Unlike getNumber, it will not throw an error if it's a json string
	 * @param json
	 * @return
	 * @throws ValueIsJsonNullException
	 */
	public static Number forceConversionToNumber(JsonElement json) throws ValueIsJsonNullException {
		if(json.isJsonNull()) {
			throw new ValueIsJsonNullException("Element has a JsonNull value");
		}
		if (!json.isJsonPrimitive() || (!json.getAsJsonPrimitive().isNumber() && !json.getAsJsonPrimitive().isString())) {
			throw new UnexpectedJsonTypeException("forceConversionToNumber called on something that is neither a number nor a string: " + json);
		}
		return json.getAsNumber();
	}

	public static boolean isNull(JsonElement jsonElement) {
		return jsonElement == null || jsonElement.isJsonNull();
	}

	/**
	 * Thrown if the value is JsonNull
	 */
	@SuppressWarnings("serial")
	public static class ValueIsJsonNullException extends Exception {
		public ValueIsJsonNullException(String msg) {
			super(msg);
		}
	}

	/**
	 * To allow conditions catch these exceptions when necessary
	 * i.e to catch and throw a nicer 'error(..., args(...))' from a condition
	 */
	@SuppressWarnings("serial")
	public static class UnexpectedJsonTypeException extends RuntimeException {
		public UnexpectedJsonTypeException(String msg) {
			super(msg);
		}
	}

	public static <T> JsonArray convertListToJsonArray(List<T> list, Function<T, JsonElement> converter) {
		JsonArray jsonArray = new JsonArray();
		for (T item : list) {
			jsonArray.add(converter.apply(item));
		}
		return jsonArray;
	}

	public static <T> List<T> convertJsonArrayToList(JsonArray jsonArray, Function<JsonElement, T> converter) {
		List<T> list = new ArrayList<>();
		for (JsonElement item : jsonArray) {
			list.add(converter.apply(item));
		}
		return list;
	}

	public static JsonArray convertListToJsonArray(List<String> list) {
		return convertListToJsonArray(list, JsonPrimitive::new);
	}

	public static List<String> convertJsonArrayToList(JsonArray jsonArray) {
		return convertJsonArrayToList(jsonArray, OIDFJSON::getString);
	}
}
