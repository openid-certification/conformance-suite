package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.vci10issuer.condition.clientattestation.AddClientAttestationClientAuthToEndpointRequest;
import net.openid.conformance.vci10issuer.condition.clientattestation.ReplaceClientInstanceKeyWithNewKey;

/**
 * Negative test that verifies the authorization server properly rejects client attestation
 * proof-of-possession JWTs signed with a key that does not match the attested cnf.jwk.
 * <p>
 * Both the attestation and PoP signatures are cryptographically valid, but the PoP is signed
 * with a freshly generated key rather than the key bound in the attestation's cnf claim.
 * The authorization server must reject this because the PoP does not prove possession of
 * the attested key.
 * <p>
 * Note: This test only runs when client_auth_type=client_attestation is selected.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.2">OAuth2-ATCA Section 5.2</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-mismatched-client-attestation-pop-key",
	displayName = "OID4VCI 1.0: Issuer fail on mismatched client attestation pop key",
	summary = "This test case checks for proper error handling when a client attestation " +
		"proof-of-possession JWT is signed with a key that does not match the cnf.jwk in the " +
		"attestation. Both signatures are cryptographically valid, but the PoP key is different " +
		"from the attested key. The authorization server must reject this request with an " +
		"invalid_client error. " +
		"Note: This test requires client_auth_type=client_attestation variant. " +
		"If a different client authentication method is used, the test will be skipped.",
	profile = "OID4VCI-1_0"
)
@VariantNotApplicable(parameter = VCIClientAuthType.class, values = {"mtls", "private_key_jwt"})
public class VCIIssuerFailOnMismatchedClientAttestationPopKey extends VCIIssuerHappyFlow {

	@Override
	protected void addClientAuthenticationToPAREndpointRequest() {
		ConditionSequence seq = new AddClientAttestationClientAuthToEndpointRequest();
		seq.butFirst(condition(ReplaceClientInstanceKeyWithNewKey.class).requirement("OAuth2-ATCA07-5.2"));
		call(seq);
	}

	@Override
	protected void processParResponse() {
		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-1");
		callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-1");
		env.unmapKey("endpoint_response");

		fireTestFinished();
	}
}
