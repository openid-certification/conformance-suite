package io.fintechlabs.testframework.fapi;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointDiscoveryUrl;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointIdTokenSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointIssuer;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRegistrationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointUserinfoSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckJwksUri;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointResponseTypesSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointTokenEndpointAuthMethodsSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckTLSClientCertificateBoundAccessTokens;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;

public abstract class AbstractFAPIDiscoveryEndpointVerification extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {

		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		setStatus(Status.CONFIGURED);
		fireSetupDone();

	}

	protected void performEndpointVerification() {

		callAndContinueOnFailure(CheckDiscEndpointDiscoveryUrl.class,Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointIssuer.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");

		callAndContinueOnFailure(FAPIRWCheckDiscEndpointResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-2");
		callAndContinueOnFailure(FAPIRWCheckTLSClientCertificateBoundAccessTokens.class, Condition.ConditionResult.WARNING, "FAPI-RW-5.2.2-6");

		callAndContinueOnFailure(CheckDiscEndpointIdTokenSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");

		callAndContinueOnFailure(FAPIRWCheckDiscEndpointTokenEndpointAuthMethodsSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-14");
		callAndContinueOnFailure(CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");

		call(condition(CheckDiscEndpointUserinfoSigningAlgValuesSupported.class)
			.skipIfElementMissing("server", "userinfo_signing_alg_values_supported")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.requirement("FAPI-RW-8.6")
			.dontStopOnFailure()
		);

		callAndContinueOnFailure(CheckDiscEndpointTokenEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCD-3");

		call(condition(CheckDiscEndpointRegistrationEndpoint.class)
			.skipIfElementMissing("server", "registration_endpoint")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.requirement("OIDCD-3")
			.dontStopOnFailure()
		);

		callAndContinueOnFailure(CheckJwksUri.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
	}

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		performEndpointVerification();

		fireTestFinished();

	}

}
