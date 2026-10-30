package net.openid.conformance.cmf.op

import net.openid.conformance.condition.Condition
import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.CallPAREndpoint
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage
import net.openid.conformance.condition.common.GrantManagementSupport
import net.openid.conformance.condition.common.RARSupport
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.Environment
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.variant.ConfigurationFields

/**
 * Modules that call the grant management endpoint, and so request the GM 6.1 scopes. The endpoint is called with
 * the client's certificate, as the Chile profile requires mTLS everywhere.
 */
abstract class AbstractCMFChileOPGrantManagementTestModule : AbstractCMFChileOPTestModule() {

	protected fun grantManagementFlow(client: OPClient = OPClient.FIRST): OPFlow =
		flow(client, needsGrantManagementApiScopes = true)

	protected fun grantManagementQuery(): TestExecutionUnit =
		Steps.of(
			"Query grant",
			condition(CreateEmptyResourceEndpointRequestHeaders::class.java),
			stopOnFailure().requirements("GM-6.4").condition(GrantManagementSupport.CallGrantManagementEndpointQuery::class.java)
		)

	protected fun grantManagementRevoke(): TestExecutionUnit =
		Steps.of(
			"Revoke grant",
			condition(CreateEmptyResourceEndpointRequestHeaders::class.java),
			stopOnFailure().requirements("GM-6.5").condition(GrantManagementSupport.CallGrantManagementEndpointRevoke::class.java)
		)

	/** The validation GM 6.4 calls for on a successful query response: the status, the headers and the body. */
	protected fun validateGrantManagementQueryResponse(): TestExecutionUnit =
		Steps.of(
			"Validate grant management query response",
			// GM 6.4 permits a 503 from a server under load, which is not a conformance failure, but leaves
			// nothing about the grant to check
			Steps.action("Skip when the grant management endpoint is under load") { env ->
				if (grantManagementStatus(env) == 503) {
					val retryAfter = env.getString(GrantManagementSupport.GRANT_MANAGEMENT_RESPONSE_KEY, "headers.retry-after")
					fireTestSkipped("The grant management endpoint returned HTTP 503, which GM 6.4 permits when the"
						+ " authorization server is under load. Please retry the test"
						+ (if (retryAfter.isNullOrEmpty()) "." else ", respecting the 'Retry-After' header of $retryAfter."))
				}
			},
			continueOnFailure().requirements("GM-6.4").condition(GrantManagementSupport.EnsureGrantManagementQuerySucceeded::class.java),
			// the remaining checks describe a successful response, and would only repeat the status failure
			Steps.`when`(
				"grant management query returned 200",
				{ env -> grantManagementStatus(env) == 200 },
				Steps.of(
					"Successful query response",
					continueOnFailure().requirements("GM-6.4")
						.condition(GrantManagementSupport.EnsureGrantManagementQueryResponseContentTypeIsJson::class.java),
					// GM 6.4 only shows Cache-Control in its non-normative examples
					continueOnFailure().onFail(ConditionResult.WARNING).requirements("GM-6.4")
						.condition(GrantManagementSupport.EnsureGrantManagementQueryResponseIsNotCacheable::class.java),
					continueOnFailure().requirements("GM-6.4")
						.condition(GrantManagementSupport.EnsureGrantManagementQueryResponseBodyIsJsonObject::class.java),
					queryBodyCheck(GrantManagementSupport.ValidateGrantManagementQueryResponseSchema::class.java, ConditionResult.FAILURE),
					queryBodyCheck(GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps::class.java, ConditionResult.FAILURE),
					// every member of the response is OPTIONAL per GM 6.4, so these can only warn
					queryBodyCheck(GrantManagementSupport.CheckGrantManagementQueryResponseContainsGrantDetails::class.java, ConditionResult.WARNING),
					queryBodyCheck(GrantManagementSupport.CheckForUnexpectedParametersInGrantManagementQueryResponse::class.java, ConditionResult.WARNING)
				)
			)
		)

	/** A check of the parsed query response body, skipped when there is none rather than failing once per check. */
	protected fun queryBodyCheck(condition: Class<out Condition>, onFail: ConditionResult): TestExecutionUnit =
		continueOnFailure().onFail(onFail).requirements("GM-6.4").condition(condition)
			.skipIfElementMissing(GrantManagementSupport.GRANT_MANAGEMENT_RESPONSE_KEY, "body_json")
			.onSkip(ConditionResult.INFO)

	private fun grantManagementStatus(env: Environment): Int? =
		env.getInteger(GrantManagementSupport.GRANT_MANAGEMENT_RESPONSE_KEY, "status")
}

@PublishTestModule(
	testName = "cmf-chile-op-grant-management-query-and-revoke",
	displayName = "Chile CMF OP: Grant Management - Query and Revoke",
	summary = "Performs a full authorization flow with grant_management_action=create, extracts the grant_id from the token response, queries the grant (expecting HTTP 200), revokes it (expecting HTTP 204), then queries again expecting HTTP 404.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPGrantManagementQueryAndRevoke : AbstractCMFChileOPGrantManagementTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = grantManagementFlow()
		return listOf(AuthorizationLeg(flow.beforeRedirect(), flow.afterCallback()))
	}

	override fun afterLastLeg(): TestExecutionUnit =
		Steps.block(
			"Grant Management: query, revoke, and query again",
			stopOnFailure().requirements("GM-6.3").condition(GrantManagementSupport.SetGrantManagementEndpointUrl::class.java),
			grantManagementQuery(),
			validateGrantManagementQueryResponse(),
			grantManagementRevoke(),
			continueOnFailure().requirements("GM-6.5").of(
				"Revoke response",
				GrantManagementSupport.EnsureGrantManagementRevokeSucceeded::class.java,
				GrantManagementSupport.EnsureGrantManagementRevokeResponseBodyIsEmpty::class.java
			),
			grantManagementQuery(),
			continueOnFailure().requirements("GM-6.6").condition(GrantManagementSupport.EnsureGrantManagementEndpointReturns404::class.java)
		)
}

/**
 * Two authorization flows: the first creates a grant, the second updates it with [actionCondition] and the grant
 * id, after which the grant must still be the same one and cover the access just issued.
 */
abstract class AbstractCMFChileOPGrantManagementUpdate : AbstractCMFChileOPGrantManagementTestModule() {

	/** Adds the grant_management_action that updates the grant. */
	protected abstract val actionCondition: Class<out Condition>

	/** Checks grant_management_actions_supported advertises the action. */
	protected abstract val actionSupportedCondition: Class<out Condition>

	protected abstract val actionName: String

	override fun legs(): List<AuthorizationLeg> {
		val flow = grantManagementFlow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = flow.beforeRedirect(),
				afterCallback = flow.afterCallback(
					postAuthorization = Steps.of(
						"Post authorization",
						flow.postAuthorization(),
						// the discovery check cannot require every action of every server, so check it where the
						// test depends on it
						continueOnFailure().requirements("GM-7.1").condition(actionSupportedCondition)
							.skipIfElementMissing("server", "grant_management_actions_supported").onSkip(ConditionResult.INFO),
						// the second flow's grant_id is compared against this one
						stopOnFailure().requirements("GM-5.5").condition(GrantManagementSupport.StoreGrantIdForComparison::class.java)
					)
				)
			),
			AuthorizationLeg(
				beforeRedirect = Steps.modify(flow.beforeRedirect())
					.replace(
						GrantManagementSupport.AddGrantManagementActionCreateToAuthorizationRequest::class.java,
						stopOnFailure().requirements("GM-5.2").condition(actionCondition)
					)
					.insertAfter(
						RARSupport.AddRARToAuthorizationEndpointRequest::class.java,
						stopOnFailure().requirements("GM-5.2").condition(GrantManagementSupport.AddGrantIdToAuthorizationRequest::class.java)
					)
					.build(),
				afterCallback = flow.afterCallback()
			)
		)
	}

	override fun afterLastLeg(): TestExecutionUnit =
		Steps.block(
			"Grant Management: query after $actionName",
			// the action updates the grant the request named, so the server must not have minted a new one
			continueOnFailure().onFail(ConditionResult.WARNING).requirements("GM-5.2")
				.condition(GrantManagementSupport.EnsureGrantIdIsUnchanged::class.java),
			stopOnFailure().requirements("GM-6.3").condition(GrantManagementSupport.SetGrantManagementEndpointUrl::class.java),
			grantManagementQuery(),
			validateGrantManagementQueryResponse(),
			// the grant has to cover the access just issued, otherwise the action did not take effect
			queryBodyCheck(GrantManagementSupport.CheckGrantManagementQueryResponseCoversGrantedScope::class.java, ConditionResult.WARNING)
		)
}

@PublishTestModule(
	testName = "cmf-chile-op-grant-management-merge",
	displayName = "Chile CMF OP: Grant Management Merge",
	summary = "Performs two authorization flows. The first uses grant_management_action=create to create a new grant. The second uses grant_management_action=merge with the previously issued grant_id. After the second token response, queries the grant endpoint to verify the grant is valid.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPGrantManagementMerge : AbstractCMFChileOPGrantManagementUpdate() {
	override val actionCondition: Class<out Condition> = GrantManagementSupport.AddGrantManagementActionMergeToAuthorizationRequest::class.java
	override val actionSupportedCondition: Class<out Condition> = GrantManagementSupport.CheckGrantManagementActionsSupportedContainsMerge::class.java
	override val actionName: String = "merge"
}

@PublishTestModule(
	testName = "cmf-chile-op-grant-management-replace",
	displayName = "Chile CMF OP: Grant Management Replace",
	summary = "Performs two authorization flows. The first uses grant_management_action=create to create a new grant. The second uses grant_management_action=replace with the previously issued grant_id. After the second token response, queries the grant endpoint to verify the grant is valid.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPGrantManagementReplace : AbstractCMFChileOPGrantManagementUpdate() {
	override val actionCondition: Class<out Condition> = GrantManagementSupport.AddGrantManagementActionReplaceToAuthorizationRequest::class.java
	override val actionSupportedCondition: Class<out Condition> = GrantManagementSupport.CheckGrantManagementActionsSupportedContainsReplace::class.java
	override val actionName: String = "replace"
}

@PublishTestModule(
	testName = "cmf-chile-op-grant-management-ensure-query-nonexistent-grant-fails",
	displayName = "Chile CMF OP: Grant Management - Ensure Query of Non-Existent Grant Returns 404",
	summary = "Performs a standard authorization flow to obtain a valid access token, then attempts to query the grant management endpoint using a fabricated grant_id. Expects the server to return HTTP 404.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPGrantManagementEnsureQueryNonExistentGrantFails : AbstractCMFChileOPGrantManagementTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = grantManagementFlow()
		return listOf(AuthorizationLeg(flow.beforeRedirect(), flow.afterCallback()))
	}

	override fun afterLastLeg(): TestExecutionUnit =
		Steps.block(
			"Grant Management: query non-existent grant",
			stopOnFailure().requirements("GM-5.5").condition(GrantManagementSupport.CreateGrantIdThatDoesNotExist::class.java),
			stopOnFailure().requirements("GM-6.3").condition(GrantManagementSupport.SetGrantManagementEndpointUrl::class.java),
			grantManagementQuery(),
			continueOnFailure().requirements("GM-6.6").condition(GrantManagementSupport.EnsureGrantManagementEndpointReturns404::class.java)
		)
}

/**
 * Client 1 creates a grant, then client 2 obtains its own access token and uses it on client 1's grant, which the
 * grant management endpoint must refuse with 403 or 404.
 */
@ConfigurationFields(
	"client2.client_id",
	"client2.scope",
	"client2.jwks",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
)
abstract class AbstractCMFChileOPGrantManagementWrongClient : AbstractCMFChileOPGrantManagementTestModule() {

	override val usesSecondClient: Boolean = true

	/** Calls the grant management endpoint on client 1's grant, as client 2. */
	protected abstract val grantManagementCall: TestExecutionUnit

	protected abstract val callDescription: String

	override fun legs(): List<AuthorizationLeg> {
		val first = grantManagementFlow(OPClient.FIRST)
		val second = grantManagementFlow(OPClient.SECOND)
		return listOf(
			AuthorizationLeg(
				beforeRedirect = first.beforeRedirect(),
				afterCallback = first.afterCallback(
					postAuthorization = Steps.of(
						"Post authorization",
						first.postAuthorization(),
						Steps.action("Keep client 1's grant_id") { env ->
							env.putString(CLIENT1_GRANT_ID_KEY, env.getString(GrantManagementSupport.GRANT_ID_KEY))
						}
					)
				)
			),
			AuthorizationLeg(
				beforeRedirect = Steps.of("Second client authorization request", secondClientSetupSteps(), second.beforeRedirect()),
				afterCallback = second.afterCallback()
			)
		)
	}

	// client 2's client, certificate and access token are still in use
	override fun afterLastLeg(): TestExecutionUnit =
		Steps.of(
			"Wrong client grant management call",
			Steps.action("Use client 1's grant_id") { env ->
				env.putString(GrantManagementSupport.GRANT_ID_KEY, env.getString(CLIENT1_GRANT_ID_KEY))
			},
			stopOnFailure().requirements("GM-6.3").condition(GrantManagementSupport.SetGrantManagementEndpointUrl::class.java),
			Steps.block(
				"Client 2 attempts to $callDescription Client 1's grant",
				grantManagementCall,
				continueOnFailure().requirements("GM-6.6").condition(GrantManagementSupport.EnsureGrantManagementEndpointReturns403Or404::class.java)
			)
		)

	private companion object {
		const val CLIENT1_GRANT_ID_KEY = "client1_grant_id"
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-grant-management-ensure-wrong-client-cannot-query-grant",
	displayName = "Chile CMF OP: Grant Management - Ensure Wrong Client Cannot Query Grant",
	summary = "Client 1 performs a full authorization flow and creates a grant. Client 2 then performs its own authorization flow to obtain an access token, and then attempts to query Client 1's grant using its own access token. The test expects the grant management endpoint to return HTTP 403 or 404.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPGrantManagementEnsureWrongClientCannotQueryGrant : AbstractCMFChileOPGrantManagementWrongClient() {
	override val grantManagementCall: TestExecutionUnit get() = grantManagementQuery()
	override val callDescription: String = "query"
}

@PublishTestModule(
	testName = "cmf-chile-op-grant-management-ensure-wrong-client-cannot-revoke-grant",
	displayName = "Chile CMF OP: Grant Management - Ensure Wrong Client Cannot Revoke Grant",
	summary = "Client 1 performs a full authorization flow and creates a grant. Client 2 then performs its own authorization flow to obtain an access token, and then attempts to revoke Client 1's grant using its own access token. The test expects the grant management endpoint to return HTTP 403 or 404.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPGrantManagementEnsureWrongClientCannotRevokeGrant : AbstractCMFChileOPGrantManagementWrongClient() {
	override val grantManagementCall: TestExecutionUnit get() = grantManagementRevoke()
	override val callDescription: String = "revoke"
}

/**
 * GM 5.4 requires invalid_grant_id only for an unknown grant_id, and invalid_request for an unsupported action, so
 * the request uses whichever of merge and replace the server advertises, and the test is skipped when it advertises
 * neither: both are OPTIONAL in GM 7.1. The action itself is not under test, so the GM 6.1 scopes are not needed.
 */
@PublishTestModule(
	testName = "cmf-chile-op-grant-management-ensure-invalid-grant-id-fails",
	displayName = "Chile CMF OP: Grant Management - Ensure Invalid grant_id Fails",
	summary = "Sends an authorization request with a fabricated invalid grant_id, using whichever of grant_management_action=merge or replace the authorization server advertises. Expects the authorization server to reject the request with an invalid_grant_id error (either from the PAR endpoint or the authorization endpoint callback). The test is skipped if the authorization server supports neither merge nor replace, as both are OPTIONAL and there is then no request that can carry a grant_id.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPGrantManagementEnsureInvalidGrantIdFails : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(
				stopOnFailure().requirements("GM-5.4").condition(ExpectRedirectUriErrorPage::class.java),
				"redirect_uri_error"
			),
			parRejected = onEndpointResponse(
				CallPAREndpoint.RESPONSE_KEY,
				continueOnFailure().requirements("PAR-2.3").condition(EnsureHttpStatusCodeIs4xx::class.java),
				continueOnFailure().requirements("GM-5.4").condition(GrantManagementSupport.EnsurePAREndpointRejectsInvalidGrantId::class.java)
			),
			callback = expectAuthorizationError(
				continueOnFailure().requirements("GM-5.4").condition(GrantManagementSupport.EnsureAuthorizationEndpointRejectsInvalidGrantId::class.java)
			)
		) {
			butFirst(stopOnFailure().requirements("GM-5.5").condition(GrantManagementSupport.CreateGrantIdThatDoesNotExist::class.java))
			replace(
				GrantManagementSupport.AddGrantManagementActionCreateToAuthorizationRequest::class.java,
				Steps.of(
					"Grant management action taking a grant_id",
					stopOnFailure().requirements("GM-7.1").condition(GrantManagementSupport.SelectGrantManagementActionTakingAGrantId::class.java),
					Steps.action("Skip when neither merge nor replace is supported") { env ->
						if (env.getString(GrantManagementSupport.SELECTED_GRANT_MANAGEMENT_ACTION_KEY) == null) {
							fireTestSkipped("The authorization server does not advertise 'merge' or 'replace' in"
								+ " grant_management_actions_supported. Both are OPTIONAL per GM 7.1, and without one of them"
								+ " there is no authorization request that carries a grant_id, so an invalid grant_id cannot"
								+ " be presented to the authorization server.")
						}
					}
				)
			)
			insertAfter(
				RARSupport.AddRARToAuthorizationEndpointRequest::class.java,
				stopOnFailure().requirements("GM-5.2").condition(GrantManagementSupport.AddGrantIdToAuthorizationRequest::class.java)
			)
		}
	)
}
