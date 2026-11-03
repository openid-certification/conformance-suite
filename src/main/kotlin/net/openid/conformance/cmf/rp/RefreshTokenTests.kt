package net.openid.conformance.cmf.rp

import com.google.gson.JsonObject
import net.openid.conformance.condition.`as`.LogAccessTokenAlwaysRejectedToForceARefreshGrant
import net.openid.conformance.condition.`as`.RemoveIssuedAccessTokenFromEnvironment
import net.openid.conformance.condition.`as`.ValidateRefreshToken
import net.openid.conformance.condition.common.GrantManagementSupport
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.testmodule.Environment
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import java.util.function.Predicate

/** How many times the client has used its refresh token. */
private const val REFRESH_TOKEN_GRANTS = "refresh_token_grants"

private const val ACCOUNTS_RESPONSE = "accounts_endpoint_response"

@PublishTestModule(
	testName = "cmf-chile-rp-refresh-token",
	displayName = "Chile CMF RP: client refresh token test",
	summary = "Tests a refresh token flow; the client should perform OpenID discovery from the displayed discoveryUrl, " +
		"push a signed authorization request to the PAR endpoint, call the authorization endpoint (which will " +
		"immediately redirect back), exchange the authorization code for an access token at the token endpoint and " +
		"make a GET request to the accounts endpoint displayed. This call will always return a 401 error, the client " +
		"must call the token endpoint again using the refresh_token grant type twice (the first call will return a new " +
		"refresh token) to obtain a new access token and call the accounts endpoint again with the new access token " +
		"obtained using the refresh_token.",
	profile = CMF_RP_PROFILE
)
class CMFChileRPRefreshToken : AbstractCMFChileRPTestModule() {

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			tokenEndpoint = Steps.modify(server.tokenEndpoint)
				// the access token from the authorization code grant must not be accepted
				.insertAfter(
					GrantManagementSupport.AddGrantIdToTokenEndpointResponse::class.java,
					condition(RemoveIssuedAccessTokenFromEnvironment::class.java)
				)
				.insertAfter(ValidateRefreshToken::class.java, Steps.action("Count refresh token grants") { env ->
					env.putInteger(REFRESH_TOKEN_GRANTS, refreshTokenGrants(env) + 1)
				})
				.build(),
			accountsEndpoint = Steps.`when`(
				"the client has not yet used the refresh token twice",
				::refreshTokenNotYetUsedTwice,
				rejectedAccountsRequest(),
				Steps.of(
					null,
					Steps.action("Clear the rejection") { env -> env.removeNativeValue(ACCOUNTS_RESPONSE + HTTP_STATUS_SUFFIX) },
					server.accountsEndpoint
				)
			),
			accountsEndpointFinishesTest = Predicate { env -> !refreshTokenNotYetUsedTwice(env) }
		)
	}

	private fun refreshTokenGrants(env: Environment): Int = env.getInteger(REFRESH_TOKEN_GRANTS) ?: 0

	private fun refreshTokenNotYetUsedTwice(env: Environment): Boolean = refreshTokenGrants(env) < 2

	/** A 401 telling the client its access token is invalid, without checking the request. */
	private fun rejectedAccountsRequest(): TestExecutionUnit =
		Steps.block(
			"Accounts endpoint (always rejected)",
			condition(LogAccessTokenAlwaysRejectedToForceARefreshGrant::class.java),
			Steps.action("Reject the access token") { env ->
				env.storeResponseStatus(ACCOUNTS_RESPONSE, 401)
				val headers = JsonObject()
				headers.addProperty(
					"WWW-Authenticate",
					"Bearer realm=\"conformancesuite\", error=\"invalid_token\", error_description=\"Invalid access token. " +
						"This test requires you to obtain a new access token twice using the refresh_token\""
				)
				env.putObject(ACCOUNTS_RESPONSE + "_headers", headers)
			}
		)
}
