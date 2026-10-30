package net.openid.conformance.cmf.op

import com.google.gson.JsonObject
import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.*
import net.openid.conformance.condition.common.GrantManagementSupport
import net.openid.conformance.ekyc.condition.client.EnsureAuthorizationResponseIssParameterSupportedIsTrue
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.testmodule.AbstractTestModule
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.testmodule.TestModule
import net.openid.conformance.variant.ConfigurationFields
import net.openid.conformance.variant.FAPIOpenIDConnect
import net.openid.conformance.variant.VariantParameters

@PublishTestModule(
	testName = "cmf-chile-op-discovery-end-point-verification",
	displayName = "Chile CMF OP: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configuration (including scopes, response_types, grant_types etc) contains values required by the specification",
	profile = CMF_OP_PROFILE
)
@VariantParameters(FAPIOpenIDConnect::class)
@ConfigurationFields("server.discoveryUrl")
class CMFChileOPDiscoveryEndpointVerification : AbstractTestModule() {

	private val openid: Boolean
		get() = getVariant(FAPIOpenIDConnect::class.java) == FAPIOpenIDConnect.OPENID_CONNECT

	override fun configure(config: JsonObject?, baseUrl: String?, externalUrlOverride: String?, baseMtlsUrl: String?) {
		env.putString("base_url", baseUrl)
		env.putString("base_mtls_url", baseMtlsUrl)
		env.putObject("config", config)

		call(fetchServerConfiguration())

		status = TestModule.Status.CONFIGURED
		fireSetupDone()
	}

	override fun start() {
		status = TestModule.Status.RUNNING
		call(endpointVerification())
		fireTestFinished()
	}

	private fun fetchServerConfiguration(): TestExecutionUnit {
		val requirement = if (openid) "OIDCD-4" else "RFC8414-3.2"
		return Steps.of(
			"Fetch server configuration",
			if (openid) condition(GetDynamicServerConfiguration::class.java) else condition(GetOauthDynamicServerConfiguration::class.java),
			continueOnFailure().requirements(requirement).of(
				"Discovery response",
				EnsureDiscoveryEndpointResponseStatusCodeIs200::class.java,
				CheckDiscoveryEndpointReturnedJsonContentType::class.java
			),
			continueOnFailure().requirements("RFC8705-5").condition(AddMTLSEndpointAliasesToEnvironment::class.java)
		)
	}

	private fun endpointVerification(): TestExecutionUnit =
		Steps.of(
			"Discovery endpoint verification",
			authorizationEndpointChecks(),
			if (openid) {
				Steps.of(
					null,
					continueOnFailure().condition(CheckDiscEndpointDiscoveryUrl::class.java),
					continueOnFailure().requirements("OIDCD-4.3", "OIDCD-7.2").condition(CheckDiscEndpointIssuer::class.java)
				)
			} else {
				Steps.of(
					null,
					continueOnFailure().condition(CheckOauthDiscEndpointDiscoveryUrl::class.java),
					continueOnFailure().requirements("RFC8414-3.3", "RFC8414-6.2").condition(CheckOauthDiscEndpointIssuer::class.java)
				)
			},
			continueOnFailure().requirements("RFC8414-2").of(
				"Metadata",
				CheckDiscEndpointIssuerIsValidUrl::class.java,
				ValidateServerMetadataAgainstSchema::class.java
			),
			continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC8414-2")
				.condition(CheckForUnexpectedParametersInServerMetadata::class.java),
			continueOnFailure().requirements("FAPI2-SP-FINAL-5.3.3.1-5", "RFC8705-3.3")
				.condition(CheckTLSClientCertificateBoundAccessTokensTrue::class.java),
			if (openid) {
				continueOnFailure().requirements("FAPI2-SP-FINAL-5.4-1").condition(FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported::class.java)
			} else Steps.NONE,
			continueOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.1-6")
				.condition(CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsPrivateKeyOrTlsClient::class.java),
			continueOnFailure().requirements("FAPI2-SP-FINAL-5.4-1")
				.condition(FAPI2CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported::class.java),
			continueOnFailure().requirements("FAPI2-SP-FINAL-5.4").condition(FAPI2CheckDiscEndpointUserinfoSigningAlgValuesSupported::class.java)
				.skipIfElementMissing("server", "userinfo_signing_alg_values_supported").onSkip(ConditionResult.INFO),
			continueOnFailure().requirements("OIDCD-3").condition(CheckDiscEndpointTokenEndpoint::class.java),
			continueOnFailure().requirements("OIDCD-3").condition(CheckDiscEndpointRegistrationEndpoint::class.java)
				.skipIfElementMissing("server", "registration_endpoint").onSkip(ConditionResult.INFO),
			if (openid) continueOnFailure().requirements("OIDCD-3").condition(CheckJwksUri::class.java) else Steps.NONE,
			continueOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-5").condition(EnsureServerConfigurationSupportsCodeChallengeMethodS256::class.java),
			continueOnFailure().requirements("RFC6749-3.3").condition(CheckDiscEndpointScopesSupportedSyntax::class.java),
			continueOnFailure().requirements("RFC8414-2").condition(CheckDiscEndpointLocalesSyntax::class.java),
			continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC8414-2").condition(CheckDiscEndpointLocalesCanonicalCasing::class.java),
			continueOnFailure().requirements("FAPI2-SP-FINAL-5.3.3.1-5").condition(EnsureServerConfigurationSupportsMTLS::class.java),
			// although PAR is required by FAPI2, the server may support non-FAPI2 use cases, so it need not be 'true'
			continueOnFailure().requirements("PAR-5").condition(CheckDiscRequirePushedAuthorizationRequestsIsABoolean::class.java),
			continueOnFailure().condition(FAPI2CheckDiscEndpointRequestObjectSigningAlgValuesSupported::class.java),
			continueOnFailure().condition(CheckDiscEndpointAuthorizationEndpoint::class.java),
			continueOnFailure().condition(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode::class.java),
			if (openid) {
				Steps.of(
					null,
					continueOnFailure().condition(CheckDiscEndpointScopesSupportedContainsOpenId::class.java),
					continueOnFailure().requirements("OIDCD-3").condition(CheckDiscEndpointSubjectTypesSupported::class.java)
				)
			} else Steps.NONE,
			grantManagementChecks()
		)

	private fun authorizationEndpointChecks(): TestExecutionUnit =
		Steps.of(
			"Authorization endpoint metadata",
			continueOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-1").condition(CheckDiscEndpointResponseTypeCodeSupported::class.java),
			continueOnFailure().requirements("OAuth2-iss-3", "FAPI2-SP-FINAL-5.3.2.2-7")
				.condition(EnsureAuthorizationResponseIssParameterSupportedIsTrue::class.java),
			continueOnFailure().requirements("PAR-5", "FAPI2-SP-FINAL-5.3.2.2-2").condition(CheckDiscEndpointPARSupported::class.java)
		)

	/**
	 * Both members are OPTIONAL in GM 7.1, but the Chile profile requires grant management: without the endpoint
	 * no grant can be queried or revoked (GM 6.2, 6.3), and omitting the actions means no action is supported.
	 */
	private fun grantManagementChecks(): TestExecutionUnit =
		Steps.of(
			"Grant management metadata",
			continueOnFailure().requirements("GM-6.2", "GM-7.1").condition(GrantManagementSupport.CheckDiscoveryForGrantManagementEndpoint::class.java),
			continueOnFailure().requirements("GM-7.1").condition(GrantManagementSupport.CheckDiscoveryForGrantManagementActionsSupported::class.java),
			continueOnFailure().requirements("GM-7.1")
				.condition(GrantManagementSupport.CheckDiscoveryForGrantManagementActionsSupportedContainsRequiredActions::class.java)
				.skipIfElementMissing("server", "grant_management_actions_supported").onSkip(ConditionResult.INFO)
		)
}
