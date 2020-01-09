package net.openid.conformance.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

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

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_noAssertionType() {

		resourceAssertion.remove("assertion_type");

		env.putObject("resource_assertion", resourceAssertion);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_blankAssertionType() {

		resourceAssertion.addProperty("assertion_type", "");

		env.putObject("resource_assertion", resourceAssertion);

		cond.execute(env);

	}

	@Test(expected = IllegalArgumentException.class)
	public void testEvaluate_nullAssertionType() {

		resourceAssertion.add("assertion_type", JsonNull.INSTANCE);

		env.putObject("resource_assertion", resourceAssertion);

		cond.execute(env);

	}

}
