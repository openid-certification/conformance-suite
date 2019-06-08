package io.fintechlabs.testframework.testmodule;

import com.google.gson.JsonElement;

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
			throw new RuntimeException("getNumber called on something that is not a number: " + json);
		}
		return json.getAsNumber();
	}

	public static byte getByte(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new RuntimeException("getByte called on something that is not a number: " + json);
		}
		return json.getAsByte();
	}

	public static short getShort(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new RuntimeException("getShort called on something that is not a number: " + json);
		}
		return json.getAsShort();
	}

	public static int getInt(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new RuntimeException("getInt called on something that is not a number: " + json);
		}
		return json.getAsInt();
	}

	public static float getFloat(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new RuntimeException("getFloat called on something that is not a number: " + json);
		}
		return json.getAsFloat();
	}

	public static double getDouble(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new RuntimeException("getDouble called on something that is not a number: " + json);
		}
		return json.getAsDouble();
	}

	public static long getLong(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
			throw new RuntimeException("getLong called on something that is not a number: " + json);
		}
		return json.getAsLong();
	}

	public static char getCharacter(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
			throw new RuntimeException("getCharacter called on something that is not a string: " + json);
		}
		return json.getAsCharacter();
	}

	public static String getString(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
			throw new RuntimeException("getString called on something that is not a string: " + json);
		}
		return json.getAsString();
	}

	public static boolean getBoolean(JsonElement json) {
		if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isBoolean()) {
			throw new RuntimeException("getBoolean called on something that is not a boolean: " + json);
		}
		return json.getAsBoolean();
	}

	public static String forceConversionToString(JsonElement json) {

		return json.getAsString();
	}
}
