package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.ObtainAccessTokenWithClientCredentials;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.openbanking_brasil.testmodules.support.MapDirectoryValues;
import net.openid.conformance.openbanking_brasil.testmodules.support.UnmapDirectoryValues;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "preflight-cert-check-payments-test",
	displayName = "Pre-flight checks will validate the mTLS certificate before requesting an access token using the Directory client_id provided in the test configuration. Finally, an SSA will be generated using the Open Banking Brasil Directory.",
	summary = "Pre-flight checks will validate the mTLS certificate before requesting an access token using the Directory client_id provided in the test configuration. Finally, an SSA will be generated using the Open Banking Brasil Directory.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"directory.client_id"
	}
)

public class PreFlightCertCheckPaymentsModule extends PreFlightCertCheckModule {

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		super.createGetAccessTokenWithClientCredentialsSequence(clientAuthSequence)
			.replace(SetConsentsScopeOnTokenEndpointRequest.class, condition(SetPaymentsScopeOnTokenEndpointRequest.class));
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

}
