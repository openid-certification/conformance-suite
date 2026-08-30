package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-present-credential-without-status",
	displayName = "OID4VP-1.0-FINAL Verifier: Present a credential without revocation information",
	summary = """
		Presents a credential that carries no revocation information at all: for the SD-JWT VC \
		credential format there is no 'status' claim, and for the ISO mDL credential format the \
		Mobile Security Object has no status element. Both are optional (draft-ietf-oauth-status-list \
		section 6.2; ISO/IEC 18013-5 12.3.6.2 "An MSO may contain the Status structure"), so a \
		verifier must be able to process such a credential - whether it then accepts it is the \
		verifier's trust policy, but it must not malfunction on the absent status information.

		Other than the missing status reference the credential and flow are identical to the happy \
		flow. The conformance suite acts as a mock web wallet. You must configure your verifier to \
		use the authorization endpoint url below instead of 'openid4vp://' and then start the flow \
		in your verifier as normal.
		""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
public class VP1FinalVerifierPresentCredentialWithoutStatus extends AbstractVP1FinalVerifierTest {

	@Override
	protected void createStatusListReference() {
		eventLog.log(getName(), "Not allocating a status list reference: the presented credential "
			+ "will carry no revocation information, which the specifications permit.");
	}

	@Override
	protected void generateStatusListToken() {
		// nothing to serve - no status list is referenced
	}
}
