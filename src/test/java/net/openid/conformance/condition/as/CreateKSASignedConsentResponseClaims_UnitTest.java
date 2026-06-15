package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(MockitoExtension.class)
public class CreateKSASignedConsentResponseClaims_UnitTest {

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateKSASignedConsentResponseClaims cond;
	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new CreateKSASignedConsentResponseClaims();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();

		JsonObject data = new JsonObject();
		data.addProperty("ConsentId", "aac-123");
		JsonObject response = new JsonObject();
		response.add("Data", data);
		env.putObject("account_request_response", response);

		JsonObject server = new JsonObject();
		server.addProperty("issuer", "https://suite.example/issuer");
		env.putObject("server", server);
	}

	@Test
	public void testWrapsResponseInMessage() {
		env = cond.evaluate(env);

		JsonObject claims = env.getObject("consent_response");
		assertThat(OIDFJSON.getString(claims.get("iss")), is("https://suite.example/issuer"));
		assertThat(claims.has("iat"), is(true));
		assertThat(OIDFJSON.getString(claims.getAsJsonObject("message").getAsJsonObject("Data").get("ConsentId")), is("aac-123"));
	}
}
