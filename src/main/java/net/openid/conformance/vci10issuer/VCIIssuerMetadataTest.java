package net.openid.conformance.vci10issuer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointClientAttestationSigningAlgValuesSupported;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsAttestJwtClientAuth;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.variant.VCIProfile;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.vci10issuer.condition.VCIAuthorizationServerMetadataValidation;
import net.openid.conformance.vci10issuer.condition.VCICheckRequiredMetadataFields;
import net.openid.conformance.vci10issuer.condition.VCICredentialIssuerMetadataValidation;
import net.openid.conformance.vci10issuer.condition.VCIEnsureAuthorizationDetailsTypesSupportedContainOpenIdCredentialIfScopeIsMissing;
import net.openid.conformance.vci10issuer.condition.VCIEnsureHttpsUrlsMetadata;
import net.openid.conformance.vci10issuer.condition.VCIExtractTlsInfoFromCredentialIssuer;
import net.openid.conformance.vci10issuer.condition.VCIFetchOAuthorizationServerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIParseCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialIssuerUri;
import net.openid.conformance.vci10issuer.condition.VCIValidateFormatOfCredentialConfigurationsInMetadata;
import net.openid.conformance.vci10issuer.condition.VCIValidateNonceEndpointInIssuerMetadata;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-metadata-test",
	displayName = "OID4VCI 1.0: Issuer metadata test",
	summary = """
		This test case validates the metadata exposed by the credential issuer,
		as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification.

		This test will send a credential issuer metadata request with content-type: application/json.
		""",
	profile = "OID4VCI-1_0"
)
@VariantParameters({VCIClientAuthType.class, VCIProfile.class})
public class VCIIssuerMetadataTest extends AbstractVciTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Credential Issuer Metadata", this::fetchCredentialIssuerMetadata);

		eventLog.runBlock("Process Credential Issuer Metadata Response", () -> {

			call(exec().mapKey("endpoint_response", "credential_issuer_metadata_endpoint_response"));
			checkIssuerMetadataResponse();
			callAndStopOnFailure(VCIExtractTlsInfoFromCredentialIssuer.class);
			call(exec().unmapKey("endpoint_response"));
			checkIssuerMetadata();
		});

		eventLog.runBlock("Fetch OAuth Authorization Server Metadata", () -> {
			callAndStopOnFailure(VCIFetchOAuthorizationServerMetadata.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.3", "RFC8414-3.1");
			call(exec().mapKey("endpoint_response", "oauth_authorization_server_metadata_response"));
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.WARNING, "RFC8414-3.2");
			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Verify OAuth Authorization Server Metadata Response", () -> {

			JsonObject credentialIssuerMetadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
			JsonElement authorizationServersEL = credentialIssuerMetadata.get("authorization_servers");

			// Determine how many authorization servers we have metadata for
			String countStr = env.getString("vci", "authorization_servers.count");
			int serverCount = countStr != null ? Integer.parseInt(countStr) : (authorizationServersEL != null && authorizationServersEL.isJsonArray() ? authorizationServersEL.getAsJsonArray().size() : 1);

			for (int i = 0; i < serverCount; i++) {
				String authServerMetadataPath = String.format("authorization_servers.server%d.authorization_server_metadata", i);
				checkAuthServerMetadata(authServerMetadataPath);
			}
		});

		fireTestFinished();
	}

	protected void checkIssuerMetadata() {
		callAndContinueOnFailure(VCICheckRequiredMetadataFields.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.3");
		callAndContinueOnFailure(VCIEnsureHttpsUrlsMetadata.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.3");
		callAndContinueOnFailure(VCIValidateCredentialIssuerUri.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.1");
		callAndContinueOnFailure(VCICredentialIssuerMetadataValidation.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.3");

		if (vciProfile == VCIProfile.HAIP) {
			callAndContinueOnFailure(VCIValidateNonceEndpointInIssuerMetadata.class, Condition.ConditionResult.FAILURE, "HAIP-4.1-5");
			callAndContinueOnFailure(new VCIValidateFormatOfCredentialConfigurationsInMetadata(true), Condition.ConditionResult.WARNING, "OID4VCI-1FINALA-A.3.1", "OID4VCI-1FINALA-A.2", "HAIP-6");
		} else {
			callAndContinueOnFailure(new VCIValidateFormatOfCredentialConfigurationsInMetadata(false), Condition.ConditionResult.WARNING, "OID4VCI-1FINALA-A.3.1", "OID4VCI-1FINALA-A.1", "OID4VCI-1FINALA-A.2");
		}
	}

	protected void checkIssuerMetadataResponse() {
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC8414-3.2");
		callAndStopOnFailure(VCIParseCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
	}

	protected void checkAuthServerMetadata(String authServerMetadataPath) {
		env.runWithMapKey("current_auth_server_metadata_path", authServerMetadataPath, () -> {
			callAndStopOnFailure(VCIAuthorizationServerMetadataValidation.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.3", "OID4VCI-1FINAL-12.3");

			if (clientAuthType == VCIClientAuthType.CLIENT_ATTESTATION) {
				env.putObject("server", env.getElementFromObject("vci", authServerMetadataPath).getAsJsonObject());
				callAndContinueOnFailure(EnsureServerConfigurationSupportsAttestJwtClientAuth.class, Condition.ConditionResult.WARNING, "OAuth2-ATCA07-13.4");
				callAndContinueOnFailure(CheckDiscEndpointClientAttestationSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-10.1");
				env.removeObject("server");
			}

			callAndContinueOnFailure(VCIEnsureAuthorizationDetailsTypesSupportedContainOpenIdCredentialIfScopeIsMissing.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.4-2.11.2.2");
		});
	}
}
