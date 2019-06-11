package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AddAudToRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddAudToRequestObject cond;

	private String issuer = "https://fapidev-as.authlete.net/";

	@Before
	public void setUp() throws Exception {
		cond = new AddAudToRequestObject();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject serverIssuer = new JsonObject();

		serverIssuer.addProperty("issuer", issuer);

		env.putObject("server", serverIssuer);
	}

	@Test
	public void testEvaluate_presentAudValueAndEqualIssuer() {

		JsonObject requestObjectClaims = new JsonObject();

		env.putObject("request_object_claims", requestObjectClaims);

		cond.evaluate(env);

		assertThat(env.getObject("request_object_claims").has("aud")).isTrue();

		assertThat(env.getString("request_object_claims", "aud")).isEqualTo(issuer);

	}

}
