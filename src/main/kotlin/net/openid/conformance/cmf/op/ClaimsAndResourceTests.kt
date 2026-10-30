package net.openid.conformance.cmf.op

import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.*
import net.openid.conformance.condition.common.RARSupport
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.OIDFJSON
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.variant.FAPIOpenIDConnect
import net.openid.conformance.variant.VariantNotApplicable

@PublishTestModule(
	testName = "cmf-chile-op-access-token-type-header-case-sensitivity",
	displayName = "Chile CMF OP: test whether the resource endpoint's authorization header is case sensitive for token type",
	summary = "Tests whether the RS is case sensitive to the access token type in the token request.\n\nAs per https://www.rfc-editor.org/rfc/rfc9110#name-authentication-scheme the authentication scheme name must be treated as case insensitive.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPAccessTokenTypeHeaderCaseSensitivity : AbstractCMFChileOPTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = flow.beforeRedirect(),
				afterCallback = flow.afterCallback(
					postAuthorization = Steps.of(
						"Post authorization",
						flow.callTokenEndpoint,
						flow.verifyTokenEndpointResponse,
						stopOnFailure().requirements("RFC9110-11.1").condition(SetAccessTokenTypeToInvertedCase::class.java),
						flow.callResourceEndpoint
					)
				)
			)
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-test-claims-parameter-identity-claims",
	displayName = "Chile CMF OP: test requesting identity claims using the claims parameter",
	summary = "This is an optional test that will be skipped if the server's metadata does not indicate support for requesting identity claims using the claims parameter, as supporting these claims is not mandatory in the specification.\n\nThe test will request all claims listed in the claims_supported server metadata are returned in the id_token and userinfo (using a variety of different forms of request, and including requesting some random unknown claims that should be ignored), and will warn if any are not returned in either - the server is free to decide whether to only make them available via the id_token, via the userinfo endpoint (if it has one), or via both.\n\nThe test should be performed using a user which has all supported claims present on the server.",
	profile = CMF_OP_PROFILE
)
@VariantNotApplicable(parameter = FAPIOpenIDConnect::class, values = ["plain_oauth"])
class CMFChileOPTestClaimsParameterIdentityClaims : AbstractCMFChileOPTestModule() {

	override fun extraConfiguration(): TestExecutionUnit =
		Steps.of(
			"Claims parameter support",
			Steps.check("Check claims_parameter_supported is a boolean") { env, c ->
				val supported = env.getElementFromObject("server", "claims_parameter_supported")
				if (supported != null && !(supported.isJsonPrimitive && supported.asJsonPrimitive.isBoolean)) {
					throw c.error("'claims_parameter_supported' in the server metadata is not a boolean",
						mapOf("claims_parameter_supported" to supported))
				}
				c.logSuccess("'claims_parameter_supported' is absent or a boolean")
			},
			Steps.`when`(
				"server does not support the claims parameter",
				{ env -> env.getElementFromObject("server", "claims_parameter_supported")?.let { OIDFJSON.getBoolean(it) } != true },
				Steps.action("Skip the test") {
					fireTestSkipped("The 'claims_parameter_supported' server metadata indicates that it does not support the claims parameter (which is permitted behaviour), so claims behaviour cannot be tested.")
				}
			),
			stopOnFailure().requirements("OIDCC-5.1").condition(CheckIfOidcStandardClaimsSupported::class.java),
			Steps.`when`(
				"server supports no standard claims",
				{ env -> env.getBoolean(CheckIfOidcStandardClaimsSupported.envVarAtLeastOneOidcStandardClaimSupport) != true },
				Steps.action("Skip the test") {
					fireTestSkipped("The 'claims_supported' server metadata does not contain any standard OpenID Connect claims (which is permitted behaviour), and means claims behaviour cannot be tested.")
				}
			)
		)

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = Steps.modify(flow.beforeRedirect())
					.insertAfter(
						RARSupport.AddRARToAuthorizationEndpointRequest::class.java,
						Steps.of(
							"Request claims",
							stopOnFailure().requirements("OIDCC-5.1", "OIDCC-5.5", "OIDCD-3")
								.condition(AddAllSupportedStandardClaimsToAuthorizationEndpointRequestIdTokenAndUserinfoClaims::class.java),
							stopOnFailure().requirements("OIDCC-5.5").condition(AddRandomLocationClaimsToAuthorizationEndpointRequest::class.java)
						)
					)
					.build(),
				// the claims are checked in the id token and userinfo, instead of calling the resource endpoint
				afterCallback = flow.afterCallback(
					postAuthorization = Steps.of(
						"Post authorization",
						flow.callTokenEndpoint,
						flow.verifyTokenEndpointResponse,
						identityClaimsSteps()
					)
				)
			)
		)
	}

	private fun identityClaimsSteps(): TestExecutionUnit =
		Steps.of(
			"Identity claims",
			condition(ExtractIdentityClaimsFromIdToken::class.java),
			Steps.`when`(
				"server has a userinfo endpoint",
				{ env -> env.getElementFromObject("server", "userinfo_endpoint") != null },
				userinfoSteps(),
				Steps.check("No userinfo endpoint") { _, c ->
					c.log("No userinfo_endpoint in server metadata; skipping calling userinfo endpoint")
				}
			),
			continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCC-5.5")
				.condition(EnsureIdentityClaimsContainRequestedClaims::class.java),
			// not part of the standard id token checks, as it could be noisy
			continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCC-5.1")
				.condition(CheckForUnexpectedClaimsInIdToken::class.java)
		)

	private fun userinfoSteps(): TestExecutionUnit =
		Steps.block(
			"Call userinfo endpoint",
			condition(CreateEmptyResourceEndpointRequestHeaders::class.java),
			stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.4-2").condition(CallUserInfoEndpoint::class.java),
			onEndpointResponse(
				"userinfo_endpoint_response_full",
				continueOnFailure().condition(EnsureHttpStatusCodeIs200::class.java),
				continueOnFailure().requirements("OIDCC-5.3.2").condition(EnsureContentTypeJson::class.java)
			),
			condition(ExtractUserInfoFromUserInfoEndpointResponse::class.java),
			continueOnFailure().requirements("OIDCC-5.1").condition(ValidateUserInfoStandardClaims::class.java),
			continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCC-5.1").condition(CheckForUnexpectedClaimsInUserinfo::class.java),
			continueOnFailure().requirements("OIDCC-5.3.2").condition(EnsureUserInfoContainsSub::class.java),
			continueOnFailure().requirements("OIDCC-5.1").condition(EnsureUserInfoUpdatedAtValid::class.java),
			continueOnFailure().requirements("OIDCC-5.6.2").condition(EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources::class.java),
			mapKey("Use id_token as token_endpoint_id_token", "token_endpoint_id_token", "id_token"),
			continueOnFailure().requirements("OIDCC-5.3.2").condition(VerifyUserInfoAndIdTokenInTokenEndpointSameSub::class.java),
			condition(AddIdentityClaimsFromUserInfo::class.java)
		)
}
