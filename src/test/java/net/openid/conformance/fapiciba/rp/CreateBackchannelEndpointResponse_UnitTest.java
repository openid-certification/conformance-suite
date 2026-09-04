package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.fapiciba.OpenBankingBrazilCibaProfileConstants;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class CreateBackchannelEndpointResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateBackchannelEndpointResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new CreateBackchannelEndpointResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("backchannel_endpoint_http_request", new JsonObject());
		env.putObject("backchannel_request_object", new JsonObject());
		JsonObject config = new JsonObject();
		config.add("client", new JsonObject());
		env.putObject("config", config);
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
	public void setsProfileMaximumAsInteger() {
		SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry setter =
			new SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry();
		setter.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		setter.execute(env);

		assertEquals(86_400,
			env.getInteger(SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry.ENVIRONMENT_KEY));
	}

	@Test
	public void setsConfiguredProfileMaximum() {
		JsonObject client = new JsonObject();
		client.addProperty("brazil_ciba_maximum_expiry", "3600");
		JsonObject config = new JsonObject();
		config.add("client", client);
		env.putObject("config", config);
		SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry setter =
			new SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry();
		setter.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		setter.execute(env);

		assertEquals(3_600,
			env.getInteger(SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry.ENVIRONMENT_KEY));
	}

	@Test
	public void usesProfileMaximumWhenRequestedExpiryIsAbsent() {
		env.putInteger(SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry.ENVIRONMENT_KEY,
			OpenBankingBrazilCibaProfileConstants.DEFAULT_AUTHENTICATION_REQUEST_MAXIMUM_EXPIRY_SECONDS);
		cond.execute(env);
		assertEquals(86_400, env.getInteger("backchannel_endpoint_response", "expires_in"));
	}

	@Test
	public void usesRequestedExpiryWhenItDoesNotExceedProfileMaximum() {
		env.putInteger(SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry.ENVIRONMENT_KEY,
			OpenBankingBrazilCibaProfileConstants.DEFAULT_AUTHENTICATION_REQUEST_MAXIMUM_EXPIRY_SECONDS);
		env.putInteger("requested_expiry", 30);
		cond.execute(env);
		assertEquals(30, env.getInteger("backchannel_endpoint_response", "expires_in"));
	}

	@Test
	public void capsRequestedExpiryAtProfileMaximum() {
		env.putInteger(SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry.ENVIRONMENT_KEY,
			OpenBankingBrazilCibaProfileConstants.DEFAULT_AUTHENTICATION_REQUEST_MAXIMUM_EXPIRY_SECONDS);
		env.putInteger("requested_expiry", 86_401);
		cond.execute(env);
		assertEquals(86_400, env.getInteger("backchannel_endpoint_response", "expires_in"));
	}
}
