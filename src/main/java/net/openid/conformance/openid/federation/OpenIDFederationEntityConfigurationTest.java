package net.openid.conformance.openid.federation;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-entity-configuration",
	displayName = "OpenID Federation: Entity Configuration validation",
	summary = "This test verifies the correctness of the given entity's Entity Configuration. The test will " +
		"proceed to the Immediate Superiors of the entity as specified in authority_hints and perform additional " +
		"verification of those entities, including the output of their list and fetch endpoints.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier"
	}
)
public class OpenIDFederationEntityConfigurationTest extends AbstractOpenIDFederationTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		validateEntityStatement();
		validateAbsenceOfMetadataPolicy();
		validateImmediateSuperiors();

		fireTestFinished();
	}

}
