package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddNbfValueIs10MinutesInFutureToRequestObject;
import io.fintechlabs.testframework.condition.client.AddNbfValueIs70MinutesInPastToRequestObject;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-ensure-request-object-nbf-is-70-minutes-in-past-fails",
	displayName = "FAPI-CIBA: 'nbf' value in request object is 70 minutes in the past, should return an error",
	summary = "This test should return an error that the 'nbf' value in request object from back channel authentication endpoint request is 70 minutes in the past",
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
public class FAPICIBAEnsureRequestObjectNbfIs70MinutesInPastFails extends AbstractFAPICIBAEnsureSendingInvalidBackchannelAuthorisationRequest {

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
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(AddNbfValueIs70MinutesInPastToRequestObject.class, "CIBAProfile-5.2.2-9");
	}

}
