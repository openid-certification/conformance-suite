package net.openid.conformance.vci10issuer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchBindingKeysAreDistinct;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchBindingKeysMatchSentProofKeys;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchMdocCredentialDatasetsMatch;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchSdJwtCredentialDatasetsMatch;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchSdJwtDisclosureSaltsAreDistinct;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchStatusListIndicesAreUnpredictable;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchStatusReferencesAreDistinct;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchTimeClaimsNotLinkable;
import net.openid.conformance.vci10issuer.condition.VCIEnsureNotMoreCredentialsThanRequestedProofs;
import net.openid.conformance.vci10issuer.condition.VCIExtractBatchMdocBindingKeys;
import net.openid.conformance.vci10issuer.condition.VCIExtractBatchSdJwtBindingKeys;
import net.openid.conformance.vci10issuer.condition.VCIPrepareBatchProofKeys;
import net.openid.conformance.vci10issuer.condition.VCIValidateBatchCredentialIssuanceMetadata;
import net.openid.conformance.vci10issuer.condition.VCIWarnBatchStatusListUrisProvideHerdPrivacy;
import net.openid.conformance.vci10issuer.condition.VCIWarnIfFewerCredentialsThanRequestedProofs;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-batch-issuance",
	displayName = "OID4VCI 1.0: Issuer batch credential issuance",
	summary = "This test requests multiple credentials in a single credential request, as advertised by the "
		+ "'batch_credential_issuance' credential issuer metadata. The wallet sends one key proof per requested "
		+ "credential (the advertised batch_size, capped at 20) and verifies that the issuer returns no more "
		+ "credentials than proofs sent, that all returned credentials contain the same Credential Dataset, and "
		+ "that each credential is bound to a distinct one of the proof keys. "
		+ "The test is skipped if the issuer does not advertise batch support, if the credential configuration "
		+ "does not use cryptographic binding (no proofs are sent), or if only one credential is returned (which "
		+ "is permitted, but means batch behavior cannot be evaluated). A warning is raised if the issuer "
		+ "returns fewer credentials than requested, which is permitted by the specification.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerBatchIssuance extends AbstractVCIIssuerTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);

		if (env.getElementFromObject("vci", "credential_issuer_metadata.batch_credential_issuance") == null) {
			fireTestSkipped("The credential issuer metadata does not contain 'batch_credential_issuance', "
				+ "so the issuer does not support batch credential issuance (which is permitted behavior) "
				+ "and this test cannot be run.");
		}

		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			fireTestSkipped("The selected credential configuration does not use cryptographic binding, "
				+ "so no 'proofs' parameter is sent in the credential request and batch issuance cannot be tested.");
		}

		callAndStopOnFailure(VCIValidateBatchCredentialIssuanceMetadata.class, "OID4VCI-1FINAL-12.2.4");
	}

	@Override
	protected void generateKeyAttestationAndProof() {
		callAndStopOnFailure(VCIPrepareBatchProofKeys.class, "OID4VCI-1FINAL-12.2.4");
		// Make the batch keys visible as client_jwks so VCIGenerateJwtProof generates one proof
		// per key (and any key attestation covers all of them), without touching the real
		// client_jwks which is also used for client authentication.
		env.mapKey("client_jwks", "vci_batch_proof_jwks");
		try {
			super.generateKeyAttestationAndProof();
		} finally {
			env.unmapKey("client_jwks");
		}
	}

	@Override
	protected void verifyEffectiveCredentialResponse() {
		// extraction + per-credential validation + notification
		super.verifyEffectiveCredentialResponse();

		eventLog.startBlock(currentClientString() + "Verify batch issuance behavior");

		callAndContinueOnFailure(VCIEnsureNotMoreCredentialsThanRequestedProofs.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");
		callAndContinueOnFailure(VCIWarnIfFewerCredentialsThanRequestedProofs.class, ConditionResult.WARNING, "OID4VCI-1FINAL-8.3");

		JsonArray credentials = env.getObject("extracted_credentials").getAsJsonArray("list");
		if (credentials.size() < 2) {
			eventLog.endBlock();
			fireTestSkipped("Only " + credentials.size() + " credential(s) were returned (issuing fewer "
				+ "credentials than requested proofs is permitted behavior), so batch issuance behavior "
				+ "cannot be evaluated.");
		}

		String format = env.getString("vci_credential_configuration", "format");
		if ("mso_mdoc".equals(format)) {
			callAndContinueOnFailure(VCIEnsureBatchMdocCredentialDatasetsMatch.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-3.3.2");
			callAndStopOnFailure(VCIExtractBatchMdocBindingKeys.class, "OID4VCI-1FINAL-8.3");
		} else {
			callAndContinueOnFailure(VCIEnsureBatchSdJwtCredentialDatasetsMatch.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-3.3.2");
			callAndContinueOnFailure(VCIEnsureBatchSdJwtDisclosureSaltsAreDistinct.class, ConditionResult.FAILURE, "SDJWT-10.1");
			callAndStopOnFailure(VCIExtractBatchSdJwtBindingKeys.class, "OID4VCI-1FINAL-8.3");
		}
		callAndContinueOnFailure(VCIEnsureBatchBindingKeysMatchSentProofKeys.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");
		callAndContinueOnFailure(VCIEnsureBatchBindingKeysAreDistinct.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3", "OID4VCI-1FINAL-3.3.2");

		// Time-claim unlinkability (RFC 9901 §10.1): within a batch the credentials are issued in the
		// same instant, so a precise iat/nbf (mdoc signed/validFrom) is an identical high-entropy
		// timestamp shared across the batch. The claims must instead be randomized or rounded.
		// The §10.1 MUST literally covers the SD-JWT iat/exp/nbf claims; for mdoc validityInfo the
		// basis is the section's privacy rationale only (and ISO 18013-5 defines 'signed' as the MSO
		// signing time), so mdoc is checked at WARNING.
		callAndContinueOnFailure(VCIEnsureBatchTimeClaimsNotLinkable.class,
			"mso_mdoc".equals(format) ? ConditionResult.WARNING : ConditionResult.FAILURE, "SDJWT-10.1");

		// Token Status List unlinkability: each credential must have a distinct, unpredictable status
		// list index (HAIP §6.1 makes this a MUST for SD-JWT VCs only; Token Status List §12.5
		// RECOMMENDS it otherwise, including for mdoc), and the status list URI should be shared
		// rather than unique-per-credential (herd privacy).
		boolean haip = getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.VCI_HAIP;
		boolean mdoc = "mso_mdoc".equals(format);
		callAndContinueOnFailure(VCIEnsureBatchStatusReferencesAreDistinct.class, ConditionResult.FAILURE,
			mdoc ? new String[] {"OTSL-13.3"} : new String[] {"OTSL-13.3", "HAIP-6.1"});
		callAndContinueOnFailure(VCIEnsureBatchStatusListIndicesAreUnpredictable.class,
			haip && !mdoc ? ConditionResult.FAILURE : ConditionResult.WARNING,
			mdoc ? new String[] {"OTSL-12.5"} : new String[] {"HAIP-6.1", "OTSL-12.5"});
		callAndContinueOnFailure(VCIWarnBatchStatusListUrisProvideHerdPrivacy.class, ConditionResult.WARNING, "OTSL-12.2", "OTSL-12.5");

		eventLog.endBlock();
	}
}
