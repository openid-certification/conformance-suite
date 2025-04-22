package net.openid.conformance.openid.federation;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-entity-configuration",
	displayName = "OpenID Federation: Entity Configuration validation",
	summary =
		"The test will fetch and validate the given entity's Entity Configuration, and then proceed to its Immediate Superiors " +
		"as specified in `authority_hints`. For each Immediate Superior, its Entity Configuration is fetched and validated. " +
		"Following that, the test will invoke the superior's List endpoint to confirm the presence of the original entity, " +
		"and finally use the Fetch endpoint to retrieve and validate the Subordinate Statement for the entity.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier"
	}
)
public class OpenIDFederationEntityConfigurationTest extends AbstractOpenIDFederationTest {

	@Override
	public void additionalConfiguration() {
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		validateEntityStatement();
		validateAbsenceOfMetadataPolicy();
		validateImmediateSuperiors();

		fireTestFinished();
	}

}
