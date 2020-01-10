package net.openid.conformance.testmodule;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Wrappers around the GSON getAsXXXX methods
 *
 * The 'getAs' methods automatically coerce types, for example if 'getAsNumber' finds a string, it will automatically
 * convert it to a number. This is not desirable behaviour when we're trying to write a conformance suite that
 * checks if the returned values are actually the correct type (for example it's pretty wrong to return 'expires_in'
 * as a string, it should always be a number.
 *
 * The 'getAs' methods should never be directly used in our code, these wrappers should always be used.
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

	public static Number getNumberIfNotJsonNull(JsonElement json) throws ValueIsJsonNullException {
		if(json.isJsonNull()) {
			throw new ValueIsJsonNullException("Element has a JsonNull value");
		}
		return OIDFJSON.getNumber(json);
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

	public static char getCharacter(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
			throw new UnexpectedJsonTypeException("getCharacter called on something that is not a string: " + json);
		}
		return json.getAsCharacter();
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

		return json.getAsString();
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
}
