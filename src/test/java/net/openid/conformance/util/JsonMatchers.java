package net.openid.conformance.util;

import com.google.gson.JsonElement;
import net.openid.conformance.testmodule.OIDFJSON;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class JsonMatchers {

	public static JsonStringMatcher isString(String expected) {
		return new JsonStringMatcher(expected);
	}

	static class JsonStringMatcher extends TypeSafeMatcher<JsonElement> {

		private final String expected;

		public JsonStringMatcher(String expected) {
			this.expected = expected;
		}

		@Override
		protected boolean matchesSafely(JsonElement element) {
			if(element == null) {
				return false;
			}
			try {
				return expected.equals(OIDFJSON.getString(element));
			} catch(OIDFJSON.UnexpectedJsonTypeException e) {
				return false;
			}
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("Expecting " + expected);
		}
	}

}
