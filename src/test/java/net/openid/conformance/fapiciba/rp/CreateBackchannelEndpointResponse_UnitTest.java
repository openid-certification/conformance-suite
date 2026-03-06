package net.openid.conformance.fapiciba.rp;

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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class CreateBackchannelEndpointResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateBackchannelEndpointResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new CreateBackchannelEndpointResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("backchannel_endpoint_http_request", new JsonObject());
		env.putObject("backchannel_request_object", new JsonObject());
	}

	@Test
	public void testEvaluate_usesDefaultExpiresInWhenNoOverridesPresent() {
		cond.execute(env);
		assertEquals(CreateBackchannelEndpointResponse.EXPIRES_IN, env.getInteger("backchannel_endpoint_response", "expires_in"));
	}

	@Test
	public void testEvaluate_usesRequestedExpiryWhenConsentExpirationNotPresent() {
		env.putInteger("requested_expiry", 30);
		cond.execute(env);
		assertEquals(30, env.getInteger("backchannel_endpoint_response", "expires_in"));
	}

	@Test
	public void testEvaluate_usesConsentExpirationWhenPresent() {
		JsonObject consentResponse = new JsonObject();
		JsonObject data = new JsonObject();
		data.addProperty("expirationDateTime", Instant.now().plus(120, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS).toString());
		consentResponse.add("data", data);
		env.putObject("consent_response", consentResponse);
		env.putInteger("requested_expiry", 30);

		cond.execute(env);

		int expiresIn = env.getInteger("backchannel_endpoint_response", "expires_in");
		assertTrue(expiresIn <= 120 && expiresIn >= 100, "expires_in should be based on consent expiration");
	}

	@Test
	public void testEvaluate_clampsToOneSecondWhenConsentAlreadyExpired() {
		JsonObject consentResponse = new JsonObject();
		JsonObject data = new JsonObject();
		data.addProperty("expirationDateTime", Instant.now().minus(5, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS).toString());
		consentResponse.add("data", data);
		env.putObject("consent_response", consentResponse);

		cond.execute(env);

		assertEquals(1, env.getInteger("backchannel_endpoint_response", "expires_in"));
	}
}
