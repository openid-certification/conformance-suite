package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.PDPServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-results-evaluate-to-permit",
	displayName = "AuthZEN Resource Search API - Section 8.1: Search results evaluate to a permitted decision",
	summary = "Per Section 8.1, any result from a Search API, when used in an Access Evaluation API call, SHOULD get decision true. "
		+ "The harness runs the search, then sends each of the first 10 results to the Access Evaluation API in place of the searched-for resource, with the search's other entities and context. "
		+ "A result that is not permitted is reported as a warning, as the specification notes the outcome may depend on other variables such as time.\n" + AuthzenPDPResourceSearchResultsEvaluateToPermitTest.payload,
	profile = "AuthZEN"
)
@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "static", configurationFields = {
	"pdp.access_evaluation_endpoint"
})
public class AuthzenPDPResourceSearchResultsEvaluateToPermitTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" }
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
					{ "type": "record", "id": "record-1" }
				]
			}
			""";
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		evaluateSearchResultsWithAccessEvaluationApi("resource");
		super.onPostAuthorizationFlowComplete();
	}
}
