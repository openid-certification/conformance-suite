package io.fintechlabs.testframework.fapi;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointClaimsParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointDiscoveryUrl;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointIdTokenSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRegistrationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointUserinfoSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckJwksUri;
import io.fintechlabs.testframework.condition.client.CheckJwksUriIsHostedOnOpenBankingDirectory;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointClaimsSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointGrantTypesSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointResponseTypesSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointScopesSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointTokenEndpointAuthMethodsSupported;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-discovery-end-point-verification",
	displayName = "FAPI-RW: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configurations (including scopes, response_types, grant_types etc) is containing the required value in the specification",
	profile = "FAPI-RW",
	configurationFields = {
		"server.discoveryUrl",
	}
)
public class FAPIRWDiscoveryEndpointVerification extends AbstractTestModule {

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		callAndContinueOnFailure(CheckDiscEndpointDiscoveryUrl.class,Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(FAPIRWCheckDiscEndpointResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-2");
		callAndContinueOnFailure(CheckDiscEndpointRequestParameterSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointRequestUriParameterSupported.class, Condition.ConditionResult.WARNING, "OB-7.1-1");
		callAndContinueOnFailure(CheckDiscEndpointIdTokenSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointRequestObjectSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(FAPIRWCheckDiscEndpointTokenEndpointAuthMethodsSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-14");
		callAndContinueOnFailure(CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);


		call(condition(CheckDiscEndpointUserinfoSigningAlgValuesSupported.class)
			.skipIfElementMissing("server", "userinfo_endpoint")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.dontStopOnFailure()
		);

		callAndContinueOnFailure(CheckDiscEndpointTokenEndpoint.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE);

		call(condition(CheckDiscEndpointRegistrationEndpoint.class)
			.skipIfElementMissing("server", "registration_endpoint")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.dontStopOnFailure()
		);

		callAndContinueOnFailure(CheckJwksUri.class, Condition.ConditionResult.FAILURE);

		performProfileSpecificChecks();

		fireTestFinished();

	}

	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);
		callAndStopOnFailure(GetDynamicServerConfiguration.class);
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	protected void performProfileSpecificChecks() {
		callAndContinueOnFailure(FAPIRWCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(FAPIRWCheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(FAPIRWCheckDiscEndpointScopesSupported.class, Condition.ConditionResult.FAILURE);
	}
}
