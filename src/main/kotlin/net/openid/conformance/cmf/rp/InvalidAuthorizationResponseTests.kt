package net.openid.conformance.cmf.rp

import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.`as`.AddInvalidIssToAuthorizationEndpointResponseParams
import net.openid.conformance.condition.`as`.AddInvalidStateToAuthorizationEndpointResponseParams
import net.openid.conformance.condition.`as`.AddIssToAuthorizationEndpointResponseParams
import net.openid.conformance.condition.`as`.CreateAuthorizationCode
import net.openid.conformance.condition.`as`.CreateEffectiveAuthorizationRequestParameters
import net.openid.conformance.condition.`as`.RemoveIssFromAuthorizationEndpointResponseParams
import net.openid.conformance.condition.`as`.RemoveStateFromAuthorizationEndpointResponseParams
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.variant.ConfigurationFields

/** A test whose authorization endpoint returns an invalid response, after which the client must not call the server again. */
@ConfigurationFields("waitTimeoutSeconds")
abstract class AbstractCMFChileRPInvalidAuthorizationResponseTest : AbstractCMFChileRPTestModule() {

	/** What is wrong with the authorization response, as shown to the tester. */
	protected abstract val fault: String

	/** Changes the authorization response parameters before they are sent. */
	protected abstract fun responseFault(): TestExecutionUnit

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			authorizationEndpoint = Steps.modify(server.authorizationEndpoint)
				.insertAfter(
					AddIssToAuthorizationEndpointResponseParams::class.java,
					Steps.of(null, responseFault(), clientMustStopAfter("an invalid authorization response ($fault)"))
				)
				.build()
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-authorization-response-iss",
	displayName = "Chile CMF RP: client test - invalid iss from authorization response must be rejected",
	summary = "This test sends an invalid iss in the authorization response. The client should display an invalid issuer message and must not call any other endpoints.",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidAuthorizationResponseIss : AbstractCMFChileRPInvalidAuthorizationResponseTest() {
	override val fault = "Returned invalid iss"

	override fun responseFault(): TestExecutionUnit =
		continueOnFailure().onFail(ConditionResult.INFO).condition(AddInvalidIssToAuthorizationEndpointResponseParams::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-remove-authorization-response-iss",
	displayName = "Chile CMF RP: client test - authorization_endpoint response without iss must be rejected",
	summary = "This test does not send an issuer in the authorization response. The client should display a message that the authorization response does not contain an issuer and must not call any other endpoints.",
	profile = CMF_RP_PROFILE
)
class CMFChileRPRemoveAuthorizationResponseIss : AbstractCMFChileRPInvalidAuthorizationResponseTest() {
	override val fault = "Removed iss from authorization response"

	override fun responseFault(): TestExecutionUnit =
		continueOnFailure().onFail(ConditionResult.INFO).condition(RemoveIssFromAuthorizationEndpointResponseParams::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-ensure-authorization-response-with-invalid-state-fails",
	displayName = "Chile CMF RP: sends an authorization endpoint response with an invalid state value.",
	summary = "This test should end with the client displaying an error message that the state value in the authorization endpoint response is invalid",
	profile = CMF_RP_PROFILE
)
class CMFChileRPEnsureAuthorizationResponseWithInvalidStateFails : AbstractCMFChileRPInvalidAuthorizationResponseTest() {
	override val fault = "Added invalid state to the authorization response"

	override fun responseFault(): TestExecutionUnit =
		continueOnFailure().onFail(ConditionResult.INFO).condition(AddInvalidStateToAuthorizationEndpointResponseParams::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-ensure-authorization-response-with-invalid-missing-state-fails",
	displayName = "Chile CMF RP: sends an authorization endpoint response with a missing state value which the client should reject",
	summary = "This test should end with the client displaying an error message that the state value in the authorization endpoint response is invalid. If the client does not send a state value the test result will be SKIPPED.",
	profile = CMF_RP_PROFILE
)
class CMFChileRPEnsureAuthorizationResponseWithInvalidMissingStateFails : AbstractCMFChileRPInvalidAuthorizationResponseTest() {
	override val fault = "Removed state from the authorization response"

	override fun responseFault(): TestExecutionUnit =
		continueOnFailure().onFail(ConditionResult.INFO).condition(RemoveStateFromAuthorizationEndpointResponseParams::class.java)

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			authorizationEndpoint = Steps.modify(server.authorizationEndpoint)
				.insertBefore(
					CreateAuthorizationCode::class.java,
					Steps.`when`(
						"the client sent no state",
						{ env ->
							env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE)
								.isNullOrEmpty()
						},
						Steps.action("Skip the test") {
							fireTestSkipped(
								"This test is being skipped as it relies on the client supplying an OPTIONAL state value - since none is " +
									"supplied, this can not be tested. PKCE prevents CSRF so this is acceptable and will not prevent certification."
							)
						}
					)
				)
				.build()
		)
	}
}
