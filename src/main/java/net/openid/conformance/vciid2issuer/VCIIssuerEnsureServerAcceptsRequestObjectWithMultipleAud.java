package net.openid.conformance.vciid2issuer;

import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddMultipleAudToRequestObject;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vci-id2-issuer-ensure-request-object-with-multiple-aud-succeeds",
	displayName = "OID4VCIID2: ensure request object with multiple aud succeeds",
	summary = "This test case validates the standard credential issuance flow using an emulated wallet, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by retrieving metadata from both the Credential Issuer and the OAuth 2.0 Authorization Server. An authorization request is initiated using Pushed Authorization Requests (PAR), and an access token is obtained. The test then retrieves a nonce from the Credential Endpoint, constructs a DPoP proof JWT bound to the nonce, and successfully requests a credential from the Credential Endpoint. Additionally, this test passes the aud value as an array containing both good and bad values and expects the server to auth server must accept it.",
	profile = "OID4VCI-ID2",
	configurationFields = {
		"server.discoveryIssuer",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl",
		"vci.credential_configuration_id",
		"vci.authorization_server"
	}
)
@VariantNotApplicable(
		parameter = FAPI2ID2OPProfile.class,
		values = "cbuae"
)
public class VCIIssuerEnsureServerAcceptsRequestObjectWithMultipleAud extends AbstractVCIIssuerTestModule {

	@Override
	protected void performPARRedirectWithRequestUri() {
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "PAR-4");
		performRedirect();
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps()
			.replace(AddAudToRequestObject.class,
					condition(AddMultipleAudToRequestObject.class).requirement("RFC7519-4.1.3"));
	}
}
