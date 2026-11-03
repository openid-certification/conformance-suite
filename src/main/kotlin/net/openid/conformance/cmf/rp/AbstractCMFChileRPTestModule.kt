package net.openid.conformance.cmf.rp

import com.google.gson.JsonObject
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import net.openid.conformance.condition.common.GrantManagementSupport
import net.openid.conformance.sequence.Steps
import net.openid.conformance.testmodule.AbstractTestModule
import net.openid.conformance.testmodule.Endpoint
import net.openid.conformance.testmodule.Environment
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.testmodule.TestFailureException
import net.openid.conformance.testmodule.OIDFJSON
import net.openid.conformance.testmodule.TestModule
import net.openid.conformance.variant.ConfigurationFields
import net.openid.conformance.variant.FAPIClientType
import net.openid.conformance.variant.VariantParameters
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.view.RedirectView

/**
 * FAPI2 Message Signing client tests under the Chile Open Finance (CMF) profile, built from [RPServer] units instead
 * of overridable hooks.
 *
 * The suite plays the authorization server, so the client drives the test: this class routes each request to the
 * [Endpoint] serving its path and returns the response the endpoint builds. A module describes itself through the
 * [server] it emulates.
 */
@VariantParameters(FAPIClientType::class)
@ConfigurationFields(
	"server.jwks",
	"client.client_id",
	"client.scope",
	"client.redirect_uri",
	"client.certificate",
	"client.jwks",
	"resource.authorization_details_types_supported"
)
abstract class AbstractCMFChileRPTestModule : AbstractTestModule() {

	protected val openid: Boolean
		get() = getVariant(FAPIClientType::class.java) == FAPIClientType.OIDC

	/** The server this module emulates. Called once, when the module is configured. */
	protected open fun server(): RPServer = RPServer(openid)

	/** The emulated server's discovery metadata and keys. Called once, when the module is configured. */
	protected open fun serverConfiguration(): TestExecutionUnit = serverConfigurationSteps(openid)

	/** Run at the end of configuration. */
	protected open fun extraConfiguration(): TestExecutionUnit? = null

	private lateinit var emulated: RPServer
	private lateinit var plainEndpoints: Map<String, Endpoint>
	private lateinit var mtlsEndpoints: Map<String, Endpoint>

	/** How long the client is given to make a request it must not make, before the test finishes. */
	private var waitTimeoutSeconds = 5L

	/** The response after which the client must not call the server again, once the test has sent it. */
	@Volatile
	private var clientMustHaveStoppedAfter: String? = null

	override fun configure(config: JsonObject, baseUrl: String, externalUrlOverride: String?, baseMtlsUrl: String) {
		env.putString("base_url", baseUrl)
		env.putString("base_mtls_url", baseMtlsUrl)
		env.putObject("config", config)
		config.get("waitTimeoutSeconds")?.let { waitTimeoutSeconds = OIDFJSON.getLong(it) }

		emulated = server()
		plainEndpoints = plainEndpoints().mapValues { (_, endpoint) -> endpoint.precededBy(emulated.incomingTlsChecks) }
		mtlsEndpoints = mtlsEndpoints().mapValues { (_, endpoint) -> endpoint.precededBy(emulated.incomingTlsChecks) }

		call(serverConfiguration())
		call(clientConfigurationSteps())
		extraConfiguration()?.let { call(it) }

		status = TestModule.Status.CONFIGURED
		fireSetupDone()
	}

	override fun start() {
		status = TestModule.Status.RUNNING
		// the client drives the test from here
		status = TestModule.Status.WAITING
	}

	/**
	 * The client must not make any request but for discovery and keys after receiving the response the endpoint is
	 * building, described by [response] (e.g. "an invalid id_token (missing iss value)"). The test passes if it does
	 * not within waitTimeoutSeconds, and fails on the first request it makes.
	 */
	protected fun clientMustStopAfter(response: String): TestExecutionUnit =
		Steps.check("Expect the client to stop") { _, c ->
			if (clientMustHaveStoppedAfter == null) {
				clientMustHaveStoppedAfter = response
				startWaitingForTimeout()
			}
			c.log(
				"The client must not call the server again after receiving $response. The test finishes if it makes no " +
					"further request within $waitTimeoutSeconds seconds; this can be changed with waitTimeoutSeconds in the test configuration."
			)
		}

	private fun startWaitingForTimeout() {
		testExecutionManager.runInBackground {
			Thread.sleep(waitTimeoutSeconds * 1000)
			if (status == TestModule.Status.WAITING) {
				setStatus(TestModule.Status.RUNNING)
				fireTestFinished()
			}
			"done"
		}
	}

	private fun refuseIfClientMustHaveStopped(path: String) {
		clientMustHaveStoppedAfter?.let { throw TestFailureException(id, "Client has incorrectly called '$path' after receiving $it") }
	}

	/** The endpoints served without mTLS: discovery, keys and the authorization endpoint. */
	private fun plainEndpoints(): Map<String, Endpoint> = mapOf(
		".well-known/openid-configuration" to discoveryEndpoint(),
		"jwks" to Endpoint.of(Steps.NONE) { env -> ResponseEntity<Any>(env.responseBody("server_public_jwks"), HttpStatus.OK) },
		"authorize" to Endpoint.of(emulated.authorizationEndpoint) { env ->
			RedirectView(env.getString("authorization_endpoint_response_redirect"), false, false, false)
		}.visitedByBrowser(),
		"par" to mtlsOnly("The PAR endpoint must be called over an mTLS secured connection using the pushed_authorization_request_endpoint found in mtls_endpoint_aliases."),
		"token" to mtlsOnly("The token endpoint must be called over an mTLS secured connection using the token_endpoint found in mtls_endpoint_aliases."),
		"userinfo" to mtlsOnly("The userinfo endpoint must be called over an mTLS secured connection."),
		ACCOUNTS_PATH to mtlsOnly("The accounts endpoint must be called over an mTLS secured connection."),
		GRANTS_PATH to mtlsOnly("The grant management endpoint must be called over an mTLS secured connection, using the grant_management_endpoint found in mtls_endpoint_aliases.")
	)

	/** The client-authenticated and resource endpoints, which the profile serves only at their mTLS aliases. */
	private fun mtlsEndpoints(): Map<String, Endpoint> = mapOf(
		"par" to Endpoint.of(emulated.parEndpoint) { env -> storedResponse(env, "par_endpoint_response", HttpStatus.CREATED) },
		"token" to Endpoint.of(emulated.tokenEndpoint) { env -> jsonResponse(env, "token_endpoint_response", HttpStatus.OK) },
		"userinfo" to Endpoint.of(emulated.userinfoEndpoint) { env -> jsonResponse(env, "user_info_endpoint_response", HttpStatus.OK) },
		ACCOUNTS_PATH to Endpoint.of(emulated.accountsEndpoint) { env ->
			storedResponse(env, "accounts_endpoint_response", HttpStatus.OK)
		}.finishingTestWhen(emulated.accountsEndpointFinishesTest),
		GRANTS_PATH to Endpoint.of(emulated.grantManagementEndpoint) { env ->
			storedResponse(env, GrantManagementSupport.CreateGrantManagementQueryResponse.RESPONSE_KEY, HttpStatus.OK)
		}.finishingTestWhen(emulated.grantManagementEndpointFinishesTest)
	)

	private fun discoveryEndpoint(): Endpoint =
		Endpoint.of(emulated.discoveryEndpoint) { env -> ResponseEntity<Any>(env.responseBody("server"), HttpStatus.OK) }

	/** A response with the JSON body stored under [key] and the headers stored under [key]_headers. */
	private fun jsonResponse(env: Environment, key: String, status: HttpStatus): ResponseEntity<Any> =
		ResponseEntity(env.responseBody(key), headersFromJson(env.getObject(key + "_headers")), status)

	/**
	 * A response with the body, if any, stored under [key], the headers stored under [key]_headers and the status
	 * stored under [key][HTTP_STATUS_SUFFIX], or [status] when none is stored.
	 */
	private fun storedResponse(env: Environment, key: String, status: HttpStatus): ResponseEntity<Any> =
		ResponseEntity(
			env.getObject(key),
			headersFromJson(env.getObject(key + "_headers")),
			env.getInteger(key + HTTP_STATUS_SUFFIX)?.let { HttpStatusCode.valueOf(it) } ?: status
		)

	/** The endpoint serving [path]: the one for the path itself, or for a prefix ending in '/' that it starts with. */
	private fun Map<String, Endpoint>.servingPath(path: String): Endpoint? =
		this[path] ?: entries.firstOrNull { (prefix, _) -> prefix.endsWith("/") && path.startsWith(prefix) }?.value

	/** A path the client must only call at its mTLS alias. */
	private fun mtlsOnly(message: String): Endpoint =
		Endpoint.of(Steps.check("Ensure endpoint is called over mTLS") { _, c -> throw c.error(message) }) { error(message) }

	override fun handleHttp(path: String, req: HttpServletRequest, res: HttpServletResponse, session: HttpSession, requestParts: JsonObject): Any {
		if (path !in ALWAYS_SERVED) {
			refuseIfClientMustHaveStopped(path)
		}
		val endpoint = plainEndpoints.servingPath(path) ?: throw TestFailureException(id, "Got unexpected HTTP call to $path")
		return serve(endpoint, requestParts)
	}

	override fun handleHttpMtls(path: String, req: HttpServletRequest, res: HttpServletResponse, session: HttpSession, requestParts: JsonObject): Any {
		refuseIfClientMustHaveStopped(path)
		val endpoint = mtlsEndpoints.servingPath(path) ?: throw TestFailureException(id, "Got unexpected HTTP (using mtls) call to $path")
		return serve(endpoint, requestParts)
	}

	override fun handleWellKnown(path: String, req: HttpServletRequest, res: HttpServletResponse, session: HttpSession, requestParts: JsonObject): Any {
		if (path.startsWith("/.well-known/oauth-authorization-server")) {
			return serve(discoveryEndpoint(), requestParts)
		}
		return super.handleWellKnown(path, req, res, session, requestParts)
	}
}

/** The paths a client may still call after a response that obliges it to stop: discovery and keys. */
private val ALWAYS_SERVED = setOf(".well-known/openid-configuration", "jwks")
