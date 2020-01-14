package net.openid.conformance.testmodule;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class OIDFJSON_UnitTest {

	@Test
	public void getFloat_number() {
		JsonElement json = new JsonParser().parse("5.1");

		float f = OIDFJSON.getFloat(json);

		assert(f > 5.09 && f < 5.11);
	}

	@Test(expected = OIDFJSON.UnexpectedJsonTypeException.class)
	public void getFloat_string() {
		JsonElement json = new JsonParser().parse("\"5.1\"");

		float f = OIDFJSON.getFloat(json);
	}

	@Test
	public void forceConversionToString_number() {
		JsonElement json = new JsonParser().parse("5.1");

		String s = OIDFJSON.forceConversionToString(json);

		assertNotNull(s);
	}

	@Test
	public void forceConversionToString_string() {
		JsonElement json = new JsonParser().parse("\"flibble\"");

		String s = OIDFJSON.forceConversionToString(json);

		assertNotNull(s);
	}

	@Test(expected = OIDFJSON.UnexpectedJsonTypeException.class)
	public void forceConversionToString_bool() {
		JsonElement json = new JsonParser().parse("true");

		String s = OIDFJSON.forceConversionToString(json);
	}

	@Test(expected = OIDFJSON.UnexpectedJsonTypeException.class)
	public void forceConversionToString_object() {
		JsonElement json = new JsonParser().parse("{ \"foo\": 5 }");

		String s = OIDFJSON.forceConversionToString(json);
	}

	@Test(expected = OIDFJSON.UnexpectedJsonTypeException.class)
	public void forceConversionToString_array() {
		JsonElement json = new JsonParser().parse("[ 5 ]");

		String s = OIDFJSON.forceConversionToString(json);
	}

	@Test(expected = OIDFJSON.UnexpectedJsonTypeException.class)
	public void forceConversionToString_null() {
		JsonElement json = new JsonParser().parse("null");

		String s = OIDFJSON.forceConversionToString(json);
	}
}
