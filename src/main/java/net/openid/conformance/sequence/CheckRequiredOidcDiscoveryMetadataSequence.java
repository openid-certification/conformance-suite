package net.openid.conformance.sequence;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointScopesSupportedContainsOpenId;
import net.openid.conformance.condition.client.CheckDiscEndpointSubjectTypesSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpoint;
import net.openid.conformance.condition.client.CheckJwksUri;

/**
 * Checks the provider metadata fields OpenID Connect Discovery 1.0 section 3 marks as REQUIRED.
 *
 * <p>Only usable where the server under test is an OpenID Provider; an RFC 8414 authorization
 * server that does not implement OpenID Connect has no such obligation.
 *
 * <p>{@code issuer} is deliberately not included: every caller already checks it, and the check
 * differs between the OpenID Connect and plain OAuth flavours of the discovery document.
 *
 * <p>The {@code response_types_supported} and {@code id_token_signing_alg_values_supported}
 * checks are supplied by the caller, as the acceptable values are profile specific (FAPI, for
 * example, requires PS256/ES256 where plain OpenID Connect requires RS256). Pass {@code null}
 * for either where the caller already performs its own check, to avoid checking it twice.
 */
public class CheckRequiredOidcDiscoveryMetadataSequence extends AbstractConditionSequence {

	private final Class<? extends Condition> responseTypesSupportedCheck;
	private final Class<? extends Condition> idTokenSigningAlgValuesSupportedCheck;
	private String[] responseTypesSupportedRequirements = { "OIDCD-3" };
	private String[] idTokenSigningAlgValuesSupportedRequirements = { "OIDCD-3" };
	private boolean checkAuthorizationEndpoint = true;

	public CheckRequiredOidcDiscoveryMetadataSequence(Class<? extends Condition> responseTypesSupportedCheck,
													  Class<? extends Condition> idTokenSigningAlgValuesSupportedCheck) {
		this.responseTypesSupportedCheck = responseTypesSupportedCheck;
		this.idTokenSigningAlgValuesSupportedCheck = idTokenSigningAlgValuesSupportedCheck;
	}

	public CheckRequiredOidcDiscoveryMetadataSequence responseTypesSupportedRequirements(String... requirements) {
		this.responseTypesSupportedRequirements = requirements;
		return this;
	}

	public CheckRequiredOidcDiscoveryMetadataSequence idTokenSigningAlgValuesSupportedRequirements(String... requirements) {
		this.idTokenSigningAlgValuesSupportedRequirements = requirements;
		return this;
	}

	/**
	 * For a CIBA-only provider, which has no authorization endpoint and hence no
	 * {@code response_types_supported} either.
	 */
	public CheckRequiredOidcDiscoveryMetadataSequence withoutAuthorizationEndpointCheck() {
		this.checkAuthorizationEndpoint = false;
		return this;
	}

	@Override
	public void evaluate() {

		if (checkAuthorizationEndpoint) {
			callAndContinueOnFailure(CheckDiscEndpointAuthorizationEndpoint.class, ConditionResult.FAILURE, "OIDCD-3");
		}

		callAndContinueOnFailure(CheckDiscEndpointTokenEndpoint.class, ConditionResult.FAILURE, "OIDCD-3");

		callAndContinueOnFailure(CheckJwksUri.class, ConditionResult.FAILURE, "OIDCD-3");

		if (checkAuthorizationEndpoint && responseTypesSupportedCheck != null) {
			callAndContinueOnFailure(responseTypesSupportedCheck, ConditionResult.FAILURE, responseTypesSupportedRequirements);
		}

		callAndContinueOnFailure(CheckDiscEndpointSubjectTypesSupported.class, ConditionResult.FAILURE, "OIDCD-3");

		if (idTokenSigningAlgValuesSupportedCheck != null) {
			callAndContinueOnFailure(idTokenSigningAlgValuesSupportedCheck, ConditionResult.FAILURE, idTokenSigningAlgValuesSupportedRequirements);
		}

		// scopes_supported is only RECOMMENDED, but an OpenID Provider that publishes it must
		// list the openid scope it necessarily supports
		call(condition(CheckDiscEndpointScopesSupportedContainsOpenId.class)
			.skipIfElementMissing("server", "scopes_supported")
			.onFail(ConditionResult.FAILURE)
			.onSkip(ConditionResult.WARNING)
			.requirement("OIDCD-3")
			.dontStopOnFailure());
	}
}
