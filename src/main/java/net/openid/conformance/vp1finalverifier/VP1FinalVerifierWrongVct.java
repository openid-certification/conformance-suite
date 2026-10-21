package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.as.CreateSdJwtKbCredentialWithWrongVct;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-wrong-vct",
	displayName = "OID4VP-1.0-FINAL Verifier: Credential with a vct that was not requested",
	summary = """
		Creates a credential whose vct is a type the verifier did not request: it is not one of \
		the vct_values in the DCQL query and does not inherit from one. The claims, the issuer \
		signature and the Key Binding JWT are otherwise valid. The verifier must reject this \
		presentation, as the returned credential does not meet the criteria in the query.

		Whether a verifier must accept a credential whose vct inherits from a requested type is \
		discussed in https://github.com/openid/OpenID4VP/issues/741; this test uses an unrelated \
		type, so it does not depend on that outcome.

		This test is only applicable for the SD-JWT VC credential format.

		The conformance suite acts as a mock web wallet. You must configure your verifier to use \
		the authorization endpoint url below instead of 'openid4vp://' and then start the flow in \
		your verifier as normal.

		On a 4xx response the test passes immediately; on a success response a screenshot of the \
		verifier's error must be uploaded and the test finishes as REVIEW.
		""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
@VariantNotApplicable(parameter = VP1FinalVerifierCredentialFormat.class, values = {"iso_mdl"})
public class VP1FinalVerifierWrongVct extends AbstractVP1FinalVerifierNegativeTest {

	@Override
	protected void createSdJwtCredential() {
		callAndStopOnFailure(CreateSdJwtKbCredentialWithWrongVct.class);
	}
}
