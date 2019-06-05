package io.fintechlabs.testframework.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureResourceAssertionTypeIsJwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject resourceAssertion;

	private String assertionType;

	private EnsureResourceAssertionTypeIsJwt cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureResourceAssertionTypeIsJwt();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		assertionType = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

		resourceAssertion = new JsonParser().parse("{\n" +
				"\"assertion_type\": \"" + assertionType + "\"" +
			"}").getAsJsonObject();


	}

	@Test
	public void testEvaluate() {
		env.putObject("resource_assertion", resourceAssertion);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_noAssertionType() {

		resourceAssertion.remove("assertion_type");

		env.putObject("resource_assertion", resourceAssertion);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_blankAssertionType() {

		resourceAssertion.addProperty("assertion_type", "");

		env.putObject("resource_assertion", resourceAssertion);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_nullAssertionType() {

		resourceAssertion.add("assertion_type", JsonNull.INSTANCE);

		env.putObject("resource_assertion", resourceAssertion);

		cond.evaluate(env);

	}

}
