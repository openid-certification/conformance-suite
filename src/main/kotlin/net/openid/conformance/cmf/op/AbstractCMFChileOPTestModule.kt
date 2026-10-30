package net.openid.conformance.cmf.op

import com.google.gson.JsonObject
import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.CallPAREndpoint
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped
import net.openid.conformance.sequence.Steps
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule
import net.openid.conformance.testmodule.Environment
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.testmodule.TestFailureException
import net.openid.conformance.testmodule.TestModule
import net.openid.conformance.variant.ConfigurationFields
import net.openid.conformance.variant.FAPIOpenIDConnect
import net.openid.conformance.variant.VariantParameters

/**
 * One visit to the authorization endpoint: the steps that prepare it, and the steps run on the callback.
 *
 * @param placeholder when set, the authorization server may show an error page instead of redirecting back; the
 * unit creates the placeholder for its screenshot and stores it in error_callback_placeholder
 * @param redirectWhen checked after [beforeRedirect]; when false the test finishes without a redirect, e.g.
 * because the PAR endpoint rejected the request and [beforeRedirect] has already checked the error
 */
class AuthorizationLeg(
	val beforeRedirect: TestExecutionUnit,
	val afterCallback: TestExecutionUnit,
	val placeholder: TestExecutionUnit? = null,
	val redirectWhen: (Environment) -> Boolean = { true }
)

/**
 * FAPI2 Message Signing authorization server tests under the Chile Open Finance (CMF) profile, built from
 * [OPFlow] units instead of overridable hooks.
 *
 * A module describes itself as data: the [legs] it runs through the authorization endpoint, any extra
 * configuration and the steps after the last leg. This class only sequences them around the browser redirects.
 */
@VariantParameters(FAPIOpenIDConnect::class)
@ConfigurationFields(
	"server.discoveryUrl",
	"client.client_id",
	"client.scope",
	"client.jwks",
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"resource.resourceUrl",
	"resource.richAuthorizationRequest"
)
abstract class AbstractCMFChileOPTestModule : AbstractRedirectServerTestModule() {

	protected val openid: Boolean
		get() = getVariant(FAPIOpenIDConnect::class.java) == FAPIOpenIDConnect.OPENID_CONNECT

	/** Whether the module also configures and validates client2; see [AbstractCMFChileOPMultipleClientTestModule]. */
	protected open val usesSecondClient: Boolean = false

	/** Run at the end of configuration, e.g. to replace the redirect uri. */
	protected open fun extraConfiguration(): TestExecutionUnit? = null

	/** The visits to the authorization endpoint this module makes, in order. Called once the module starts. */
	protected abstract fun legs(): List<AuthorizationLeg>

	/** Run after the callback of the last leg, before the test finishes. */
	protected open fun afterLastLeg(): TestExecutionUnit? = null

	protected fun flow(client: OPClient = OPClient.FIRST, needsGrantManagementApiScopes: Boolean = false): OPFlow =
		OPFlow(client, openid, needsGrantManagementApiScopes)

	/**
	 * The standard flow for one client, changed by [modify]; e.g. `modifiedLeg { skip(X::class.java, "...") }`.
	 */
	protected fun modifiedLeg(flow: OPFlow = flow(), modify: Steps.Modifier.() -> Unit): AuthorizationLeg =
		AuthorizationLeg(
			beforeRedirect = Steps.modify(flow.beforeRedirect()).apply(modify).build(),
			afterCallback = flow.afterCallback()
		)

	/**
	 * A request the authorization server must reject: at the PAR endpoint, with an error page the tester takes a
	 * screenshot of, or with an error returned to the redirect uri.
	 *
	 * @param errorPage creates the placeholder for the error page; see [expectErrorPage]
	 * @param parRejected the checks on the PAR endpoint's error response
	 * @param callback the steps for the redirect back to the suite; [expectAuthorizationError] for an error
	 * @param signed whether the authorization request is sent as a signed request object
	 * @param modify the changes that make the request invalid, applied to [OPFlow.beforeRedirect]
	 */
	protected fun rejectedRequestLeg(
		errorPage: TestExecutionUnit,
		parRejected: TestExecutionUnit,
		callback: (OPFlow) -> TestExecutionUnit,
		flow: OPFlow = flow(),
		signed: Boolean = true,
		modify: Steps.Modifier.() -> Unit = {}
	): AuthorizationLeg =
		AuthorizationLeg(
			beforeRedirect = Steps.modify(flow.beforeRedirect(parRejected, signed)).apply(modify).build(),
			afterCallback = callback(flow),
			placeholder = errorPage,
			redirectWhen = { parAccepted(it) }
		)

	/** A request that ends at the PAR endpoint: [steps] call it and check its response, and there is no redirect. */
	protected fun parEndpointOnlyLeg(steps: TestExecutionUnit): AuthorizationLeg =
		AuthorizationLeg(beforeRedirect = steps, afterCallback = Steps.NONE, redirectWhen = { false })

	/** A PAR request changed by [beforeCall] just before it is sent, whose response [check] must find an error. */
	protected fun parRequestRejectedLeg(beforeCall: TestExecutionUnit, check: TestExecutionUnit): AuthorizationLeg {
		val flow = flow()
		return parEndpointOnlyLeg(
			Steps.of(
				"Rejected PAR request",
				flow.createAuthorizationRequest,
				flow.createRequestObject,
				Steps.modify(flow.pushAuthorizationRequest).insertBefore(CallPAREndpoint::class.java, beforeCall).build(),
				check
			)
		)
	}

	/** A callback that must be an error response, checked by the standard error checks and then [errorChecks]. */
	protected fun expectAuthorizationError(vararg errorChecks: TestExecutionUnit): (OPFlow) -> TestExecutionUnit =
		{ flow -> flow.errorCallback(authorizationErrorResponseSteps(*errorChecks)) }

	private var plannedLegs: List<AuthorizationLeg> = emptyList()
	private var currentLeg = 0

	override fun configure(config: JsonObject?, baseUrl: String?, externalUrlOverride: String?, baseMtlsUrl: String?) {
		env.putString("base_url", baseUrl)
		env.putString("base_mtls_url", baseMtlsUrl)
		env.putObject("config", config)
		env.putString("external_url_override", externalUrlOverride)

		if (env.getBoolean("config", "skip_test") == true) {
			// for CI overrides where an environment cannot run this test to completion
			callAndContinueOnFailure(ConfigurationRequestsTestIsSkipped::class.java, ConditionResult.FAILURE)
			fireTestFinished()
			return
		}

		if (!openid && scopeContains("openid")) {
			throw TestFailureException(id, "openid scope cannot be used with PLAIN_OAUTH")
		}

		call(serverConfigurationSteps(openid))
		exposeEnvString("redirect_uri")

		call(firstClientConfigurationSteps())
		exposeEnvString("client_id")
		if (usesSecondClient) {
			call(secondClientConfigurationSteps())
		}
		call(resourceConfigurationSteps())

		extraConfiguration()?.let { call(it) }

		status = TestModule.Status.CONFIGURED
		fireSetupDone()
	}

	override fun start() {
		status = TestModule.Status.RUNNING
		plannedLegs = legs()
		beginLeg(0)
	}

	private fun beginLeg(index: Int) {
		currentLeg = index
		val leg = plannedLegs[index]

		call(leg.beforeRedirect)

		if (!leg.redirectWhen(env)) {
			fireTestFinished()
			return
		}

		redirectToAuthorizationEndpoint(leg)
	}

	/** Sends the browser to redirect_to_authorization_endpoint, waiting for the leg's placeholder when it has one. */
	protected open fun redirectToAuthorizationEndpoint(leg: AuthorizationLeg) {
		if (leg.placeholder != null) {
			performRedirectAndWaitForPlaceholdersOrCallback()
		} else {
			performRedirect()
		}
	}

	override fun createPlaceholder() {
		val placeholder = plannedLegs[currentLeg].placeholder
			?: throw TestFailureException(id, "Placeholder must be created for test $name")
		call(placeholder)
	}

	override fun processCallback() {
		call(plannedLegs[currentLeg].afterCallback)

		if (currentLeg + 1 < plannedLegs.size) {
			beginLeg(currentLeg + 1)
			return
		}

		afterLastLeg()?.let { call(it) }
		fireTestFinished()
	}

	private fun scopeContains(requiredScope: String): Boolean {
		val scope = env.getString("config", "client.scope")
		if (scope.isNullOrEmpty()) {
			throw TestFailureException(id, "'scope' seems to be missing from client configuration")
		}
		return scope.split(" ").contains(requiredScope)
	}
}

/**
 * Modules that run the flow for both configured clients, which must be distinct clients with their own keys and
 * certificates.
 */
@ConfigurationFields(
	"client2.client_id",
	"client2.scope",
	"client2.jwks",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
)
abstract class AbstractCMFChileOPMultipleClientTestModule : AbstractCMFChileOPTestModule() {
	override val usesSecondClient: Boolean = true
}
