package net.openid.conformance.authzen.scenarios.search.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.PDPServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.util.List;

@PublishTestModule(
	testName = "authzen-pdp-action-search-results-evaluate-to-permit",
	displayName = "AuthZEN Action Search API - Section 8.1: Search results evaluate to a permitted decision",
	summary = "Per Section 8.1, any result from a Search API, when used in an Access Evaluation API call, SHOULD get decision true. "
		+ "The harness runs the search, then sends each of the first 10 results to the Access Evaluation API in place of the searched-for action, with the search's other entities and context. "
		+ "A result that is not permitted is reported as a warning, as the specification notes the outcome may depend on other variables such as time. "
		+ "A `delete` result is not sent: its fixture decision depends on `action.properties.soft` (decision rules 7 and 8), which a result built from an Action Search cannot carry. The PDP MAY list `delete`, and the harness neither requires nor rejects it (openid/authzen#780).\n" + AuthzenPDPActionSearchResultsEvaluateToPermitTest.payload,
	profile = "AuthZEN"
)
@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "static", configurationFields = {
	"pdp.access_evaluation_endpoint"
})
public class AuthzenPDPActionSearchResultsEvaluateToPermitTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return """
			{
				"results": [
					{ "name": "read" },
					{ "name": "write" }
				]
			}
			""";
	}

	@Override
	protected List<String> getOtherEndpointMetadataParameters() {
		// The search results are sent to the Access Evaluation API.
		return List.of("access_evaluation_endpoint");
	}

	@Override
	protected String reasonResultIsNotEvaluated(JsonObject result) {
		JsonElement name = result.get("name");
		if (OIDFJSON.isString(name) && "delete".equals(OIDFJSON.getString(name))) {
			return "the fixture decision for delete depends on action.properties.soft, which an Action Search result cannot carry";
		}
		return null;
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		evaluateSearchResultsWithAccessEvaluationApi("action");
		super.onPostAuthorizationFlowComplete();
	}
}
