package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AddAudToRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddAudToRequestObject cond;

	private String issuer = "https://fapidev-as.authlete.net/";

	@BeforeEach
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

		cond.execute(env);

		assertThat(env.getObject("request_object_claims").has("aud")).isTrue();

		assertThat(env.getString("request_object_claims", "aud")).isEqualTo(issuer);

	}

}
