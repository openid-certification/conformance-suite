package net.openid.conformance.fapi2baselineid2;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAllSupportedStandardClaimsToAuthorizationEndpointRequestIdTokenClaims;
import net.openid.conformance.condition.client.CheckClaimsSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckForUnexpectedClaimsInIdToken;
import net.openid.conformance.condition.client.CheckIfOidcStandardClaimsSupported;
import net.openid.conformance.condition.client.EnsureIdTokenContainsRequestedClaims;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "fapi2-baseline-id2-test-claims-parameter-idtoken-identity-claims",
	displayName = "FAPI2-Baseline-ID2: test requesting id_token identity claims using the claims parameter",
	summary = "This is an optional test that will be skipped if the server's metadata does not indicate support for requesting identity claims using the claims parameter, as supporting these claims is not mandatory in the specification.\n\nThe test will request all claims listed in the claims_supported server metadata are returned in the id_token (using a variety of different forms of request), and will warn if any are not returned - the server is not required to support returning the claims in the id_token as it may have elected to only make them available via the userinfo endpoint.\n\nThe test should be performed using a user which has all supported claims present on the server.",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPI2BaselineID2TestClaimsParameterIdTokenIdentityClaims extends AbstractFAPI2BaselineID2ServerTestModule {

	private boolean isClaimsParameterSupported() {
		JsonElement claimsSupportedEl = env.getElementFromObject("server", "claims_parameter_supported");
		boolean claimsSupported;
		if (claimsSupportedEl == null) {
			claimsSupported = false;
		} else if (!claimsSupportedEl.isJsonPrimitive() || !claimsSupportedEl.getAsJsonPrimitive().isBoolean()) {
			throw new TestFailureException(getId(), "'claims_parameter_supported' in the server metadata is not a boolean");
		} else {
			claimsSupported = OIDFJSON.getBoolean(claimsSupportedEl);
		}
		return claimsSupported;
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		boolean requireBasicClaims = false;

		if (requireBasicClaims) {
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");

			callAndContinueOnFailure(CheckClaimsSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
		} else {
			if (!isClaimsParameterSupported()) {
				fireTestSkipped("The 'claims_parameter_supported' server metadata indicates that it does not support the claims parameter (which is permitted behaviour), so claims behaviour cannot be tested.");
			}
			callAndStopOnFailure(CheckIfOidcStandardClaimsSupported.class, "OIDCC-5.1");
			if (!env.getBoolean(CheckIfOidcStandardClaimsSupported.envVarAtLeastOneOidcStandardClaimSupport)) {
				fireTestSkipped("The 'claims_supported' server metadata does not contain any standard OpenID Connect claims (which is permitted behaviour), and means claims behaviour cannot be tested.");
			}
		}

		return super.makeCreateAuthorizationRequestSteps()
			.then(condition(AddAllSupportedStandardClaimsToAuthorizationEndpointRequestIdTokenClaims.class).requirements("OIDCC-5.1", "OIDCC-5.5", "OIDCD-3"));
	}

	@Override
	protected void exchangeAuthorizationCode() {
		super.exchangeAuthorizationCode();

		callAndContinueOnFailure(EnsureIdTokenContainsRequestedClaims.class, Condition.ConditionResult.WARNING, "OIDCC-5.5");

		// We don't include this check in the more general PerformStandardIdTokenChecks as it could be pretty noisy
		callAndContinueOnFailure(CheckForUnexpectedClaimsInIdToken.class, Condition.ConditionResult.WARNING, "OIDCC-5.1");
	}

	@Override
	protected void requestProtectedResource() {
		// not necessary in this test
	}
}
