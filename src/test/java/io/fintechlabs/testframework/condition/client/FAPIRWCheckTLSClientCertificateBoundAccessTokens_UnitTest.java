package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FAPIRWCheckTLSClientCertificateBoundAccessTokens_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIRWCheckTLSClientCertificateBoundAccessTokens cond;

	@Before
	public void setUp() throws Exception {
		cond = new FAPIRWCheckTLSClientCertificateBoundAccessTokens();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseNull() {
		env.putObject("server", new JsonObject());

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseJson() {
		JsonObject server = new JsonParser().parse("{\"tls_client_certificate_bound_access_tokens\":{\"value\": true}}").getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseString() {
		JsonObject server = new JsonParser().parse("{\"tls_client_certificate_bound_access_tokens\":\"true\"}").getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseGood() {
		JsonObject server = new JsonParser().parse("{\"tls_client_certificate_bound_access_tokens\":true}").getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);
	}
}
