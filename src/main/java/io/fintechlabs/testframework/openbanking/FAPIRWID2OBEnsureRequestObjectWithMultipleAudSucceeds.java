package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.OpenBankingUkAddMultipleAcrClaimsToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.fapi.AbstractFAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAud;
import io.fintechlabs.testframework.fapi.FAPIRWID2;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-ensure-request-object-with-multiple-aud-succeeds",
	displayName = "FAPI-RW-ID2-OB: ensure request object with good or bad values of aud succeeds",
	summary = "This test pass aud value as an array containing good and bad values then server must accept it.",
	profile = "FAPI-RW-ID2-OB",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.jwks",
		"resource.resourceUrl",
		"resource.resourceUrlAccountRequests",
		"resource.resourceUrlAccountsResource",
		"resource.institution_id"
	},
	notApplicableForVariants = {
		FAPIRWID2.variant_mtls,
		FAPIRWID2.variant_privatekeyjwt
	}
)
public class FAPIRWID2OBEnsureRequestObjectWithMultipleAudSucceeds extends AbstractFAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAud {

	@Variant(name = variant_openbankinguk_mtls)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
	}

	@Variant(name = variant_openbankinguk_privatekeyjwt)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
	}

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(OpenBankingUkAddMultipleAcrClaimsToAuthorizationEndpointRequest.class);
	}
}
