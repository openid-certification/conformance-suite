package net.openid.conformance.cmf.rp

import net.openid.conformance.condition.`as`.par.CreatePAREndpointResponse
import net.openid.conformance.condition.`as`.par.ExtractRequestObjectFromPAREndpointRequest
import net.openid.conformance.condition.common.GrantManagementSupport
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.variant.ConfigurationFields
import java.util.function.Predicate

/**
 * Grant management client tests, which require the client to send grant management parameters in its PAR request:
 * the suite drives both the responses and the checks, so without these checks a client that never sent
 * grant_management_action would pass.
 */
abstract class AbstractCMFChileRPGrantManagementTest : AbstractCMFChileRPTestModule() {

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			parEndpoint = Steps.modify(server.parEndpoint)
				.insertAfter(ExtractRequestObjectFromPAREndpointRequest::class.java, grantManagementRequestChecks())
				.build()
		)
	}

	/** The PAR request asks for a new grant: the create action, which takes no grant_id (GM 5.2). */
	protected open fun grantManagementRequestChecks(): TestExecutionUnit =
		Steps.of(
			"Grant management request",
			continueOnFailure().requirements("GM-5.2").condition(GrantManagementSupport.EnsurePARRequestContainsGrantManagementAction::class.java),
			continueOnFailure().requirements("GM-5.4")
				.condition(GrantManagementSupport.EnsurePARRequestDoesNotContainGrantIdWithCreateAction::class.java)
		)
}

@PublishTestModule(
	testName = "cmf-chile-rp-grant-management-happy-path",
	displayName = "Chile CMF RP: client test for happy path with Grant Management",
	summary = "Tests a 'happy path' flow with Grant Management. Configure your client to send a PAR request with grant_management_action=create (and, per GM 5.2, no grant_id). The suite returns a grant_id in the token response. The client should perform OpenID discovery, push a signed authorization request to the PAR endpoint, call the authorization endpoint, exchange the authorization code for an access token (which will include a grant_id), and make a GET request to the accounts endpoint.",
	profile = CMF_RP_PROFILE
)
class CMFChileRPGrantManagementHappyPath : AbstractCMFChileRPGrantManagementTest()

@PublishTestModule(
	testName = "cmf-chile-rp-grant-management-query-and-revoke",
	displayName = "Chile CMF RP: client test for querying and revoking a grant",
	summary = "Configure your client to send a PAR request with grant_management_action=create, and to then use the grant management API. After the authorization flow and the call to the accounts endpoint, the client should query the grant (HTTP GET to the mTLS grant_management_endpoint with the issued grant_id, using its access token, which will return the granted scopes) and then revoke it (HTTP DELETE to the same URL, which will return 204). The test finishes once the grant has been revoked; it will fail if the client calls the endpoint for a grant it was not issued, sends a body, or does not present a correctly sender constrained access token.",
	profile = CMF_RP_PROFILE
)
class CMFChileRPGrantManagementQueryAndRevoke : AbstractCMFChileRPGrantManagementTest() {

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			grantManagementEndpoint = Steps.modify(server.grantManagementEndpoint)
				.insertBefore(
					"Revoke the grant",
					Steps.`when`(
						"the grant was not queried",
						{ env -> env.getBoolean(GRANT_QUERIED) != true },
						Steps.check("Check the grant was queried before it is revoked") { _, c ->
							c.log("The client revoked the grant without querying it first; the query is part of this test, so it was expected before the revocation")
						}
					)
				)
				.build(),
			// the grant management calls come after the resource request
			accountsEndpointFinishesTest = Predicate { false },
			grantManagementEndpointFinishesTest = firstGrantRevocation
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-rp-grant-management-invalid-grant-id-fails",
	displayName = "Chile CMF RP: client test - invalid grant_id from PAR endpoint, should be rejected",
	summary = "Configure your client to send a PAR request that updates an existing grant, that is with grant_management_action set to merge or replace and a grant_id. The suite will return an invalid_grant_id error (HTTP 400) from the PAR endpoint. The test expects the client to display an error indicating that the grant_id was rejected, and to NOT proceed to the authorization endpoint.",
	profile = CMF_RP_PROFILE
)
@ConfigurationFields("waitTimeoutSeconds")
class CMFChileRPGrantManagementInvalidGrantIdFails : AbstractCMFChileRPGrantManagementTest() {

	/** The PAR request updates an existing grant; the grant_id itself does not matter, as it is rejected anyway. */
	override fun grantManagementRequestChecks(): TestExecutionUnit =
		Steps.of(
			"Grant management request",
			// create with a grant_id is a different error (invalid_request, GM 5.4), so only the updating actions are accepted
			continueOnFailure().requirements("GM-5.2")
				.condition(GrantManagementSupport.EnsurePARRequestContainsGrantManagementActionUpdatingAGrant::class.java),
			continueOnFailure().requirements("GM-5.2").condition(GrantManagementSupport.EnsurePARRequestContainsGrantId::class.java)
		)

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			parEndpoint = Steps.modify(server.parEndpoint)
				.replace(
					CreatePAREndpointResponse::class.java,
					Steps.of(
						null,
						stopOnFailure().requirements("GM-5.4").condition(GrantManagementSupport.CreatePAREndpointInvalidGrantIdErrorResponse::class.java),
						clientMustStopAfter("an invalid_grant_id error from the PAR endpoint")
					)
				)
				.build()
		)
	}
}
