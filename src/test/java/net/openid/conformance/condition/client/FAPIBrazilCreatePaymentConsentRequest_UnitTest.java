package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilCreatePaymentConsentRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIBrazilCreatePaymentConsentRequest condition;

	@Test
	public void putsRequestInEnvironment() {

		JsonObject resourceConfig = new JsonObject();
		JsonObject consentConfig = new JsonObject();
		JsonObject data = new JsonObject();
		consentConfig.add("data", data);
		data.add("loggedUser", new JsonObject());
		data.add("creditor", new JsonObject());
		data.add("payment", new JsonObject());

		resourceConfig.add("brazilPaymentConsent", consentConfig);
		env.putObject("resource", resourceConfig);
		env.putObject("config", new JsonObject());

		condition.evaluate(env);

		JsonObject consentEndpointRequest = env.getObject("consent_endpoint_request");

		assertNotNull(consentEndpointRequest);

	}

	@Test
	public void failsIfConsentConfigNotPresent() {

		JsonObject resourceConfig = new JsonObject();

		env.putObject("resource", resourceConfig);
		env.putObject("config", new JsonObject());

		ConditionError ce = tryAndFail(env);

		assertThat(ce.getMessage(), equalTo("FAPIBrazilCreatePaymentConsentRequest: As 'payments' is included in the 'scope' within the test configuration, a payment consent request JSON object must also be provided in the test configuration."));

	}

	@Test
	public void mustHaveLoggedUser() {

		JsonObject resourceConfig = new JsonObject();
		JsonObject consentConfig = new JsonObject();
		JsonObject data = new JsonObject();
		consentConfig.add("data", data);

		resourceConfig.add("brazilPaymentConsent", consentConfig);
		env.putObject("resource", resourceConfig);
		env.putObject("config", new JsonObject());

		ConditionError ce = tryAndFail(env);

		assertThat(ce.getMessage(), equalTo("FAPIBrazilCreatePaymentConsentRequest: Consent object must have loggedUser field"));

	}

	@Test
	public void mustHaveCreditor() {

		JsonObject resourceConfig = new JsonObject();
		JsonObject consentConfig = new JsonObject();
		JsonObject data = new JsonObject();
		consentConfig.add("data", data);
		data.add("loggedUser", new JsonObject());

		resourceConfig.add("brazilPaymentConsent", consentConfig);
		env.putObject("resource", resourceConfig);
		env.putObject("config", new JsonObject());

		ConditionError ce = tryAndFail(env);

		assertThat(ce.getMessage(), equalTo("FAPIBrazilCreatePaymentConsentRequest: Consent object must have creditor field"));

	}

	@Test
	public void mustHavePayment() {

		JsonObject resourceConfig = new JsonObject();
		JsonObject consentConfig = new JsonObject();
		JsonObject data = new JsonObject();
		consentConfig.add("data", data);
		data.add("loggedUser", new JsonObject());
		data.add("creditor", new JsonObject());

		resourceConfig.add("brazilPaymentConsent", consentConfig);
		env.putObject("resource", resourceConfig);
		env.putObject("config", new JsonObject());

		ConditionError ce = tryAndFail(env);

		assertThat(ce.getMessage(), equalTo("FAPIBrazilCreatePaymentConsentRequest: Consent object must have payment field"));

	}

	private ConditionError tryAndFail(Environment env) {
		try {
			condition.execute(env);
		} catch(ConditionError ce) {
			return ce;
		}
		fail("Condition was not expected to be successful");
		return null;
	}

	@BeforeEach
	public void setUp() throws Exception {
		condition = new FAPIBrazilCreatePaymentConsentRequest();

		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

}
