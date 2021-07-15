package net.openid.conformance.apis;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.account.AccountListValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.UseResurce;
import net.openid.conformance.validation.Match;
import net.openid.conformance.validation.RegexMatch;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Date;
import java.util.regex.Pattern;

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

	@Test
	public void assertsRegexes() {

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {
			@Override
			public Environment evaluate(Environment environment) {
				assertJsonField(bodyFrom(environment), "$.fixedString", RegexMatch.regex("^[a-z0-9]+$"));
				assertJsonField(bodyFrom(environment), "$.fixedNumber", RegexMatch.regex("^\\d{4,20}$|^NA$"));
				return environment;
			}
		};

		run(condition);

	}

	@Test(expected = AssertionError.class)
	public void regexFailsIfNoMatch() {

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {
			@Override
			public Environment evaluate(Environment environment) {
				assertJsonField(bodyFrom(environment), "$.fixedString", RegexMatch.regex("^MATCH$"));
				return environment;
			}
		};

		run(condition);

	}

	@Test(expected = AssertionError.class)
	public void regexFailsIfNotNumberOrString() {

		AbstractJsonAssertingCondition condition = new AbstractJsonAssertingCondition() {
			@Override
			public Environment evaluate(Environment environment) {
				assertJsonField(bodyFrom(environment), "$.bool", RegexMatch.regex("^[a-z0-9]+$"));
				return environment;
			}
		};

		run(condition);

	}

}
