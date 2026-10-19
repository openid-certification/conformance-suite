package net.openid.conformance.vci10issuer;

import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.clientattestation.ReplaceClientInstanceKeyWithNewKey;

/**
 * Negative test that verifies the authorization server properly rejects client attestation
 * proof-of-possession JWTs signed with a key that does not match the attested cnf.jwk.
 * <p>
 * Both the attestation and PoP signatures are cryptographically valid, but the PoP is signed
 * with a freshly generated key rather than the key bound in the attestation's cnf claim.
 * The authorization server must reject this because the PoP does not prove possession of
 * the attested key. The rejection is expected at the PAR endpoint for the authorization code
 * grant, or at the token endpoint for the pre-authorized code grant.
 * <p>
 * This test is only applicable when client_auth_type=client_attestation is selected.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.2">OAuth2-ATCA Section 5.2</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-mismatched-client-attestation-pop-key",
	displayName = "OID4VCI 1.0: Issuer fail on mismatched client attestation pop key",
	summary = """
		This test case checks for proper error handling when a client attestation \
		proof-of-possession JWT is signed with a key that does not match the cnf.jwk in the \
		attestation. Both signatures are cryptographically valid, but the PoP key is different \
		from the attested key. The request is sent to the PAR endpoint for the authorization \
		code grant, or to the token endpoint for the pre-authorized code grant (which does \
		not use PAR). The authorization server must reject this request with an \
		invalid_client, invalid_request, or invalid_client_attestation error.""",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnMismatchedClientAttestationPopKey extends AbstractVCIIssuerClientAttestationNegativeTest {

	@Override
	protected ConditionSequence createClientAuthenticationSequence() {
		return super.createClientAuthenticationSequence()
			.butFirst(condition(ReplaceClientInstanceKeyWithNewKey.class).requirement("OAuth2-ATCA07-5.2"));
	}
}
