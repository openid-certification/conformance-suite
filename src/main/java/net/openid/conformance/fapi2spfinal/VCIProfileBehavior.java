package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsPrivateKeyOrTlsClientOrAttestation;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.ParseCredentialAsSdJwt;
import net.openid.conformance.condition.client.ParseMdocCredentialFromVCIIssuance;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.ValidateCredentialCnfJwkIsPublicKey;
import net.openid.conformance.condition.client.ValidateCredentialIsUnpaddedBase64Url;
import net.openid.conformance.condition.client.ValidateCredentialJWTHeaderTyp;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateCredentialJWTIssIsHttpsUri;
import net.openid.conformance.condition.client.ValidateCredentialJWTVct;
import net.openid.conformance.condition.client.ValidateMdocIssuerSignedSignature;
import net.openid.conformance.openid.federation.CallCredentialIssuerNonceEndpoint;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.VCI1FinalCredentialFormat;
import net.openid.conformance.vci10issuer.condition.CheckCacheControlHeaderContainsNoStore;
import net.openid.conformance.vci10issuer.condition.VCICheckForDeferredCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCICheckKeyAttestationJwksIfKeyAttestationIsRequired;
import net.openid.conformance.vci10issuer.condition.VCIAddCredentialConfigurationIdToEnv;
import net.openid.conformance.vci10issuer.condition.VCICreateCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCIDetermineCredentialConfigurationTransferMethod;
import net.openid.conformance.vci10issuer.condition.VCIEnsureResolvedCredentialConfigurationMatchesSelection;
import net.openid.conformance.vci10issuer.condition.VCIEnsureScopePresentInCredentialConfigurationForHaip;
import net.openid.conformance.vci10issuer.condition.VCIExtractCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIExtractTlsInfoFromCredentialIssuer;
import net.openid.conformance.vci10issuer.condition.VCIFetchOAuthorizationServerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIGenerateAttestationProof;
import net.openid.conformance.vci10issuer.condition.VCIGenerateJwtProof;
import net.openid.conformance.vci10issuer.condition.VCIGenerateKeyAttestationIfNecessary;
import net.openid.conformance.vci10issuer.condition.VCIGetDynamicCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIParseCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIResolveCredentialEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveCredentialProofTypeToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveRequestedCredentialConfiguration;
import net.openid.conformance.vci10issuer.condition.VCISelectOAuthorizationServer;
import net.openid.conformance.vci10issuer.condition.VCISetDiscoveryUrlFromAuthorizationServer;
import net.openid.conformance.vci10issuer.condition.VCIValidateClientJWKsPrivatePart;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialNonceResponse;
import net.openid.conformance.vci10issuer.condition.VCIValidateNoUnknownKeysInCredentialResponse;
import net.openid.conformance.vci10issuer.condition.clientattestation.CallClientAttestationChallengeEndpoint;
import net.openid.conformance.vci10issuer.condition.clientattestation.CheckClientAttestationChallengeResponseForUnknownFields;
import net.openid.conformance.vci10issuer.condition.clientattestation.CreateClientAttestationJwt;
import net.openid.conformance.vci10issuer.condition.clientattestation.GenerateClientAttestationClientInstanceKey;
import net.openid.conformance.vci10issuer.condition.clientattestation.ValidateClientAttestationChallengeResponse;

import java.util.function.Supplier;

/**
 * Profile behavior for VCI (Verifiable Credentials Issuance) tests.
 *
 * Overrides FAPI2 behavior to:
 * - Fetch credential issuer metadata instead of standard OIDC/OAuth discovery
 * - Use VCI-specific client JWKs generation (allows missing JWKs)
 * - Use VCI-specific client JWKs validation (allows multiple signing keys)
 * - Configure credential encryption JWKs when encryption is enabled
 * - Configure client attestation keys when client_attestation auth type is used
 * - Set up the credential endpoint instead of a generic resource endpoint
 * - Skip FAPI-specific resource endpoint headers (auth date, interaction ID)
 */
public class VCIProfileBehavior extends FAPI2ProfileBehavior {

	protected VCI1FinalCredentialFormat credentialFormat;

	@Override
	public boolean shouldExtractRARFromConfig() {
		// VCI generates RAR from the credential configuration, not from user config
		return false;
	}

	@Override
	public void initializeVariants() {
		module.jarm = false;
		module.isPar = true;
		module.isOpenId = false;
		module.isSignedRequest = module.getVariant(FAPI2AuthRequestMethod.class) == FAPI2AuthRequestMethod.SIGNED_NON_REPUDIATION;
		module.isRarRequest = module.getVariant(AuthorizationRequestType.class) == AuthorizationRequestType.RAR;
		module.useDpopAuthCodeBinding = false;
		module.profileRequiresMtlsEverywhere = false;
		// VCI never uses client credentials grant
		module.clientCredentialsGrant = false;

		credentialFormat = module.getVariant(VCI1FinalCredentialFormat.class);
	}

	@Override
	public ConditionSequence fetchServerConfiguration(boolean isOpenId) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(VCIGetDynamicCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
				callAndStopOnFailure(VCIParseCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
				callAndStopOnFailure(VCIExtractTlsInfoFromCredentialIssuer.class);
				callAndStopOnFailure(VCIFetchOAuthorizationServerMetadata.class, ConditionResult.FAILURE,
					"OID4VCI-1FINAL-12.2.3", "RFC8414-3.1");
				callAndStopOnFailure(VCISelectOAuthorizationServer.class, ConditionResult.FAILURE,
					"OID4VCI-1FINAL-12.2.3");
			}
		};
	}

	@Override
	public ConditionSequence configureClientExtra() {
		// VCI client JWKs generation and encryption JWKs are handled in
		// AbstractVCIIssuerTestModule.configureClient() where they run before validation
		return configureCredentialConfigurationResolution(credentialFormat);
	}

	@Override
	public ConditionSequence configureClientAttestation() {
		if (module.getVariant(ClientAuthType.class) != ClientAuthType.CLIENT_ATTESTATION) {
			return null;
		}
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {

				// Only call challenge endpoint if server metadata advertises one
				call(condition(CallClientAttestationChallengeEndpoint.class)
					.skipIfElementMissing("server", "challenge_endpoint")
					.requirement("OAuth2-ATCA07-8"));

				call(exec().mapKey("endpoint_response", "challenge_endpoint_response"));
				call(condition(EnsureHttpStatusCodeIs200.class)
					.skipIfElementMissing("server", "challenge_endpoint")
					.requirement("OAuth2-ATCA07-8")
					.onFail(ConditionResult.FAILURE)
					.dontStopOnFailure());
				call(condition(EnsureContentTypeJson.class)
					.skipIfElementMissing("server", "challenge_endpoint")
					.requirement("OAuth2-ATCA07-8")
					.onFail(ConditionResult.WARNING)
					.dontStopOnFailure());
				call(condition(CheckCacheControlHeaderContainsNoStore.class)
					.skipIfElementMissing("server", "challenge_endpoint")
					.requirement("OAuth2-ATCA07-8")
					.onFail(ConditionResult.FAILURE)
					.dontStopOnFailure());
				call(condition(ValidateClientAttestationChallengeResponse.class)
					.skipIfElementMissing("server", "challenge_endpoint")
					.requirement("OAuth2-ATCA07-8"));
				call(condition(CheckClientAttestationChallengeResponseForUnknownFields.class)
					.skipIfElementMissing("server", "challenge_endpoint")
					.requirement("OAuth2-ATCA07-8")
					.onFail(ConditionResult.WARNING)
					.dontStopOnFailure());
				call(exec().unmapKey("endpoint_response"));

				callAndStopOnFailure(GenerateClientAttestationClientInstanceKey.class, ConditionResult.FAILURE,
					"OAuth2-ATCA07-1");
				callAndStopOnFailure(CreateClientAttestationJwt.class, ConditionResult.FAILURE,
					"OAuth2-ATCA07-1", "HAIP-4.3.1-2");
			}
		};
	}

	@Override
	public ConditionSequence validateClientJwksPrivatePart() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// VCI-specific validation that allows multiple signing keys for attestation proof type
				callAndStopOnFailure(VCIValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
			}
		};
	}

	@Override
	public ConditionSequence setupResourceEndpoint() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// Resolve credential endpoint URL from issuer metadata (not from user config)
				callAndStopOnFailure(VCIResolveCredentialEndpointToUse.class);
				// Copy resource.resourceUrl to protected_resource_url
				callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);
			}
		};
	}

	@Override
	public ConditionSequence addResourceEndpointProfileHeaders(boolean isSecondClient) {
		// VCI does not use FAPI-specific resource endpoint headers
		return null;
	}

	@Override
	public ConditionSequence setupResourceEndpointRequestBody() {
		// Call nonce endpoint before returning the sequence, since sequences
		// don't have direct env access for conditional logic
		callNonceEndpointIfNeeded();

		module.getEnv().putString("resource", "resourceMethod", "POST");
		module.getEnv().putString("resource", "resourceMediaType", "application/json");
		module.getEnv().putString("resource_endpoint_request_headers", "Content-Type", "application/json");

		generateProofAndPopulateCredentialRequest();

		// Return null — all work is done above via direct module calls
		return null;
	}

	protected void callNonceEndpointIfNeeded() {
		Boolean requiresCryptographicBinding = module.getEnv().getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			return;
		}
		if (module.getEnv().getElementFromObject("vci", "credential_issuer_metadata.nonce_endpoint") == null) {
			return;
		}
		module.doCallAndStopOnFailure(CallCredentialIssuerNonceEndpoint.class, "OID4VCI-1FINAL-7.1");

		module.getEnv().mapKey("endpoint_response", "nonce_endpoint_response");
		module.doCallAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		module.doCallAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.WARNING, "OID4VCI-1FINAL-7.2");
		module.doCallAndContinueOnFailure(CheckCacheControlHeaderContainsNoStore.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		module.doCallAndStopOnFailure(VCIValidateCredentialNonceResponse.class, "OID4VCI-1FINAL-7.2");
		module.getEnv().unmapKey("endpoint_response");
	}

	protected void generateProofAndPopulateCredentialRequest() {
		Boolean requiresCryptographicBinding = module.getEnv().getBoolean("vci_requires_cryptographic_binding");

		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			module.doCallAndContinueOnFailure(VCIGenerateKeyAttestationIfNecessary.class, ConditionResult.FAILURE,
				"HAIPA-D.1", "OID4VCI-1FINALA-D.1");

			String proofTypeKey = module.getEnv().getString("vci_proof_type_key");
			if ("jwt".equals(proofTypeKey)) {
				module.doCallAndStopOnFailure(VCIGenerateJwtProof.class, "OID4VCI-1FINALA-F.1");
			} else if ("attestation".equals(proofTypeKey)) {
				module.doCallAndStopOnFailure(VCIGenerateAttestationProof.class, "OID4VCI-1FINALA-F.3");
			}
		}

		module.doCallAndStopOnFailure(VCICreateCredentialRequest.class, "OID4VCI-1FINAL-8.2");

		JsonObject credentialRequestObject = module.getEnv().getObject("vci_credential_request_object");
		module.getEnv().putString("resource_request_entity", credentialRequestObject.toString());
	}

	@Override
	public ConditionSequence createUpdateResourceRequestSteps(
			Supplier<? extends ConditionSequence> createDpopForResourceEndpointSteps) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// Call nonce endpoint for a fresh nonce before regenerating the proof
				callNonceEndpointIfNeeded();
				generateProofAndPopulateCredentialRequest();
				if (createDpopForResourceEndpointSteps != null) {
					call(sequence(createDpopForResourceEndpointSteps));
				}
			}
		};
	}

	@Override
	public ConditionSequence validateResourceEndpointResponseHeaders(boolean isSecondClient) {
		return null;
	}

	@Override
	public void validateResourceEndpointResponse() {
		module.getEnv().mapKey("endpoint_response", "resource_endpoint_response_full");
		module.doCallAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");
		module.doCallAndContinueOnFailure(VCIValidateNoUnknownKeysInCredentialResponse.class, ConditionResult.WARNING, "OID4VCI-1FINAL-8.3");

		module.doCallAndStopOnFailure(VCICheckForDeferredCredentialResponse.class, "OID4VCI-1FINAL-9");
		module.doCallAndStopOnFailure(VCIExtractCredentialResponse.class, "OID4VCI-1FINAL-8.3");

		JsonArray extractedCredentials = module.getEnv().getObject("extracted_credentials").getAsJsonArray("list");
		for (int i = 0; i < extractedCredentials.size(); i++) {
			String credential = OIDFJSON.getString(extractedCredentials.get(i));
			module.getEnv().putString("credential", credential);

			String blockLabel = extractedCredentials.size() > 1
				? module.currentClientString() + "Verify credential " + (i + 1) + " of " + extractedCredentials.size()
				: module.currentClientString() + "Verify credential";
			module.getEventLog().startBlock(blockLabel);
			verifyCredential();
			module.getEventLog().endBlock();
		}
		module.getEnv().unmapKey("endpoint_response");
	}

	protected void verifyCredential() {
		Boolean requiresCryptographicBinding = module.getEnv().getBoolean("vci_requires_cryptographic_binding");
		String format = module.getEnv().getString("vci_credential_configuration", "format");

		if ("mso_mdoc".equals(format)) {
			module.doCallAndContinueOnFailure(ValidateCredentialIsUnpaddedBase64Url.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2.4");
			module.doCallAndContinueOnFailure(ParseMdocCredentialFromVCIIssuance.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2");
			module.doCallAndContinueOnFailure(ValidateMdocIssuerSignedSignature.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2");
		} else {
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

	@Override
	public Class<? extends AbstractCondition> getDiscoveryTokenEndpointAuthMethodsCheck() {
		return CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsPrivateKeyOrTlsClientOrAttestation.class;
	}

	// --- Discovery endpoint verification overrides ---

	@Override
	public ConditionSequence discoveryFetchServerConfiguration(boolean isOpenId) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(VCIGetDynamicCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
				callAndStopOnFailure(VCIParseCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");
				callAndStopOnFailure(VCIFetchOAuthorizationServerMetadata.class, ConditionResult.FAILURE,
					"OID4VCI-1FINAL-12.2.3", "RFC8414-3.1");
				callAndStopOnFailure(VCISelectOAuthorizationServer.class, ConditionResult.FAILURE,
					"OID4VCI-1FINAL-12.2.3");
			}
		};
	}

	@Override
	public ConditionSequence discoveryAfterServerConfigurationFetched() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(VCISetDiscoveryUrlFromAuthorizationServer.class);
			}
		};
	}

	protected ConditionSequence configureCredentialConfigurationResolution(VCI1FinalCredentialFormat vciCredentialFormat) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {

				callAndStopOnFailure(VCIAddCredentialConfigurationIdToEnv.class);
				call(exec().exposeEnvironmentString("credential_configuration_id"));

				callAndStopOnFailure(VCIResolveRequestedCredentialConfiguration.class, ConditionResult.FAILURE);
				callAndStopOnFailure(new VCIEnsureResolvedCredentialConfigurationMatchesSelection(vciCredentialFormat));

				// HAIP requires scope to be present for every credential configuration
				callAndContinueOnFailure(VCIEnsureScopePresentInCredentialConfigurationForHaip.class, ConditionResult.FAILURE, "HAIP-4.1", "HAIP-4.3");

				callAndStopOnFailure(VCIDetermineCredentialConfigurationTransferMethod.class, ConditionResult.FAILURE);
				callAndStopOnFailure(VCIResolveCredentialProofTypeToUse.class, ConditionResult.FAILURE);

				// Only check key attestation if cryptographic binding is required
				callAndStopOnFailure(VCICheckKeyAttestationJwksIfKeyAttestationIsRequired.class, ConditionResult.FAILURE);
			}
		};
	}

}
