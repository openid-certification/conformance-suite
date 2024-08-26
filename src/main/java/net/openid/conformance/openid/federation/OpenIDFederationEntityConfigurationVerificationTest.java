package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-federation-entity-configuration-verification",
	displayName = "OpenID Federation: Entity Configuration Verification",
	summary = "This test verifies the correctness of the given entity's Entity Configuration. The test will " +
		"proceed to the Immediate Superiors of the entity as specified in authority_hints and perform additional " +
		"verification of those entities, including the output of their list and fetch endpoints.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_statement_url"
	}
)
public class OpenIDFederationEntityConfigurationVerificationTest extends AbstractOpenIDFederationTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		validateEntityStatement();
		validateAbsenceOfMetadataPolicy();
		validateImmediateSuperiors();

		fireTestFinished();
	}

}
