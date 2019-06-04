package io.fintechlabs.testframework.testmodule;

import com.google.gson.JsonElement;

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
