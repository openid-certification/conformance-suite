package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-extra-subject-id-ignored",
	displayName = "AuthZEN Subject Search API - Section 8.4.1: Extra subject.id ignored",
	summary = "Section 8.4.1 says subject.id SHOULD be omitted from Subject Search and, if present, MUST be ignored. This test sends a stray `subject.id` alongside the required `subject.type`; PDP MUST still return the expected results. Scenario 7.2.3 sends the fixture id alice; this module sends `ignored-stray-id`, which names no entity, so a PDP that filters on the id returns none of the expected users and fails. The harness first sends the same search without subject.id, following every page, and warns if the two result sets differ: scenario 7.2.3 requires them to be identical, but the two searches are separate requests, so a difference may come from results that changed between them, for example under a time-dependent policy, rather than from the PDP using subject.id.\n" + AuthzenPDPSubjectSearchExtraSubjectIdIgnoredTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchExtraSubjectIdIgnoredTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "ignored-stray-id" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" }
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
					{ "type": "user", "id": "alice" },
					{ "type": "user", "id": "bob" }
				]
			}
			""";
	}

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 8.4.1: the subject id, "if present, MUST be ignored".
		return new String[] {"AUTHZEN-8.4.1"};
	}

	@Override
	protected BaseSearch getBaseSearch() {
		// Section 8.4.1: the id, "if present, MUST be ignored".
		return new BaseSearch("subject.id", request -> {
			request.getAsJsonObject("subject").remove("id");
			return request;
		}, false, List.of("AUTHZEN-8.4.1"));
	}
}
