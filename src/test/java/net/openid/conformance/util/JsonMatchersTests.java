package net.openid.conformance.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import static net.openid.conformance.util.JsonMatchers.isString;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonMatchersTests {

	@Test
	public void assertsJsonStringMatches() {

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("key", "value");

		assertThat(jsonObject.get("key"), isString("value"));

	}

	@Test
	public void assertWrongStringFails() {

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("key", "value");

		JsonMatchers.JsonStringMatcher matcher = isString("other");
		assertThat(matcher.matchesSafely(jsonObject.get("key")), Matchers.is(false));

	}

	@Test
	public void assertNotStringFails() {

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("key", 11);

		JsonMatchers.JsonStringMatcher matcher = isString("other");
		assertThat(matcher.matchesSafely(jsonObject.get("key")), Matchers.is(false));

	}

	@Test
	public void assertNotPresentFails() {

		JsonObject jsonObject = new JsonObject();

		JsonMatchers.JsonStringMatcher matcher = isString("other");
		assertThat(matcher.matchesSafely(jsonObject.get("key")), Matchers.is(false));

	}

}
