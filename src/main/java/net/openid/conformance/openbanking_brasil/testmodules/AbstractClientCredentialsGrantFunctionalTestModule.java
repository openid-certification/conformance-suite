package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.ObtainAccessTokenWithClientCredentials;

public abstract class AbstractClientCredentialsGrantFunctionalTestModule extends AbstractBlockLoggingTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		preConfigure(config, baseUrl, externalUrlOverride);

		env.putString("base_url", baseUrl);
		env.putObject("config", config);
		call(sequence(ObtainAccessTokenWithClientCredentials.class));
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);
		callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
		postConfigure(config, baseUrl, externalUrlOverride);
		setStatus(Status.CONFIGURED);
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		runTests();
		setResult(Result.PASSED);
		setStatus(Status.FINISHED);
	}

	protected abstract void runTests();


	protected void preConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {

	}


	protected void postConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {

	}


}
