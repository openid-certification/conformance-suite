package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-extra-resource-id-ignored",
	displayName = "AuthZEN Resource Search API - Section 8.5.1: Extra resource.id ignored",
	summary = "Section 8.5.1 says resource.id SHOULD be omitted from Resource Search and, if present, MUST be ignored. This test sends a stray `resource.id` alongside the required `resource.type`; PDP MUST still return the expected results. Scenario 7.3.3 sends the fixture id record-1; this module sends `ignored-stray-id`, which names no entity, so a PDP that filters on the id returns none of the expected records and fails. The harness first sends the same search without resource.id, following every page, and compares the two result sets, which scenario 7.3.3 requires to be identical. A difference in the fixture entities fails, as the fixture decisions cannot change between the requests. A difference in other results is a warning: the two searches are separate requests, so such results may have changed between them, for example under a time-dependent policy, rather than because the PDP used resource.id.\n" + AuthzenPDPResourceSearchExtraResourceIdIgnoredTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchExtraResourceIdIgnoredTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "ignored-stray-id" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return """
			{
				"results": [
					{ "type": "record", "id": "record-1" },
					{ "type": "record", "id": "record-2" }
				]
			}
			""";
	}

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 8.5.1: the resource id, "if present, MUST be ignored".
		return new String[] {"AUTHZEN-8.5.1"};
	}

	@Override
	protected BaseSearch getBaseSearch() {
		// Section 8.5.1: the id, "if present, MUST be ignored".
		return new BaseSearch("resource.id", request -> {
			request.getAsJsonObject("resource").remove("id");
			return request;
		}, false, List.of("AUTHZEN-8.5.1"));
	}
}
