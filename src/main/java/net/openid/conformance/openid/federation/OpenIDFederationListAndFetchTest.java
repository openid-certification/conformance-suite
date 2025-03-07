package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import static net.openid.conformance.openid.federation.EntityUtils.appendWellKnown;

@PublishTestModule(
	testName = "openid-federation-list-and-fetch",
	displayName = "OpenID Federation: List and fetch",
	summary = "This test validates the List and Fetch endpoints provided in the entity's `federation_entity` metadata. " +
		"The test will call the List endpoint, followed by a request to the Fetch endpoint for each of its subordinates, " +
		"followed by validation of the corresponding Subordinate statements.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier",
		"federation.trust_anchor_jwks"
	}
)
public class OpenIDFederationListAndFetchTest extends AbstractOpenIDFederationTest {

	@Override
	public void additionalConfiguration() {
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		if (env.getString("federation_list_endpoint") == null) {
			fireTestSkipped("Entity metadata does not contain a federation_list_endpoint.");
		}

		callAndStopOnFailure(ExtractFederationListEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.1");
		String listEndpoint = env.getString("federation_endpoint_url");
		String fetchEndpoint = env.getString("federation_fetch_endpoint");
		JsonArray listedEntities = validateListEndpoint(listEndpoint);
		validateFetchEndpoint(fetchEndpoint, listedEntities);

		fireTestFinished();
	}

	protected JsonArray validateListEndpoint(String listEndpoint) {
		JsonArray listEndpointResponse;
		if (listEndpoint != null) {
			eventLog.startBlock(String.format("Retrieving entities from federation_list_endpoint %s", listEndpoint));
			callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
			callAndStopOnFailure(CallListEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-8.2.1");
			validateListResponse();
			eventLog.endBlock();

			listEndpointResponse = JsonParser.parseString(env.getString("endpoint_response_body")).getAsJsonArray();
			for (JsonElement listElement : listEndpointResponse) {
				String entityIdentifier = OIDFJSON.getString(listElement);
				eventLog.startBlock(String.format("Validating entity statement for %s", entityIdentifier));
				env.putString("federation_endpoint_url", appendWellKnown(entityIdentifier));
				callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
				callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
				validateEntityStatementResponse();

				callAndContinueOnFailure(ExtractJWTFromFederationEndpointResponse.class,  Condition.ConditionResult.FAILURE, "OIDFED-9");
				if (env.containsObject("federation_response_jwt")) {
					validateEntityStatement();
				}
				eventLog.endBlock();
			}
		} else {
			listEndpointResponse = new JsonArray();
		}
		return listEndpointResponse;
	}

	protected void validateFetchEndpoint(String fetchEndpoint, JsonArray entities) {
		env.putString("expected_iss", env.getString("primary_entity_statement_iss"));
		for (JsonElement listElement : entities) {
			String entityIdentifier = OIDFJSON.getString(listElement);

			env.putString("federation_endpoint_url", fetchEndpoint);
			env.putString("expected_sub", entityIdentifier);
			callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
			callAndContinueOnFailure(AppendSubToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");

			eventLog.startBlock(String.format("Fetching subordinate statement from %s", env.getString("federation_endpoint_url")));

			callAndStopOnFailure(CallFetchEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");
			validateFetchResponse();

			callAndContinueOnFailure(ExtractJWTFromFederationEndpointResponse.class,  Condition.ConditionResult.FAILURE, "OIDFED-8.1.2");
			if (env.containsObject("federation_response_jwt")) {
				callAndContinueOnFailure(ExtractRegisteredClaimsFromFederationResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
				call(sequence(ValidateFederationResponseBasicClaimsSequence.class));

				callAndContinueOnFailure(ExtractJWKsFromPrimaryEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
				call(sequence(ValidateFederationResponseSignatureSequence.class));

				callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.INFO, "OIDFED-5.1.1");

				callAndContinueOnFailure(ValidateAbsenceOfAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
				callAndContinueOnFailure(ValidateAbsenceOfFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1");

				callAndContinueOnFailure(ValidateEntityStatementMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-6.1.2");
			}
			eventLog.endBlock();
		}
	}

}
