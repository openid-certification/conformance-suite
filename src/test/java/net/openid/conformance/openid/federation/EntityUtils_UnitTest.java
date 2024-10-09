package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class EntityUtils_UnitTest {

	@Test
	public void property_order_does_not_matter_when_comparing_json_objects() {
		String jsonStringA = """
				{"prop1":"value1","prop2":"value2","prop3":"value3"}
				""";
		String jsonStringB = """
				{"prop2":"value2","prop3":"value3","prop1":"value1"}
				""";

		JsonElement a = JsonParser.parseString(jsonStringA);
		JsonElement b = JsonParser.parseString(jsonStringB);

		List<String> propertyNames = Arrays.asList("prop1", "prop2", "prop3");

		List<String> differences = EntityUtils.diffEntityStatements(propertyNames, a, b);

		assertTrue(differences.isEmpty());
	}
}
