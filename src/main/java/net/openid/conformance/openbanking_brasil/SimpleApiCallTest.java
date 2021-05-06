package net.openid.conformance.openbanking_brasil;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "simple-endpoint-test",
	displayName = "A simple endpoint test",
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
public class SimpleApiCallTest extends AbstractTestModule {

	@VariantSetup(parameter = ClientAuthType.class, value = "none")
	public void setupAnon() {
		// noop
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupJwt() {
		// noop
	}

	@Override
	/*
		The configure method is where the environment for a specific test run is configured
		We may also invoke Conditions in here. Conditions are the basic elements of a test
		and are what will carry out invocations and assertions on your behalf. The setStatus call
		at the end is important, as it allows the framework to progress

		The env variable is essentially a context for a test run. Configuration options get translated
		here and injected into the environment for use by Conditions. Conditions can also use it
		to pass state to further Condition objects.
	 */
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putObject("config", config);
		String url = config.getAsJsonObject("resource").get("resourceUrl").getAsString();
		env.putString("url", url);
		callAndContinueOnFailure(CallSimpleEndpoint.class);
		setStatus(Status.CONFIGURED);
	}

	/**
	 * This is where things happen
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);
		callAndStopOnFailure(ExtractSimpleMessage.class);
		callAndStopOnFailure(SimpleMessageAssert.class, Condition.ConditionResult.FAILURE);
		setResult(Result.PASSED);
		setStatus(Status.FINISHED);
	}

}
