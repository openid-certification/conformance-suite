package net.openid.conformance.ekyc.test.oidccore;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.ekyc.condition.client.EnsureVerifiedClaimsSupportedParameterIsTrue;
import net.openid.conformance.ekyc.condition.client.ValidateAttachmentsSupportedInServerConfiguration;
import net.openid.conformance.ekyc.condition.client.ValidateClaimsInVerifiedClaimsSupportedInServerConfiguration;
import net.openid.conformance.ekyc.condition.client.ValidateDigestAlgorithmsSupportedInServerConfiguration;
import net.openid.conformance.ekyc.condition.client.ValidateDocumentsMethodsSupportedInServerConfiguration;
import net.openid.conformance.ekyc.condition.client.ValidateDocumentsSupportedInServerConfiguration;
import net.openid.conformance.ekyc.condition.client.ValidateDocumentsValidationMethodsSupportedInServerConfiguration;
import net.openid.conformance.ekyc.condition.client.ValidateDocumentsVerificationMethodsSupportedInServerConfiguration;
import net.openid.conformance.ekyc.condition.client.ValidateElectronicRecordsSupportedInServerConfiguration;
import net.openid.conformance.ekyc.condition.client.ValidateEvidenceSupportedInServerConfiguration;
import net.openid.conformance.ekyc.condition.client.ValidateTrustFrameworksSupportedInServerConfiguration;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ekyc-server-happypath",
	displayName = "eKYC Happy Path Server Test",
	summary = "Request only one claim, selected from the list of claims_in_verified_claims_supported, " +
		"without requesting any other verification element and expect a happy path flow.",
	profile = "OIDCC",
	configurationFields = {
		"trust_framework",
		"verified_claim_names"
	}
)
public class EKYCHappyPathTest extends AbstractEKYCTestWithOIDCCore {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		validateEKYCSpecificServerConfiguration();
	}

	protected void validateEKYCSpecificServerConfiguration() {
		//IA-9, The OP MUST support the claims parameter and needs to publish this in its openid-configuration using the claims_parameter_supported element.
		callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "IA-9");
		//verified_claims_supported: Boolean value indicating support for verified_claims, i.e. the OpenID Connect for Identity Assurance extension.
		//Boolean value indicating support for verified_claims, i.e. the OpenID Connect for Identity Assurance extension
		//this must be true for eKYC tests
		callAndContinueOnFailure(EnsureVerifiedClaimsSupportedParameterIsTrue.class, Condition.ConditionResult.FAILURE, "IA-9");

		//trust_frameworks_supported: JSON array containing all supported trust frameworks.
		validateTrustFrameworksSupportedInServerConfiguration();

		//evidence_supported: JSON array containing all types of identity evidence the OP uses.
		validateEvidenceSupportedInServerConfiguration();

		//documents_supported: JSON array containing all identity documents utilized by the OP for identity verification.
		validateDocumentsSupportedInServerConfiguration();

		//documents_methods_supported: OPTIONAL. JSON array containing the validation & verification process the
		// OP supports (see @!predefined_values).
		validateDocumentsMethodsSupportedInServerConfiguration();

		validateDocumentsValidationMethodsSupportedInServerConfiguration();

		validateDocumentsVerificationMethodsSupportedInServerConfiguration();

		validateElectronicRecordsSupportedInServerConfiguration();

		//claims_in_verified_claims_supported: JSON array containing all claims supported within verified_claims.
		validateClaimsInVerifiedClaimsSupported();

		validateAttachmentsSupportedInServerConfiguration();

		validateDigestAlgorithmsSupportedInServerConfiguration();
	}

	protected void validateTrustFrameworksSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateTrustFrameworksSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-9");
	}

	protected void validateEvidenceSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateEvidenceSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-9");
	}

	protected void validateDocumentsSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateDocumentsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-9");
	}

	protected void validateDocumentsMethodsSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateDocumentsMethodsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-9");
	}

	protected void validateDocumentsValidationMethodsSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateDocumentsValidationMethodsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-9");
	}

	protected void validateDocumentsVerificationMethodsSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateDocumentsVerificationMethodsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-9");
	}

	protected void validateElectronicRecordsSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateElectronicRecordsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-9");
	}

	protected void validateClaimsInVerifiedClaimsSupported() {
		callAndContinueOnFailure(ValidateClaimsInVerifiedClaimsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-9");
	}

	protected void validateAttachmentsSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateAttachmentsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-9");
	}

	protected void validateDigestAlgorithmsSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateDigestAlgorithmsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-9");
	}

}
