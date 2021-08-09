package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.apis.AbstractProtectedResourceInferenceCondition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class ProtectedResourceInferenceConditionTests {

	@Mock
	private TestInstanceEventLog eventLog;

	private Environment environment = new Environment();

	@Test
	public void ignoresFullUrls() {

		String resourceUrl = "https://matls-api.mockbank.poc.raidiam.io/payments/v1/pix/payments";
		setResourceUrl(resourceUrl);

		AbstractCondition condition = condition("/payments/v1/pix/payments");

		condition.execute(environment);

		String protectedResourceUrl = environment.getString("protected_resource_url");
		assertEquals(resourceUrl, protectedResourceUrl);

	}

	@Test
	public void fullUrlsAreNotOverridden() {

		String resourceUrl = "https://matls-api.mockbank.poc.raidiam.io/payments/v1/pix/payments";
		setResourceUrl(resourceUrl);

		AbstractCondition condition = condition("/payments/v2/payments");

		condition.execute(environment);

		String protectedResourceUrl = environment.getString("protected_resource_url");
		assertEquals(resourceUrl, protectedResourceUrl);

	}

	@Test
	public void baseUrlsWithNoTrailingSlashAreConfigured() {

		String resourceUrl = "https://matls-api.mockbank.poc.raidiam.io";
		setResourceUrl(resourceUrl);

		AbstractCondition condition = condition("/payments/v2/payments");

		condition.execute(environment);

		String protectedResourceUrl = environment.getString("protected_resource_url");
		assertEquals("https://matls-api.mockbank.poc.raidiam.io/payments/v2/payments", protectedResourceUrl);

	}

	@Test
	public void baseUrlsWithTrailingSlashAreConfigured() {

		String resourceUrl = "https://matls-api.mockbank.poc.raidiam.io/";
		setResourceUrl(resourceUrl);

		AbstractCondition condition = condition("/payments/v3/payments");

		condition.execute(environment);

		String protectedResourceUrl = environment.getString("protected_resource_url");
		assertEquals("https://matls-api.mockbank.poc.raidiam.io/payments/v3/payments", protectedResourceUrl);

	}

	@Test
	public void baseUrlsWhichAreNotUrls() {

		String resourceUrl = "thisisnotaurl";
		setResourceUrl(resourceUrl);

		AbstractCondition condition = condition("/payments/v3/payments");

		try {
			condition.execute(environment);
			fail("This should have errored");
		} catch(ConditionError ce) {
			assertEquals(": The configured resource URL: 'thisisnotaurl' was not a URL", ce.getMessage());
		}

	}

	private void setResourceUrl(String resourceUrl) {
		JsonObject resourceConfig = new JsonObject();
		resourceConfig.addProperty("resourceUrl", resourceUrl);
		environment.putObject("resource", resourceConfig);
	}

	private AbstractCondition condition(final String url) {
		AbstractCondition condition = new AbstractProtectedResourceInferenceCondition() {
			@Override
			protected String getResourcePath() {
				return url;
			}
		};
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		return condition;
	}

}
