package net.openid.conformance.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.validation.Match;
import net.openid.conformance.validation.RegexMatch;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class GsonTests {

	@Test
	public void ensureGsonParserCanHandleDoublesProperly() {

		Set<String> doubles = Set.of(
			"1",
			"385.5",
			"999948448.483",
			"123456789012345.1234",
			"999999999999999.9999"
		);
		Gson gson = JsonUtils.createBigDecimalAwareGson();

		DoubleField doubleField = new DoubleField.Builder("").build();

		for(String s: doubles) {
			String json = String.format("{\"doubleValue\": %s}", s);
			JsonObject entity = gson.fromJson(json, JsonObject.class);
			JsonElement element = entity.get("doubleValue");
			String found = String.valueOf(OIDFJSON.getNumber(element));
			assertEquals(s, found);

			Match regex = RegexMatch.regex(doubleField.getPattern());
			assertThat(regex.matches(found));
		}

	}

}
