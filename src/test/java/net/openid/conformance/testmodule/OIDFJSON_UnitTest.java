package net.openid.conformance.testmodule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("UnusedVariable")
public class OIDFJSON_UnitTest {

	@Test
	public void getFloat_number() {
		JsonElement json = JsonParser.parseString("5.1");

		float f = OIDFJSON.getFloat(json);

		assert(f > 5.09 && f < 5.11);
	}

	@Test
	public void getFloat_string() {
		assertThrows(OIDFJSON.UnexpectedJsonTypeException.class, () -> {
			JsonElement json = JsonParser.parseString("\"5.1\"");

			float f = OIDFJSON.getFloat(json);
		});
	}

	@Test
	public void forceConversionToString_number() {
		JsonElement json = JsonParser.parseString("5.1");

		String s = OIDFJSON.forceConversionToString(json);

		assertNotNull(s);
	}

	@Test
	public void forceConversionToString_string() {
		JsonElement json = JsonParser.parseString("\"flibble\"");

		String s = OIDFJSON.forceConversionToString(json);

		assertNotNull(s);
	}

	@Test
	public void forceConversionToString_bool() {
		assertThrows(OIDFJSON.UnexpectedJsonTypeException.class, () -> {
			JsonElement json = JsonParser.parseString("true");

			String s = OIDFJSON.forceConversionToString(json);
		});
	}

	@Test
	public void forceConversionToString_object() {
		assertThrows(OIDFJSON.UnexpectedJsonTypeException.class, () -> {
			JsonElement json = JsonParser.parseString("{ \"foo\": 5 }");

			String s = OIDFJSON.forceConversionToString(json);
		});
	}

	@Test
	public void forceConversionToString_array() {
		assertThrows(OIDFJSON.UnexpectedJsonTypeException.class, () -> {
			JsonElement json = JsonParser.parseString("[ 5 ]");

			String s = OIDFJSON.forceConversionToString(json);
		});
	}

	@Test
	public void forceConversionToString_null() {
		assertThrows(OIDFJSON.UnexpectedJsonTypeException.class, () -> {
			JsonElement json = JsonParser.parseString("null");

			String s = OIDFJSON.forceConversionToString(json);
		});
	}

	@Test
	public void testConvertJsonArrayToList_throwsExceptionOnNonString() {
		JsonArray jsonArray = new JsonArray();
		jsonArray.add("string1");
		jsonArray.add("string2");
		jsonArray.add(42); // Add a non-string element

		assertThrows(OIDFJSON.UnexpectedJsonTypeException.class, () -> {
			OIDFJSON.convertJsonArrayToList(jsonArray);
		});
	}

}
