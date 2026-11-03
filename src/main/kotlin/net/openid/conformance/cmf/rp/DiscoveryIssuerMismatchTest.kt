package net.openid.conformance.cmf.rp

import net.openid.conformance.condition.`as`.ChangeIssuerInServerConfigurationToBeInvalid
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.variant.ConfigurationFields

@PublishTestModule(
	testName = "cmf-chile-rp-discovery-issuer-mismatch",
	displayName = "Chile CMF RP: Relying party OpenID discovery issuer mismatch",
	summary = "The client is expected to retrieve OpenID Provider Configuration Information from the .well-known/openid-configuration endpoint and detect that the issuer in the provider configuration does not match the Issuer URL that was used as the prefix to /.well-known/openid-configuration to retrieve the configuration information. As per OpenID Connect Discovery Section 4.3 the client is expected to stop the flow.",
	profile = CMF_RP_PROFILE
)
@ConfigurationFields("waitTimeoutSeconds")
class CMFChileRPDiscoveryIssuerMismatch : AbstractCMFChileRPTestModule() {

	override fun extraConfiguration(): TestExecutionUnit = condition(ChangeIssuerInServerConfigurationToBeInvalid::class.java)

	override fun server(): RPServer =
		super.server().copy(
			discoveryEndpoint = clientMustStopAfter("a discovery document whose issuer does not match the URL the document was retrieved from")
		)
}
