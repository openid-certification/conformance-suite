package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class FAPICIBAAddAcrValuesToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPICIBAAddAcrValuesToAuthorizationEndpointRequest cond;

	private String acrValues = "urn:openbanking:psd2:sca";

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPICIBAAddAcrValuesToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		env.putObject("authorization_endpoint_request", new JsonObject());

		JsonObject server = new JsonObject();
		JsonArray acrValuesSupported = new JsonArray();
		acrValuesSupported.add("urn:mace:incommon:iap:silver");
		acrValuesSupported.add("urn:openbanking:psd2:sca");
		acrValuesSupported.add("urn:openbanking:psd2:ca");
		server.add("acr_values_supported", acrValuesSupported);
		env.putObject("server", server);
	}

	@Test
	public void testEvaluate_validAcrValue() {
		JsonObject client = new JsonObject();
		client.addProperty("acr_value", acrValues);
		env.putObject("client", client);

		cond.execute(env);
		assertThat(env.getString("authorization_endpoint_request", "acr_values")).isNotEmpty();
		assertThat(env.getString("authorization_endpoint_request", "acr_values")).isEqualTo(acrValues);
	}

	@Test
	public void testEvaluate_invalidAcrValue() {
		assertThrows(ConditionError.class, () -> {
			JsonObject client = new JsonObject();
			client.addProperty("acr_value", "urn:openbanking:psd2:acr");
			env.putObject("client", client);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_NoAcrValueInConfig() {
		assertThrows(ConditionError.class, () -> {
			JsonObject client = new JsonObject();
			env.putObject("client", client);

			cond.execute(env);
			assertThat(env.getString("authorization_endpoint_request", "acr_values")).isNotEmpty();
			assertThat(env.getString("authorization_endpoint_request", "acr_values")).isEqualTo(acrValues);
		});
	}
}
