package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CopyAcrValueFromDynamicRegistrationTemplateToClientConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CopyAcrValueFromDynamicRegistrationTemplateToClientConfiguration cond;

	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new CopyAcrValueFromDynamicRegistrationTemplateToClientConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_NoError() {
		JsonObject dynamicClientRegistrationTemplate = new JsonObject();
		dynamicClientRegistrationTemplate.addProperty("acr_value", "urn:openbanking:psd2:sca");
		env.putObject("original_client_config", dynamicClientRegistrationTemplate);

		JsonObject client = new JsonObject();
		client.add("client", new JsonObject());
		env.putObject("client", client);

		cond.execute(env);

		assertThat(env.getString("client", "acr_value")).isEqualTo("urn:openbanking:psd2:sca");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingInConfiguration() {
		JsonObject dynamicClientRegistrationTemplate = new JsonObject();
		env.putObject("original_client_config", dynamicClientRegistrationTemplate);
		cond.execute(env);
	}
}
