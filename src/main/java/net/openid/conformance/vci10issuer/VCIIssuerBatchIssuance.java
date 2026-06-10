package net.openid.conformance.vci10issuer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchBindingKeysAreDistinct;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchBindingKeysMatchSentProofKeys;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchMdocCredentialDatasetsMatch;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchSdJwtCredentialDatasetsMatch;
import net.openid.conformance.vci10issuer.condition.VCIEnsureBatchSdJwtDisclosureSaltsAreDistinct;
import net.openid.conformance.vci10issuer.condition.VCIEnsureNotMoreCredentialsThanRequestedProofs;
import net.openid.conformance.vci10issuer.condition.VCIExtractBatchMdocBindingKeys;
import net.openid.conformance.vci10issuer.condition.VCIExtractBatchSdJwtBindingKeys;
import net.openid.conformance.vci10issuer.condition.VCIPrepareBatchProofKeys;
import net.openid.conformance.vci10issuer.condition.VCIValidateBatchCredentialIssuanceMetadata;
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

		eventLog.endBlock();
	}
}
