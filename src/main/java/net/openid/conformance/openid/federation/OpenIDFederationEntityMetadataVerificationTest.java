package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

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

		String listEndpoint = env.getString("federation_list_endpoint");
		JsonArray listEndpointResponse;
		if (listEndpoint != null) {
			eventLog.startBlock(String.format("Retrieving entities from federation_list_endpoint %s", listEndpoint));
			callAndContinueOnFailure(GetSubordinateListingResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.mapKey("endpoint_response", "federation_list_endpoint_response");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			callAndContinueOnFailure(EnsureResponseIsJsonArray.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("endpoint_response");
			eventLog.endBlock();

			listEndpointResponse = JsonParser.parseString(env.getString("endpoint_response_body")).getAsJsonArray();
			for (JsonElement listElement : listEndpointResponse) {
				String entityIdentifier = OIDFJSON.getString(listElement);
				eventLog.startBlock(String.format("Validating entity statement for %s", entityIdentifier));
				env.putString("entity_statement_url", appendWellKnown(entityIdentifier));
				callAndContinueOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				validateEntityStatementResponse();
				validateEntityStatement();
				eventLog.endBlock();
			}
		} else {
			 listEndpointResponse = new JsonArray();
		}

		String fetchEndpoint = env.getString("federation_fetch_endpoint");
		if (fetchEndpoint != null) {
			for (JsonElement listElement : listEndpointResponse) {
				String entityIdentifier = OIDFJSON.getString(listElement);
				eventLog.startBlock(String.format("Fetching subordinate statement for %s using federation_fetch_endpoint %s", entityIdentifier, fetchEndpoint));
				env.putString("entity_statement_iss", entityIdentifier);
				env.mapKey("entity_statement_url", "federation_fetch_endpoint");
				callAndContinueOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndContinueOnFailure(ValidateEntityStatementIat.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(ValidateEntityStatementExp.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.putString("entity_statement_url", env.getString("federation_fetch_endpoint_iss"));
				callAndContinueOnFailure(ValidateEntityStatementIss.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.putString("entity_statement_url", env.getString("primary_entity_statement_sub"));
				callAndContinueOnFailure(ValidateEntityStatementSub.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndContinueOnFailure(ValidateAbsenceOfAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(ValidateAbsenceOfFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndContinueOnFailure(ValidateEntityStatementMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			}
		}

		fireTestFinished();
	}

}
