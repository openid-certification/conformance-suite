package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddClientIdToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddIatToRequestObject;
import io.fintechlabs.testframework.condition.client.AddJtiToRequestObject;
import io.fintechlabs.testframework.condition.client.AddNbfToRequestObject;
import io.fintechlabs.testframework.condition.client.AddRequestToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.CreateBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.SignRequestObjectInvalid;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ensure-request-object-signature-algorithm-is-bad-fails",
	displayName = "FAPI-CIBA: Ensure request_object signature algorithm is bad fails",
	summary = "This test should end with the backchannel authorisation server returning an error message that the request is invalid.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPICIBAEnsureRequestObjectSignatureAlgorithmIsBadFails extends AbstractFAPICIBAEnsureSendingInvalidBackchannelAuthorisationRequest {

	@Variant(name = variant_ping_mtls)
	public void setupPingMTLS() {
		super.setupPingMTLS();
	}

	@Variant(name = variant_ping_privatekeyjwt)
	public void setupPingPrivateKeyJwt() {
		super.setupPingPrivateKeyJwt();
	}

	@Variant(name = variant_poll_mtls)
	public void setupPollMTLS() {
		super.setupPollMTLS();
	}

	@Variant(name = variant_poll_privatekeyjwt)
	public void setupPollPrivateKeyJwt() {
		super.setupPollPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_ping_mtls)
	public void setupOpenBankingUkPingMTLS() {
		super.setupOpenBankingUkPingMTLS();
	}

	@Variant(name = variant_openbankinguk_ping_privatekeyjwt)
	public void setupOpenBankingUkPingPrivateKeyJwt() {
		super.setupOpenBankingUkPingPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_poll_mtls)
	public void setupOpenBankingUkPollMTLS() {
		super.setupOpenBankingUkPollMTLS();
	}

	@Variant(name = variant_openbankinguk_poll_privatekeyjwt)
	public void setupOpenBankingUkPollPrivateKeyJwt() {
		super.setupOpenBankingUkPollPrivateKeyJwt();
	}

	@Override
	protected void performAuthorizationRequest() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);
		callAndStopOnFailure(AddIatToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddExpToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddNbfToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddJtiToRequestObject.class, "CIBA-7.1.1");

		// aud, iss are added by SignRequestObject
		callAndStopOnFailure(SignRequestObject.class, "CIBA-7.1.1");

		callAndStopOnFailure(SignRequestObjectInvalid.class, "CIBA-7.2");

		callAndStopOnFailure(CreateBackchannelAuthenticationEndpointRequest.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientIdToBackchannelAuthenticationEndpointRequest.class);
		callAndStopOnFailure(AddRequestToBackchannelAuthenticationEndpointRequest.class);

		callAndStopOnFailure(CallBackchannelAuthenticationEndpoint.class);
	}
}