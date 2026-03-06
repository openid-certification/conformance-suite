package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs2xx;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.ParseCredentialAsSdJwt;
import net.openid.conformance.condition.client.ParseMdocCredentialFromVCIIssuance;
import net.openid.conformance.condition.client.ValidateCredentialCnfJwkIsPublicKey;
import net.openid.conformance.condition.client.ValidateCredentialIsUnpaddedBase64Url;
import net.openid.conformance.condition.client.ValidateCredentialJWTHeaderTyp;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateCredentialJWTIssIsHttpsUri;
import net.openid.conformance.condition.client.ValidateCredentialJWTVct;
import net.openid.conformance.condition.client.ValidateMdocIssuerSignedSignature;
import net.openid.conformance.openid.federation.CallCredentialIssuerNonceEndpoint;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.condition.CheckCacheControlHeaderContainsNoStore;
import net.openid.conformance.vci10issuer.condition.VCICreateCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCICheckForDeferredCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIDetermineCredentialConfigurationTransferMethod;
import net.openid.conformance.vci10issuer.condition.VCIExtractCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIExtractNotificationIdFromCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIExtractTlsInfoFromCredentialIssuer;
import net.openid.conformance.vci10issuer.condition.VCIFetchOAuthorizationServerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIGenerateAttestationProof;
import net.openid.conformance.vci10issuer.condition.VCIGenerateJwtProof;
import net.openid.conformance.vci10issuer.condition.VCIGenerateKeyAttestationIfNecessary;
import net.openid.conformance.vci10issuer.condition.VCIGetDynamicCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCICheckKeyAttestationJwksIfKeyAttestationIsRequired;
import net.openid.conformance.vci10issuer.condition.VCICreateNotificationRequest;
import net.openid.conformance.vci10issuer.condition.VCIParseCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIResolveCredentialEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveCredentialProofTypeToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveNotificationEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveRequestedCredentialConfiguration;
import net.openid.conformance.vci10issuer.condition.VCISelectOAuthorizationServer;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialNonceResponse;
import net.openid.conformance.vci10issuer.condition.VCIValidateNoUnknownKeysInCredentialResponse;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;

/**
 * VCI (OpenID for Verifiable Credential Issuance) profile behavior for FAPI2 tests.
 *
 * This profile adapts the FAPI2 test flow to test VCI credential issuers by:
 * - Fetching credential issuer metadata and deriving the authorization server from it
 * - Using the credential endpoint as the protected resource
 * - Calling the nonce endpoint and generating proofs before credential requests
 * - Validating credential responses (SD-JWT VC or mdoc format)
 */
public class VCIProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public void fetchServerConfiguration(AbstractFAPI2SPFinalServerTestModule module) {
		// Fetch VCI credential issuer metadata first
		module.doStartBlock("Fetch Credential Issuer Metadata");
		module.doCallAndStopOnFailure(VCIGetDynamicCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
		module.doCallAndStopOnFailure(VCIParseCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
		module.doCallAndStopOnFailure(VCIExtractTlsInfoFromCredentialIssuer.class);
		module.doEndBlock();

		// Fetch OAuth authorization server metadata from VCI metadata
		module.doStartBlock("Fetch Authorization Server Metadata");
		module.doCallAndStopOnFailure(VCIFetchOAuthorizationServerMetadata.class, "OID4VCI-1FINAL-12.2.3", "RFC8414-3.1");
		module.doCallAndStopOnFailure(VCISelectOAuthorizationServer.class, "OID4VCI-1FINAL-12.2.3");
		module.doEndBlock();
		// CheckServerConfiguration is called by the parent configure() method
	}

	@Override
	public void configureAdditional(AbstractFAPI2SPFinalServerTestModule module) {
		// Resolve credential configuration from VCI metadata
		resolveCredentialConfiguration(module);
	}

	@Override
	public void setupResourceEndpoint(AbstractFAPI2SPFinalServerTestModule module) {
		// Use the credential endpoint from VCI metadata as the resource endpoint
		module.doCallAndStopOnFailure(VCIResolveCredentialEndpointToUse.class);
		module.doCallAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);
		module.getEnv().putString("credential_resource_url", module.getEnv().getString("resource", "resourceUrl"));
	}

	@Override
	public void requestProtectedResource(AbstractFAPI2SPFinalServerTestModule module) {
		// VCI credential endpoint flow replaces the standard resource endpoint call
		module.doStartBlock(module.doCurrentClientString() + "Prepare Credential endpoint requests");

		module.doCallAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		if (!module.isSecondClient()) {
			module.doCallAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");
			module.doCallAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
			module.doCallAndStopOnFailure(CreateRandomFAPIInteractionId.class);
			module.doCallAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "CDR-http-headers");
		}

		boolean mtlsRequired = module.doIsMtlsRequired();
		JsonObject mtls = null;
		if (!mtlsRequired) {
			mtls = module.getEnv().getObject("mutual_tls_authentication");
			module.getEnv().removeObject("mutual_tls_authentication");
		}
		module.doEndBlock();

		// Call nonce endpoint if cryptographic binding is required
		callNonceEndpointIfNeeded(module);

		// Generate proof and create credential request
		module.doStartBlock(module.doCurrentClientString() + "Call Credential Endpoint");
		generateProofAndCreateCredentialRequest(module);

		// Call the credential endpoint
		if (module.doIsDpop()) {
			module.doRequestProtectedResourceUsingDpop();
		} else {
			module.doCallAndStopOnFailure(CallProtectedResource.class, "OID4VCI-1FINAL-8", "FAPI2-SP-FINAL-5.3.4-2");
		}

		if (!mtlsRequired && mtls != null) {
			module.getEnv().putObject("mutual_tls_authentication", mtls);
		}

		module.doCall(module.doExec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		module.doEndBlock();

		// Verify credential response
		module.doStartBlock(module.doCurrentClientString() + "Verify Credential Endpoint Response");
		verifyCredentialResponse(module);
		module.doEndBlock();
	}

	private void resolveCredentialConfiguration(AbstractFAPI2SPFinalServerTestModule module) {
		String credConfigId = module.getEnv().getString("config", "vci.credential_configuration_id");
		if (credConfigId == null || credConfigId.isBlank()) {
			module.doFireTestSkipped("credential_configuration_id is missing from the VCI section in the test configuration");
			return;
		}
		module.getEnv().putString("vci_credential_configuration_id", credConfigId);
		module.doCallAndStopOnFailure(VCIResolveRequestedCredentialConfiguration.class);

		module.doCallAndStopOnFailure(VCIDetermineCredentialConfigurationTransferMethod.class);
		module.doCallAndStopOnFailure(VCIResolveCredentialProofTypeToUse.class);

		// Only check key attestation if cryptographic binding is required
		Boolean requiresCryptographicBinding = module.getEnv().getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			module.doCallAndStopOnFailure(VCICheckKeyAttestationJwksIfKeyAttestationIsRequired.class);
		}
	}

	private void callNonceEndpointIfNeeded(AbstractFAPI2SPFinalServerTestModule module) {
		Boolean requiresCryptographicBinding = module.getEnv().getBoolean("vci_requires_cryptographic_binding");

		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			module.doStartBlock(module.doCurrentClientString() + "Call credential issuer nonce endpoint");
			JsonElement nonceEndpointEl = module.getEnv().getElementFromObject("vci", "credential_issuer_metadata.nonce_endpoint");
			if (nonceEndpointEl != null) {
				module.doCallAndStopOnFailure(CallCredentialIssuerNonceEndpoint.class, "OID4VCI-1FINAL-7.1");
				module.doEndBlock();

				module.doStartBlock(module.doCurrentClientString() + "Verify Credential Nonce Endpoint Response");
				module.doCall(module.doExec().mapKey("endpoint_response", "nonce_endpoint_response"));
				module.doCallAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
				module.doCallAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.WARNING, "OID4VCI-1FINAL-7.2");
				module.doCallAndContinueOnFailure(CheckCacheControlHeaderContainsNoStore.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
				module.doCallAndStopOnFailure(VCIValidateCredentialNonceResponse.class, "OID4VCI-1FINAL-7.2");
			} else {
				module.doLog("Skipping nonce endpoint call - 'nonce_endpoint' not present in credential issuer metadata");
			}
			module.doEndBlock();
		} else {
			module.doLog("Skipping nonce endpoint call - credential configuration does not require cryptographic binding");
		}
	}

	private void generateProofAndCreateCredentialRequest(AbstractFAPI2SPFinalServerTestModule module) {
		// Set up credential endpoint request as POST with JSON
		module.getEnv().putString("resource", "resourceMethod", "POST");
		module.getEnv().putString("resource_endpoint_request_headers", "Content-Type", "application/json");

		Boolean requiresCryptographicBinding = module.getEnv().getBoolean("vci_requires_cryptographic_binding");

		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			// Generate key attestation if necessary
			module.doCallAndContinueOnFailure(VCIGenerateKeyAttestationIfNecessary.class, ConditionResult.FAILURE,
				"HAIPA-D.1", "OID4VCI-1FINALA-D.1");

			// Generate proof based on type
			String proofTypeKey = module.getEnv().getString("vci_proof_type_key");
			if ("jwt".equals(proofTypeKey)) {
				module.doCallAndStopOnFailure(VCIGenerateJwtProof.class, "OID4VCI-1FINALA-F.1");
			} else if ("attestation".equals(proofTypeKey)) {
				module.doCallAndStopOnFailure(VCIGenerateAttestationProof.class, "OID4VCI-1FINALA-F.3");
			}
		} else {
			module.doLog("Skipping proof generation - credential configuration does not require cryptographic binding");
		}

		// Create the credential request body
		module.doCallAndStopOnFailure(VCICreateCredentialRequest.class, "OID4VCI-1FINAL-8.2");

		JsonObject credentialRequestObject = module.getEnv().getObject("vci_credential_request_object");
		module.getEnv().putString("resource_request_entity", credentialRequestObject.toString());
	}

	private void verifyCredentialResponse(AbstractFAPI2SPFinalServerTestModule module) {
		module.doCallAndStopOnFailure(EnsureHttpStatusCodeIs200.class, "OID4VCI-1FINAL-8.3");
		module.doCallAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");

		module.doCallAndContinueOnFailure(VCIValidateNoUnknownKeysInCredentialResponse.class, ConditionResult.WARNING, "OID4VCI-1FINAL-8.3");

		// Check for deferred response
		module.doCallAndStopOnFailure(VCICheckForDeferredCredentialResponse.class, "OID4VCI-1FINAL-9");

		String isDeferredStr = module.getEnv().getString("deferred_credential_response");
		boolean isDeferred = "true".equals(isDeferredStr);

		if (isDeferred) {
			module.doLog("Deferred credential response detected - deferred flow not yet supported in FAPI2 VCI profile");
			// For now, just extract whatever is available
		}

		// Extract and validate credentials
		module.doCallAndStopOnFailure(VCIExtractCredentialResponse.class, "OID4VCI-1FINAL-8.3");

		JsonArray extractedCredentials = module.getEnv().getObject("extracted_credentials").getAsJsonArray("list");
		for (int i = 0; i < extractedCredentials.size(); i++) {
			String credential = OIDFJSON.getString(extractedCredentials.get(i));
			module.getEnv().putString("credential", credential);

			if (extractedCredentials.size() > 1) {
				module.doStartBlock(module.doCurrentClientString() + "Verify credential " + (i + 1) + " of " + extractedCredentials.size());
			} else {
				module.doStartBlock(module.doCurrentClientString() + "Verify credential");
			}

			verifyCredential(module);

			module.doEndBlock();
		}

		module.doCall(module.doExec().unmapKey("endpoint_response"));
		module.doCallAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, ConditionResult.FAILURE, "RFC7231-7.1.1.2");

		module.doSkipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.INFO,
			CheckForFAPIInteractionIdInResourceResponse.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");

		if (!module.isSecondClient()) {
			module.doSkipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.INFO,
				EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");
		}

		sendNotificationIfSupported(module);
	}

	private void verifyCredential(AbstractFAPI2SPFinalServerTestModule module) {
		Boolean requiresCryptographicBinding = module.getEnv().getBoolean("vci_requires_cryptographic_binding");

		// Determine credential format from the resolved credential configuration
		String format = module.getEnv().getString("vci_credential_configuration", "format");

		if ("mso_mdoc".equals(format)) {
			module.doCallAndContinueOnFailure(ValidateCredentialIsUnpaddedBase64Url.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2.4");
			module.doCallAndContinueOnFailure(ParseMdocCredentialFromVCIIssuance.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2");
			module.doCallAndContinueOnFailure(ValidateMdocIssuerSignedSignature.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2");
		} else {
			// Default: SD-JWT VC format
			module.doCallAndContinueOnFailure(ParseCredentialAsSdJwt.class, ConditionResult.FAILURE, "SDJWT-4");
			module.doCallAndContinueOnFailure(ValidateCredentialJWTIssIsHttpsUri.class, ConditionResult.FAILURE, "SDJWTVC-3.2.2.2");
			module.doCallAndContinueOnFailure(ValidateCredentialJWTIat.class, ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-5.2");
			module.doCallAndContinueOnFailure(ValidateCredentialJWTVct.class, ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-3.5");
			module.doCallAndContinueOnFailure(ValidateCredentialJWTHeaderTyp.class, ConditionResult.FAILURE, "SDJWTVC-3.2.1");
			if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
				module.doCallAndContinueOnFailure(ValidateCredentialCnfJwkIsPublicKey.class, ConditionResult.FAILURE, "SDJWT-4.1.2");
			}
		}
	}

	private void sendNotificationIfSupported(AbstractFAPI2SPFinalServerTestModule module) {
		module.doCall(module.doExec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		module.doCallAndContinueOnFailure(VCIExtractNotificationIdFromCredentialResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");
		module.doCall(module.doExec().unmapKey("endpoint_response"));

		String notificationId = module.getEnv().getString("notification_id");
		if (notificationId == null) {
			module.doLog("No notification_id in credential response, skipping notification");
			return;
		}

		module.doStartBlock(module.doCurrentClientString() + "Send Notification to Issuer");

		module.doCallAndStopOnFailure(VCIResolveNotificationEndpointToUse.class, "OID4VCI-1FINAL-12.2.4");
		module.doCallAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);

		module.getEnv().putString("resource", "resourceMethod", "POST");

		module.doCallAndStopOnFailure(VCICreateNotificationRequest.class, "OID4VCI-1FINAL-11.1");

		module.doCallAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		module.doCallAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		module.doCallAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);
		module.getEnv().putString("resource_endpoint_request_headers", "Content-Type", "application/json");

		if (module.doIsDpop()) {
			module.doRequestProtectedResourceUsingDpop();
		} else {
			module.doCallAndStopOnFailure(CallProtectedResource.class, "OID4VCI-1FINAL-11", "FAPI2-SP-FINAL-5.3.4-2");
		}
		module.doEndBlock();

		module.doStartBlock(module.doCurrentClientString() + "Validate Notification Response from Issuer");
		module.doCall(module.doExec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		module.doCallAndContinueOnFailure(EnsureHttpStatusCodeIs2xx.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-11.2");
		module.doCall(module.doExec().unmapKey("endpoint_response"));
		module.doEndBlock();

		// Restore the credential endpoint URL
		String credentialResourceUrl = module.getEnv().getString("credential_resource_url");
		module.getEnv().putString("resource", "resourceUrl", credentialResourceUrl);
		module.getEnv().putString("protected_resource_url", credentialResourceUrl);
	}
}
