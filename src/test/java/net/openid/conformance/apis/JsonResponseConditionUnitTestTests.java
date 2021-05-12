package net.openid.conformance.apis;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.UseResurce;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.*;

@UseResurce("verySimpleJsonDoc.json")
public class JsonResponseConditionUnitTestTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void loadsDocumentFromClasspath() {

		assertNotNull(jsonObject);
		assertThat(OIDFJSON.getString(jsonObject.get("message")), Matchers.is("This is a simple json document"));

	}

	@Test(expected = AssertionError.class)
	@UseResurce("nonexistent.json")
	public void failsGracefullyIfDocumentNotPresent() {

		assertNull(jsonObject);

	}

	@Test
	public void appliesCondition() {

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {
			@Override
			public Environment evaluate(Environment environment) {
				assertJsonField(bodyFrom(environment), "$.message", "This is a simple json document");
				return environment;
			}
		};

		run(condition);

	}

	@Test
	public void setsResponseHeaders() {

		setHeaders("X-Fake-Header", "present");

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {
			@Override
			public Environment evaluate(Environment environment) {
				JsonObject headers = headersFrom(environment);
				assertJsonField(headers, "X-Fake-Header", "present");
				return environment;
			}
		};

		run(condition);

	}

	@Test
	public void setsResponseHeadersMultiValue() {

		setHeaders("X-Fake-Header", "present", "forsure");

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {
			@Override
			public Environment evaluate(Environment environment) {
				JsonObject headers = headersFrom(environment);
				assertJsonField(headers, "X-Fake-Header", "present", "forsure");
				return environment;
			}
		};

		run(condition);

	}

	@Test
	public void setsResponseStatus() {

		setStatus(502);

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {
			@Override
			public Environment evaluate(Environment environment) {
				assertStatus(502, environment);
				return environment;
			}
		};

		run(condition);

	}

	@Test(expected = AssertionError.class)
	public void failsTestIfConditionFails() {

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {
			@Override
			public Environment evaluate(Environment environment) {
				throw error("test error");
			}
		};

		run(condition);

	}

	@Test
	public void failsTestIfConditionFailsAndGetError() {

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {
			@Override
			public Environment evaluate(Environment environment) {
				throw error("test error");
			}
		};

		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), Matchers.containsString("test error"));

	}

	@Test(expected = AssertionError.class)
	public void failsTestIfConditionUnexpectedlyPasses() {

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {
			@Override
			public Environment evaluate(Environment environment) {
				return environment;
			};
		};

		runAndFail(condition);

	}

}
