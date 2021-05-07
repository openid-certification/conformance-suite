package net.openid.conformance.openbanking_brasil;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantParameters;
@PublishTestModule(
	testName = "configurable-new-test",
	displayName = "A simple configurable endpoint test",
	summary = "A test which hits a plain text, unprotected endpoint and carries out asserts on the response",
	profile = "PROFILE",
	/*
		These contribute to the configuration UI. There are a number of areas and fields
		which can be configured. Example src/main/resources/static/schedule-test.html for
		more info on what they are.
	 */
	configurationFields = {
		"resource.resourceUrl"
	}
)
@VariantParameters({
	ClientAuthType.class
})
public class NewTest extends AbstractTestModule  {
	/**
	 * Method is called to pass configuration parameters
	 *
	 * @param config              A JSON object consisting of details that the testRunner
	 *                            doesn't need to know about
	 * @param baseUrl             The base of the URL that will need to be appended to any
	 *                            URL construction.
	 * @param externalUrlOverride
	 */
	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		setStatus(Status.CONFIGURED);
	}

	/**
	 * Called by the TestRunner to start the test
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);
		setResult(Result.PASSED);
		setStatus(Status.FINISHED);
	}
}
