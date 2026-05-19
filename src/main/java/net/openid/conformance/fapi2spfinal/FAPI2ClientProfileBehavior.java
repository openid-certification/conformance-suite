package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddIdTokenSigningAlgsToServerConfiguration;
import net.openid.conformance.condition.as.AddSubjectTypesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FAPI2AddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.GenerateAccessTokenExpiration;
import net.openid.conformance.condition.rs.ExtractFapiInteractionIdHeader;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Function;

/**
 * Base class for FAPI2 client-test profile-specific behavior. Provides default (plain FAPI)
 * behavior. Subclasses override methods to customize behavior for specific profiles
 * (OpenBanking UK, Consumer Data Right AU, OpenBanking Brazil, ConnectID AU, CBUAE,
 * Client Credentials Grant) and for VCI/HAIP wallet tests.
 *
 * <p>This is the client-side counterpart of {@link FAPI2ProfileBehavior}: the test suite
 * here emulates the authorization server, so the hooks describe how to <em>respond</em> to
 * the relying party (the client/wallet under test) in a profile-specific way.
 *
 * <p>Methods that return {@link ConditionSequence} may return {@code null} for no-op.
 * Methods that return a routed HTTP response object may return {@code null} to indicate
 * "this profile does not handle the path", in which case the abstract module continues
 * with its default routing.
 */
public class FAPI2ClientProfileBehavior {

	protected AbstractFAPI2SPFinalClientTest module;

	public void setModule(AbstractFAPI2SPFinalClientTest module) {
		this.module = module;
	}

	// --- Boolean predicates ---

	/** Whether mTLS is required on every endpoint (UK / CDR / Brazil / ConnectID / CBUAE). */
	public boolean requiresMtlsEverywhere() {
		return false;
	}

	/** Whether this profile only does the client_credentials grant (no auth code flow). */
	public boolean isClientCredentialsGrantOnly() {
		return false;
	}

	/**
	 * Whether the userinfo endpoint is treated as the resource endpoint, terminating the
	 * test on success. ConnectID and CBUAE return {@code true}.
	 */
	public boolean userInfoIsResourceEndpoint() {
		return false;
	}

	/**
	 * Whether the token endpoint needs the raw {@code incoming_request} mapped (in addition
	 * to {@code token_endpoint_request}) so that profile-specific header checks can run.
	 * ConnectID returns {@code true} (FAPI interaction id). DPoP also requires this and is
	 * handled by the abstract module independently of the profile.
	 */
	public boolean tokenEndpointRequiresIncomingRequest() {
		return false;
	}

	// --- Server configuration setup ---

	/**
	 * Profile-specific server configuration: signing alg, grant types, claims_supported,
	 * Brazil-specific settings, ConnectID verified_claims/trust frameworks, CC-grant types.
	 *
	 * <p>The default extracts the standard server signing alg.
	 */
	public ConditionSequence addProfileSpecificServerConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(ExtractServerSigningAlg.class);
			}
		};
	}

	/**
	 * OIDC subject_types_supported (only invoked when FAPIClientType=OIDC). Default adds
	 * the standard public subject type. ConnectID overrides to add pairwise as part of
	 * {@link #addProfileSpecificServerConfiguration()} and returns {@code null} here.
	 */
	public ConditionSequence addOidcSubjectTypesSupported() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(AddSubjectTypesSupportedToServerConfiguration.class).requirement("OIDCD-3"));
			}
		};
	}

	/**
	 * The condition that adds {@code token_endpoint_auth_signing_alg_values_supported} to
	 * the server configuration. Brazil overrides with the Brazil-specific variant.
	 */
	public Class<? extends Condition> getTokenEndpointAuthSigningAlgsCondition() {
		return FAPI2AddTokenEndpointAuthSigningAlgValuesSupportedToServer.class;
	}

	/**
	 * Additional setup that runs before the auth code grant chain (id_token signing algs,
	 * PAR endpoint registration, userinfo loading). The default chain is appropriate for
	 * profiles that do the auth code grant. Client-credentials-only profiles override to
	 * skip these.
	 */
	public ConditionSequence addAuthCodeGrantServerConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AddIdTokenSigningAlgsToServerConfiguration.class);
			}
		};
	}

	/**
	 * Whether to register the PAR endpoint in the server configuration. Defaults to true;
	 * client-credentials-only profiles override to {@code false}.
	 */
	public boolean shouldRegisterPAREndpoint() {
		return true;
	}

	/**
	 * Additional server configuration steps that depend on the resolved {@link
	 * net.openid.conformance.variant.ClientAuthType}. Runs after
	 * {@code addTokenEndpointAuthMethodSupported} and before PAR / token-endpoint-auth
	 * signing algs are registered.
	 *
	 * <p>VCI overrides for {@code client_attestation}: validates the required config
	 * fields are present, registers the client-attestation trust anchor, and adds the
	 * client-attestation signing alg values to the server configuration.
	 */
	public ConditionSequence additionalServerConfiguration() {
		return null;
	}

	/**
	 * Whether to load userinfo (only meaningful for auth-code-flow profiles that may issue
	 * id_tokens). Defaults to true; client-credentials-only profiles override to false.
	 */
	public boolean shouldLoadUserInfo() {
		return true;
	}

	// --- Endpoint exposure ---

	/**
	 * Expose the profile-specific resource endpoint paths to the user. Default exposes the
	 * standard {@code accounts_endpoint} (mtls or plain depending on sender constraint).
	 */
	public void exposeProfileEndpoints() {
		if (module.isMTLSConstrain()) {
			module.exposeMtlsPath("accounts_endpoint", AbstractFAPI2SPFinalClientTest.ACCOUNTS_PATH);
		} else {
			module.exposePath("accounts_endpoint", AbstractFAPI2SPFinalClientTest.ACCOUNTS_PATH);
		}
	}

	// --- Client config validation ---

	/**
	 * Profile-specific client configuration validation. ConnectID overrides to force the
	 * configured scope to {@code openid}.
	 */
	public ConditionSequence validateClientConfiguration() {
		return null;
	}

	// --- HTTP request routing ---

	/**
	 * Whether this profile owns the given path on the plain (non-mtls) endpoint.
	 * UK overrides for {@code account-requests}.
	 */
	public boolean claimsHttpPath(String path) {
		return false;
	}

	/**
	 * Handle a profile-specific path on the plain (non-mtls) endpoint. Only invoked when
	 * {@link #claimsHttpPath(String)} returned {@code true}.
	 *
	 * <p>Two contracts exist for path handlers:
	 * <ul>
	 *   <li>The traditional imperative form here ({@link #handleProfileSpecificPath}),
	 *       returning the HTTP response object directly. OB-UK uses this.</li>
	 *   <li>The newer declarative form ({@link #getProfileSpecificPathDispatch}) returning
	 *       a {@link PathDispatch} (a {@code ConditionSequence} + a response-builder
	 *       function), letting the test module drive execution. VCI uses this so its
	 *       behavior never calls {@code module.do*}.</li>
	 * </ul>
	 * {@code AbstractFAPI2SPFinalClientTest.handleHttp} consults the declarative form
	 * first; if it returns {@code null}, it falls back to this imperative form.
	 */
	public Object handleProfileSpecificPath(String requestId, String path) {
		throw new IllegalStateException("Profile did not claim path: " + path);
	}

	/**
	 * Declarative profile-specific path handler. Return a {@link PathDispatch} bundling
	 * the {@code ConditionSequence} that does the work and a {@code responseBuilder}
	 * function that produces the HTTP response from environment state after the sequence
	 * has run. Returning {@code null} (the default) lets the test module fall back to
	 * {@link #handleProfileSpecificPath}. Only invoked when {@link #claimsHttpPath} returned true.
	 */
	public PathDispatch getProfileSpecificPathDispatch(String requestId, String path) {
		return null;
	}

	/**
	 * Bundle of "what condition sequence to run for this path" and "how to build the
	 * HTTP response from env state once it's done". The {@code responseBuilder}'s
	 * argument is the {@code AbstractFAPI2SPFinalClientTest} so the builder has access
	 * to the env, the test module's helpers (e.g. {@code scheduleDelayedFinishForAdditionalRequests}),
	 * and any side effects it needs to perform after the sequence finishes.
	 */
	public record PathDispatch(
		ConditionSequence sequence,
		Function<AbstractFAPI2SPFinalClientTest, Object> responseBuilder
	) {
	}

	/**
	 * Handle a profile-specific well-known path (e.g. {@code /.well-known/openid-credential-issuer}).
	 * Return {@code null} if the profile does not claim the path; the abstract module then falls
	 * through to its default well-known routing (which handles {@code /.well-known/oauth-authorization-server}).
	 *
	 * <p>VCI overrides to serve credential issuer metadata so HAIP wallets can perform their
	 * standard credential-issuer-first discovery against our emulated AS.
	 */
	public Object handleProfileSpecificWellKnown(String path) {
		return null;
	}

	/**
	 * Whether this profile owns the given path on the mtls endpoint.
	 * Brazil overrides for consents / payments / payment-initiation paths.
	 */
	public boolean claimsHttpMtlsPath(String path) {
		return false;
	}

	/**
	 * Handle a profile-specific path on the mtls endpoint. Only invoked when
	 * {@link #claimsHttpMtlsPath(String)} returned {@code true}.
	 */
	public Object handleProfileSpecificMtlsPath(String requestId, String path) {
		throw new IllegalStateException("Profile did not claim mtls path: " + path);
	}

	// --- Resource endpoint header validation ---

	/**
	 * Extract the FAPI interaction id from a resource request. ConnectID overrides to make
	 * it mandatory (FAILURE if missing); the default treats it as optional (skip-if-missing).
	 */
	public ConditionSequence extractFapiInteractionIdHeader() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(ExtractFapiInteractionIdHeader.class)
					.skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id")
					.onSkip(ConditionResult.INFO)
					.onFail(ConditionResult.FAILURE)
					.requirements("FAPI2-IMP-2.1.1")
					.dontStopOnFailure());
			}
		};
	}

	// --- PAR endpoint hooks ---

	/** Additional checks on the PAR request (ConnectID checks for unexpected params). */
	public ConditionSequence additionalParRequestChecks() {
		return null;
	}

	/** Validate FAPI interaction id on the PAR request (ConnectID only). */
	public ConditionSequence validateParRequestInteractionId() {
		return null;
	}

	// --- Token endpoint hooks ---

	/** Validate FAPI interaction id on the token request (ConnectID only). */
	public ConditionSequence validateTokenRequestInteractionId() {
		return null;
	}

	/**
	 * Whether this profile supports the {@code client_credentials} grant at the token
	 * endpoint. UK / Brazil / FAPI_CLIENT_CREDENTIALS_GRANT override to {@code true}.
	 * The abstract module raises a TestFailureException if a profile that returns
	 * {@code false} receives a client_credentials request.
	 */
	public boolean supportsClientCredentialsGrant() {
		return false;
	}

	/**
	 * Steps that run before the standard client-credentials grant flow. Brazil overrides
	 * to extract the requested scope from the request prior to grant processing.
	 */
	public ConditionSequence preClientCredentialsGrantSetup() {
		return null;
	}

	// --- Authorization endpoint hooks ---

	/**
	 * Profile-specific extra checks on the authorization request (ConnectID checks for
	 * verified_claims and FAPI2 claims structure).
	 */
	public ConditionSequence additionalAuthorizationRequestChecks() {
		return null;
	}

	/**
	 * Profile-specific customization at the end of the authorization endpoint flow
	 * (Brazil moves consent status to AUTHORISED).
	 */
	public ConditionSequence customizeAuthorizationEndpoint() {
		return null;
	}

	// --- Request object validation ---

	/**
	 * Validate id_token ACR claims in the request object (only invoked for OIDC clients).
	 * Brazil overrides with the Brazil-specific ACR validator.
	 */
	public ConditionSequence validateIdTokenAcrClaims() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(net.openid.conformance.condition.client.FAPIValidateRequestObjectIdTokenACRClaims.class)
					.onFail(ConditionResult.INFO)
					.requirements("OIDCC-5.5.1.1")
					.dontStopOnFailure());
			}
		};
	}

	/**
	 * Validate request-object exp/nbf claims. ConnectID overrides with the AU-specific
	 * stricter exp/nbf validators.
	 */
	public ConditionSequence validateRequestObjectExpNbf() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(net.openid.conformance.condition.as.FAPIValidateRequestObjectExp.class,
					"RFC7519-4.1.4", "FAPI2-MS-ID1-5.3.1-4");
				callAndContinueOnFailure(net.openid.conformance.condition.as.FAPI1AdvancedValidateRequestObjectNBFClaim.class,
					ConditionResult.FAILURE, "FAPI2-MS-ID1-5.3.1-3");
			}
		};
	}

	/**
	 * Validate scope-related aspects of the authorization request. Default ensures the
	 * requested scope equals the configured scope. Brazil validates the consent scope and
	 * payments/accounts inclusion. ConnectID adds purpose and identity-claim checks.
	 */
	public ConditionSequence validateAuthorizationRequestScope() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(net.openid.conformance.condition.as.EnsureRequestedScopeIsEqualToConfiguredScope.class);
			}
		};
	}

	// --- Token issuance hooks ---

	/**
	 * The condition that generates the access-token expiration. ConnectID overrides with
	 * a profile-specific generator (different default lifetime).
	 */
	public Class<? extends Condition> getGenerateAccessTokenExpirationCondition() {
		return GenerateAccessTokenExpiration.class;
	}

	/**
	 * Profile-specific id_token claim customization that runs <em>immediately after</em>
	 * {@code GenerateIdTokenClaims} and before c_hash / s_hash / at_hash are added.
	 * Brazil overrides to add CPF/CPNJ claims here.
	 */
	public ConditionSequence customizeIdTokenClaimsAfterGenerate() {
		return null;
	}

	/**
	 * Profile-specific id_token claim customization that runs <em>after</em> at_hash has
	 * been added but <em>before</em> the test module's {@code addCustomValuesToIdToken()}
	 * hook. ConnectID overrides to load requested id_token claims here.
	 */
	public ConditionSequence customizeIdTokenClaimsAfterHashes() {
		return null;
	}

	/**
	 * The condition class used to add the ACR claim to the id_token. Brazil overrides with
	 * the Brazil-specific class.
	 */
	public Class<? extends Condition> getAddAcrClaimToIdTokenClaimsCondition() {
		return net.openid.conformance.condition.as.AddACRClaimToIdTokenClaims.class;
	}

	// --- Userinfo endpoint hooks ---

	/**
	 * Profile-specific customization of the userinfo response (ConnectID adds interaction id,
	 * Brazil adds CPF/CPNJ).
	 */
	public ConditionSequence customizeUserInfoResponse() {
		return null;
	}

	// --- Accounts endpoint hooks ---

	/**
	 * Profile-specific request validation at the accounts endpoint (Brazil checks scope
	 * and rejects payment-consent-initiated calls).
	 */
	public ConditionSequence validateAccountsEndpointRequest() {
		return null;
	}

	// --- Step sequence wiring (currently driven by @VariantSetup on the module) ---

	/**
	 * Steps to add to the id_token at the token endpoint (UK adds OB-UK claims).
	 * Returns {@code null} for profiles that don't customize.
	 */
	public Class<? extends ConditionSequence> getAuthorizationCodeGrantTypeProfileSteps() {
		return null;
	}

	/**
	 * Steps to add to the id_token at the authorization endpoint (UK adds OB-UK claims).
	 */
	public Class<? extends ConditionSequence> getAuthorizationEndpointProfileSteps() {
		return null;
	}

	/**
	 * Steps to customize the accounts endpoint response (UK and Brazil generate
	 * profile-specific account payloads).
	 */
	public Class<? extends ConditionSequence> getAccountsEndpointProfileSteps() {
		return null;
	}
}
