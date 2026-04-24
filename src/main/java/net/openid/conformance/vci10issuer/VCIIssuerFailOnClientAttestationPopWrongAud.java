package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;
import net.openid.conformance.vci10issuer.condition.clientattestation.AddClientAttestationClientAuthWithWrongAudPop;

/**
 * Negative test that verifies the authorization server rejects client attestation
 * proof-of-possession JWTs whose 'aud' claim is not the authorization server issuer.
 * Mirrors the private_key_jwt test FAPI2SPFinalEnsureClientAssertionWithWrongAudFails for
 * client_attestation — 'aud' is carried on the PoP JWT for client_attestation, not the
 * attestation JWT itself.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.2-5.2.1">OAuth 2.0 Attestation-Based Client Authentication §5.2</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-client-attestation-pop-wrong-aud",
	displayName = "OID4VCI 1.0: Issuer fail on client attestation pop with wrong aud",
	summary = """
		Sends a PAR request with a client attestation proof-of-possession JWT whose \
		'aud' claim is not the authorization server issuer. The authorization server must \
		reject the request with an invalid_client, invalid_request, or \
		invalid_client_attestation error. \
		Only applicable when client_auth_type=client_attestation.""",
	profile = "OID4VCI-1_0"
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = {"mtls", "private_key_jwt"})
public class VCIIssuerFailOnClientAttestationPopWrongAud extends AbstractVCIIssuerTestModule {

	@VariantSetup(parameter = ClientAuthType.class, value = "client_attestation")
	@Override
	public void setupClientAttestation() {
		addClientAuthentication = AddClientAttestationClientAuthWithWrongAudPop.class;
	}

	@Override
	protected void processParResponse() {
		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		env.unmapKey("endpoint_response");

		fireTestFinished();
	}
}
