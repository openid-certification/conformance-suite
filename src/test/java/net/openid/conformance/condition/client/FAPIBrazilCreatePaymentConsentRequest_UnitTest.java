package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(MockitoJUnitRunner.class)
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

		assertThat(ce.getMessage(), equalTo("FAPIBrazilCreatePaymentConsentRequest: A payment consent request JSON object is needed for this configuration"));

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

	@Before
	public void setUp() throws Exception {
		condition = new FAPIBrazilCreatePaymentConsentRequest();

		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

}
