package net.openid.conformance.vci10issuer;


import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationJwt;
import net.openid.conformance.condition.client.RegisterRegistrationCertificateTrustAnchor;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.sequence.client.SetupLoteFromConfiguration;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.vci10issuer.condition.CheckForUnexpectedParametersInIssuerInfo;
import net.openid.conformance.vci10issuer.condition.CheckForUnexpectedParametersInIssuerInfoRegistrarDataset;
import net.openid.conformance.vci10issuer.condition.VCICheckIssuerInfoRegistrationCertificateProvidesAttestations;
import net.openid.conformance.vci10issuer.condition.VCICheckIssuerInfoRegistrationCertificateRegistryFields;
import net.openid.conformance.vci10issuer.condition.VCIDecodeSignedCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIEnsureIssuerInfoContainsOneRegistrarDataset;
import net.openid.conformance.vci10issuer.condition.VCIEnsureIssuerInfoRegistrationCertificateHasAttestationProviderEntitlement;
import net.openid.conformance.vci10issuer.condition.VCIEnsureIssuerInfoRegistrationCertificateSubMatchesAccessCertificate;
import net.openid.conformance.vci10issuer.condition.VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIExtractIssuerInfoRegistrationCertificate;
import net.openid.conformance.vci10issuer.condition.VCIRequestSignedCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIValidateIssuerInfo;
import net.openid.conformance.vci10issuer.condition.VCIValidateIssuerInfoRegistrarDataset;
import net.openid.conformance.vci10issuer.condition.VCIValidateIssuerInfoRegistrationCertificateHeader;
import net.openid.conformance.vci10issuer.condition.VCIValidateIssuerInfoRegistrationCertificateSignature;
import net.openid.conformance.vci10issuer.condition.VCIValidateIssuerInfoRegistrationCertificateTimestamps;
import net.openid.conformance.vci10issuer.condition.VCIWarnIfCredentialConfigurationsNotInProvidesAttestations;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-metadata-test-signed",
	displayName = "OID4VCI 1.0: Signed Issuer metadata test",
	summary = """
		This test case validates the signed metadata exposed by the credential issuer,
		as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification.

		This test will send a credential issuer metadata request with content-type: application/jwt.
		The test execution is skipped if the credential issuer metadata response is not signed.

		When a PID, mDL or WRPRC providers LoTE URL is configured, the issuer metadata is checked
		against ETSI TS 119 472-3: it must be signed, and carry 'issuer_info' with the issuer's
		registrar dataset and, optionally, its registration certificate. Otherwise an 'issuer_info'
		the issuer sends is still checked, with problems reported as warnings.
		""",
	profile = "OID4VCI-1_0"
)
@VariantParameters({ClientAuthType.class, FAPI2FinalOPProfile.class})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {
	"plain_fapi", "consumerdataright_au", "openbanking_brazil",
	"connectid_au", "cbuae", "fapi_client_credentials_grant"
})
@ConfigurationFields({
	"credential.pid_lote_url",
	"credential.mdl_lote_url",
	"credential.wrprc_lote_url",
	"credential.registration_certificate_trust_anchor_pem",
	"lote.signing_certificate_pem",
})
public class VCIIssuerMetadataSignedTest extends VCIIssuerMetadataTest {

	/** Whether the issuance is under ETSI TS 119 472-3, which a configured list of trusted entities for issuers stands for. */
	private boolean etsiIssuance;

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		etsiIssuance = LoteSlot.PID.isConfigured(env) || LoteSlot.MDL.isConfigured(env) || LoteSlot.WRPRC.isConfigured(env);
		callAndStopOnFailure(RegisterRegistrationCertificateTrustAnchor.class);
		call(SetupLoteFromConfiguration.forConfigured(env, ConditionResult.FAILURE, LoteSlot.WRPRC));
	}

	@Override
	protected void fetchCredentialIssuerMetadata() {
		callAndStopOnFailure(VCIRequestSignedCredentialIssuerMetadata.class);
		super.fetchCredentialIssuerMetadata();
	}

	@Override
	protected void checkIssuerMetadataResponse() {

		String mimeType = AbstractCheckEndpointContentTypeReturned.getMimeTypeFromContentType(
			env.getString("endpoint_response", "headers.content-type"));
		if ("application/json".equalsIgnoreCase(mimeType)) {
			if (etsiIssuance) {
				callAndStopOnFailure(EnsureContentTypeApplicationJwt.class, "ETSI119472-3-4.2.1-01");
			}
			fireTestSkipped("Skipping test as credential issuer metadata response is not signed (content-type returned is application/json).");
			return;
		}

		callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, Condition.ConditionResult.FAILURE, "RFC8414-3.2");
		callAndStopOnFailure(VCIDecodeSignedCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
	}

	@Override
	protected void checkIssuerMetadata() {
		super.checkIssuerMetadata();
		checkIssuerInfo();
	}

	/**
	 * The ETSI TS 119 472-3 issuer_info checks: required under that profile, and otherwise run as
	 * warnings when the issuer sends issuer_info anyway.
	 */
	protected void checkIssuerInfo() {
		if (!etsiIssuance && env.getElementFromObject("vci", "credential_issuer_metadata_signed.issuer_info") == null) {
			return;
		}
		ConditionResult severity = etsiIssuance ? ConditionResult.FAILURE : ConditionResult.WARNING;

		callAndContinueOnFailure(VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.class, severity,
			"ETSI119472-3-4.2.3-01", "ETSI119472-3-4.2.3-02");
		issuerInfoCheck(VCIValidateIssuerInfo.class, VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY, severity,
			"ETSI119472-3-4.2.3-03", "OID4VP-1FINAL-5.1");
		issuerInfoCheck(CheckForUnexpectedParametersInIssuerInfo.class, VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY,
			ConditionResult.WARNING, "ETSI119472-3-4.2.3-03", "OID4VP-1FINAL-5.1");
		issuerInfoCheck(VCIEnsureIssuerInfoContainsOneRegistrarDataset.class, VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY, severity,
			"ETSI119472-3-4.2.3-04", "ETSI119472-3-4.2.3-07", "ETSI119472-3-4.2.3-08", "ETSI119472-3-4.2.3-09");

		issuerInfoCheck(VCIValidateIssuerInfoRegistrarDataset.class, VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY, severity,
			"ETSI119472-3-4.2.3-10", "ETSI119472-3-4.2.3-11", "ETSI119472-3-4.2.3-12", "ETSI119472-3-4.2.3-13");
		issuerInfoCheck(CheckForUnexpectedParametersInIssuerInfoRegistrarDataset.class, VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY,
			ConditionResult.WARNING, "ETSI119472-3-4.2.3-09");
		issuerInfoCheck(VCIWarnIfCredentialConfigurationsNotInProvidesAttestations.class, VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY,
			ConditionResult.WARNING, "ETSI119472-3-4.2.3");

		issuerInfoCheck(VCIExtractIssuerInfoRegistrationCertificate.class, VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY, severity,
			"ETSI119472-3-4.2.3-05", "ETSI119472-3-4.2.3-06");
		String certificate = VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY;
		issuerInfoCheck(VCIValidateIssuerInfoRegistrationCertificateHeader.class, certificate, severity, "ETSI119475-5.2.2-01");
		issuerInfoCheck(VCIValidateIssuerInfoRegistrationCertificateSignature.class, certificate, severity,
			"ETSI119475-5.2.1-01", "ETSI119475-5.2.1-03");
		issuerInfoCheck(VCIValidateIssuerInfoRegistrationCertificateTimestamps.class, certificate, severity,
			"ETSI119475-5.2.4-01", "ETSI119475-5.2.4-08");
		issuerInfoCheck(VCIEnsureIssuerInfoRegistrationCertificateHasAttestationProviderEntitlement.class, certificate, severity,
			"ETSI119475-5.2.4-01", "ETSI119475-5.2.4-03");
		issuerInfoCheck(VCIEnsureIssuerInfoRegistrationCertificateSubMatchesAccessCertificate.class, certificate, severity,
			"ETSI119475-5.2.4-02", "ETSI119472-3-4.2.1-02");
		issuerInfoCheck(VCICheckIssuerInfoRegistrationCertificateRegistryFields.class, certificate, ConditionResult.WARNING,
			"ETSI119475-5.2.4-01");
		issuerInfoCheck(VCICheckIssuerInfoRegistrationCertificateProvidesAttestations.class, certificate, ConditionResult.WARNING,
			"ETSI119475-5.2.4-05");
	}

	private void issuerInfoCheck(Class<? extends Condition> conditionClass, String requiredObject, ConditionResult severity, String... requirements) {
		call(condition(conditionClass)
			.skipIfObjectMissing(requiredObject)
			.onSkip(ConditionResult.INFO)
			.onFail(severity)
			.dontStopOnFailure()
			.requirements(requirements));
	}
}
