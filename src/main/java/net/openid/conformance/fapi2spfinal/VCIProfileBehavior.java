package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.vci10issuer.condition.VCIExtractTlsInfoFromCredentialIssuer;
import net.openid.conformance.vci10issuer.condition.VCIFetchOAuthorizationServerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIGetDynamicCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIParseCredentialIssuerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIResolveCredentialEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCISelectOAuthorizationServer;
import net.openid.conformance.vci10issuer.condition.VCISetDiscoveryUrlFromAuthorizationServer;
import net.openid.conformance.vci10issuer.condition.VCIValidateClientJWKsPrivatePart;
import net.openid.conformance.vci10issuer.condition.clientattestation.CreateClientAttestationJwt;
import net.openid.conformance.vci10issuer.condition.clientattestation.GenerateClientAttestationClientInstanceKey;

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
		return null;
	}

	@Override
	public ConditionSequence configureClientAttestation() {
		if (module.getVariant(ClientAuthType.class) != ClientAuthType.CLIENT_ATTESTATION) {
			return null;
		}
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
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
	public ConditionSequence validateResourceEndpointResponseHeaders(boolean isSecondClient) {
		// VCI does not validate FAPI-specific response headers
		return null;
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
}
