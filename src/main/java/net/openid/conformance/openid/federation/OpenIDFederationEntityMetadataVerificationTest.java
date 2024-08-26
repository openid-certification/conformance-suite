package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
	testName = "openid-federation-entity-metadata-verification",
	displayName = "OpenID Federation: Federation Entity metadata Verification",
	summary = "This test verifies the correctness of endpoints provided in the entity's federation_entity metadata." +
		"The test is isolated to the provided entity and will not proceed to its superiors.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_statement_url",
		"federation.trust_anchor_jwks"
	}
)
public class OpenIDFederationEntityMetadataVerificationTest extends AbstractOpenIDFederationTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		if (env.getString("federation_list_endpoint") != null) {
			callAndContinueOnFailure(GetSubordinateListingResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.mapKey("endpoint_response", "federation_list_endpoint_response");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			callAndContinueOnFailure(EnsureResponseIsJsonArray.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("endpoint_response");

			JsonArray listEndpointResponse = JsonParser.parseString(env.getString("endpoint_response_body")).getAsJsonArray();
			for (JsonElement listElement : listEndpointResponse) {
				String entityIdentifier = OIDFJSON.getString(listElement);
				env.putString("entity_statement_url", appendWellKnown(entityIdentifier));
				callAndContinueOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				validateEntityStatementResponse();
				validateEntityStatement();
			}
		}

		fireTestFinished();
	}

}
