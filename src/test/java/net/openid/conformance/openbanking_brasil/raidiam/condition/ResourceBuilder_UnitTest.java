package net.openid.conformance.openbanking_brasil.raidiam.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ResourceBuilder_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ResourceBuilder cond;

	@Before
	public void setUp() throws Exception {
		cond = new ResourceBuilder();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_NoAllowDifferentBaseUrl_SameApi() {
		String initialUrl = "https://matls-api.mockbank.poc.raidiam.io/open-banking/accounts/v1/accounts";
		env.putString("config", "resource.resourceUrl", initialUrl);

		String expectedUrl = "https://matls-api.mockbank.poc.raidiam.io/open-banking/accounts/v1/accounts2";
		cond.setApi("accounts");
		cond.setEndpoint("/accounts2");

		cond.execute(env);

		verify(env, atLeastOnce()).getString("config", "resource.resourceUrl");

		assertThat(env.getString("protected_resource_url")).isEqualTo(expectedUrl);

	}

	@Test
	public void testEvaluate_NoAllowDifferentBaseUrl_DifferentApiError() {
		String initialUrl = "https://matls-api.mockbank.poc.raidiam.io/open-banking/resources/v1/resources";
		env.putString("config", "resource.resourceUrl", initialUrl);

		String expectedUrl = "https://matls-api.mockbank.poc.raidiam.io/open-banking/accounts/v1/accounts2";
		cond.setApi("accounts");
		cond.setEndpoint("/accounts2");

		boolean exception = false;

		try {
			cond.execute(env);
		} catch (ConditionError e) {
			exception = true;
		}


		verify(env, atLeastOnce()).getString("config", "resource.resourceUrl");
		assertThat(env.getString("protected_resource_url")).isNotEqualTo(expectedUrl);
		assertThat(exception);

	}

	@Test
	public void testEvaluate_AllowDifferentBaseUrl_SameApi() {
		String initialUrl = "https://matls-api.mockbank.poc.raidiam.io/open-banking/accounts/v1/accounts";
		String consentUrl = "https://matls-api.mockbank.poc.raidiam.io/open-banking/consents/v1/consents";

		env.putString("config", "resource.resourceUrl", initialUrl);
		env.putString("config","resource.consentUrl", consentUrl);

		String expectedUrl = "https://matls-api.mockbank.poc.raidiam.io/open-banking/accounts/v1/accounts2";
		cond.setApi("accounts");
		cond.setEndpoint("/accounts2");
		cond.setAllowDifferentBaseUrl(true);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("config", "resource.resourceUrl");
		verify(env, atLeastOnce()).getString("config", "resource.consentUrl");

		assertThat(env.getString("protected_resource_url")).isEqualTo(expectedUrl);

	}

	@Test
	public void testEvaluate_AllowDifferentBaseUrl_DifferentApi() {
		String initialUrl = "https://matls-api.mockbank.poc.raidiam.io/open-banking/accounts/v1/accounts";
		String consentUrl = "https://matls-api.mockbank.poc.raidiam.io/open-banking/consents/v1/consents";

		env.putString("config", "resource.resourceUrl", initialUrl);
		env.putString("config","resource.consentUrl", consentUrl);

		String expectedUrl = "https://matls-api.mockbank.poc.raidiam.io/open-banking/resources/v1/resources";
		cond.setApi("resources");
		cond.setEndpoint("/resources");
		cond.setAllowDifferentBaseUrl(true);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("config", "resource.resourceUrl");
		verify(env, atLeastOnce()).getString("config", "resource.consentUrl");

		assertThat(env.getString("protected_resource_url")).isEqualTo(expectedUrl);

	}
}
