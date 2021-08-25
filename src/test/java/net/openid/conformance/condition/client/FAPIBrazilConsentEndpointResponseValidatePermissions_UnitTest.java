package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import springfox.documentation.spring.web.json.Json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(MockitoJUnitRunner.class)
public class FAPIBrazilConsentEndpointResponseValidatePermissions_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIBrazilConsentEndpointResponseValidatePermissions condition;

	@Before
	public void setUp() throws Exception {
		condition = new FAPIBrazilConsentEndpointResponseValidatePermissions();

		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void success() {
		String response = "{\"data\":{\"consentId\":\"urn:raidiambank:58fff674-fb3a-4e45-bd10-7153d71d176c\",\"creationDateTime\":\"2021-07-13T12:07:47Z\",\"status\":\"AWAITING_AUTHORISATION\",\"statusUpdateDateTime\":\"2021-07-13T12:07:47Z\",\"permissions\":[\"RESOURCES_READ\",\"ACCOUNTS_BALANCES_READ\",\"ACCOUNTS_READ\"],\"expirationDateTime\":\"2021-07-13T14:07:46Z\"},\"links\":{\"self\":\"/\"},\"meta\":{\"totalRecords\":1,\"totalPages\":1,\"requestDateTime\":\"2021-07-13T12:07:47Z\"}}\n";

		JsonObject o = new JsonParser().parse(response).getAsJsonObject();

		env.putObject("consent_endpoint_response", o);

		JsonArray requestPerms = new JsonArray();
		for (String s: new String[] { "RESOURCES_READ", "ACCOUNTS_BALANCES_READ", "ACCOUNTS_READ" }) {
			requestPerms.add(s);
		}
		JsonObject brazil = new JsonObject();
		brazil.add("requested_permissions", requestPerms);

		env.putObject("brazil_consent", brazil);

		condition.execute(env);
	}

	@Test
	public void successDifferentOrder() {
		String response = "{\"data\":{\"consentId\":\"urn:raidiambank:58fff674-fb3a-4e45-bd10-7153d71d176c\",\"creationDateTime\":\"2021-07-13T12:07:47Z\",\"status\":\"AWAITING_AUTHORISATION\",\"statusUpdateDateTime\":\"2021-07-13T12:07:47Z\",\"permissions\":[\"RESOURCES_READ\",\"ACCOUNTS_BALANCES_READ\",\"ACCOUNTS_READ\"],\"expirationDateTime\":\"2021-07-13T14:07:46Z\"},\"links\":{\"self\":\"/\"},\"meta\":{\"totalRecords\":1,\"totalPages\":1,\"requestDateTime\":\"2021-07-13T12:07:47Z\"}}\n";

		JsonObject o = new JsonParser().parse(response).getAsJsonObject();

		env.putObject("consent_endpoint_response", o);

		JsonArray requestPerms = new JsonArray();
		for (String s: new String[] { "ACCOUNTS_BALANCES_READ", "ACCOUNTS_READ", "RESOURCES_READ" }) {
			requestPerms.add(s);
		}
		JsonObject brazil = new JsonObject();
		brazil.add("requested_permissions", requestPerms);

		env.putObject("brazil_consent", brazil);

		condition.execute(env);
	}


	@Test(expected = ConditionError.class)
	public void extraGranted() {
		String response = "{\"data\":{\"consentId\":\"urn:raidiambank:58fff674-fb3a-4e45-bd10-7153d71d176c\",\"creationDateTime\":\"2021-07-13T12:07:47Z\",\"status\":\"AWAITING_AUTHORISATION\",\"statusUpdateDateTime\":\"2021-07-13T12:07:47Z\",\"permissions\":[\"RESOURCES_READ\",\"ACCOUNTS_BALANCES_READ\",\"ACCOUNTS_READ\",\"FLIBBLE\"],\"expirationDateTime\":\"2021-07-13T14:07:46Z\"},\"links\":{\"self\":\"/\"},\"meta\":{\"totalRecords\":1,\"totalPages\":1,\"requestDateTime\":\"2021-07-13T12:07:47Z\"}}\n";

		JsonObject o = new JsonParser().parse(response).getAsJsonObject();

		env.putObject("consent_endpoint_response", o);

		JsonArray requestPerms = new JsonArray();
		for (String s: new String[] { "RESOURCES_READ", "ACCOUNTS_BALANCES_READ", "ACCOUNTS_READ" }) {
			requestPerms.add(s);
		}
		JsonObject brazil = new JsonObject();
		brazil.add("requested_permissions", requestPerms);

		env.putObject("brazil_consent", brazil);

		condition.execute(env);
	}

	// this is permitted; some banks may not support some permissions
	@Test
	public void grantedFewerThanRequested() {
		String response = "{\"data\":{\"consentId\":\"urn:raidiambank:58fff674-fb3a-4e45-bd10-7153d71d176c\",\"creationDateTime\":\"2021-07-13T12:07:47Z\",\"status\":\"AWAITING_AUTHORISATION\",\"statusUpdateDateTime\":\"2021-07-13T12:07:47Z\",\"permissions\":[\"RESOURCES_READ\",\"ACCOUNTS_BALANCES_READ\",\"ACCOUNTS_READ\"],\"expirationDateTime\":\"2021-07-13T14:07:46Z\"},\"links\":{\"self\":\"/\"},\"meta\":{\"totalRecords\":1,\"totalPages\":1,\"requestDateTime\":\"2021-07-13T12:07:47Z\"}}\n";

		JsonObject o = new JsonParser().parse(response).getAsJsonObject();

		env.putObject("consent_endpoint_response", o);

		JsonArray requestPerms = new JsonArray();
		for (String s: new String[] { "RESOURCES_READ", "ACCOUNTS_BALANCES_READ", "ACCOUNTS_READ", "FLIBBLE" }) {
			requestPerms.add(s);
		}
		JsonObject brazil = new JsonObject();
		brazil.add("requested_permissions", requestPerms);

		env.putObject("brazil_consent", brazil);

		condition.execute(env);
	}

}
