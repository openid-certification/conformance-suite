package net.openid.conformance.openbanking_brasil;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallHttpResource;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "configurable-endpoint-test",
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
/*
	These allow you to configure variants of your test. In this case, I'm allowing for different
	methods of authorisation, but many other enums exist for creating variants
 */
@VariantParameters({
	ClientAuthType.class
})
public class MoreConfigurableApiCallTest extends AbstractTestModule {

	/*
		The configure method allows us to pre-configure our test.
	 */
	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putObject("config", config);
		String url = OIDFJSON.getString(config.getAsJsonObject("resource").get("resourceUrl"));
		env.putString("resource_url", url);
		callAndContinueOnFailure(CallHttpResource.class);
		callAndStopOnFailure(ExtractSimpleMessage.class);
		setStatus(Status.CONFIGURED);
	}

	@Override
	/*
		The essense of the test itself. Here is where we run actual assertions on the response object
		or whatever we are testing.
	 */
	public void start() {
		setStatus(Status.RUNNING);
		callAndStopOnFailure(SimpleMessageAssert.class, Condition.ConditionResult.FAILURE);
		setResult(Result.PASSED);
		setStatus(Status.FINISHED);
	}
}
