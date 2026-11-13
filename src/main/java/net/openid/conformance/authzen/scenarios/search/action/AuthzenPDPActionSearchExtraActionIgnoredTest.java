package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "authzen-pdp-action-search-extra-action-ignored",
	displayName = "AuthZEN Action Search API - Sections 8.6.1 and 10.1.1: Action in an Action Search request is ignored",
	summary = "Section 8.6.1 omits `action` from the Action Search request, and this request carries one alongside the required `subject` and `resource`. "
		+ "Section 10.1.1 says receivers MUST ignore fields that are not defined for the request in question, and the AuthZEN working group confirmed in openid/authzen#766 that this covers an `action` member here: the PDP MUST ignore it, return HTTP 200 and give the same results as without it, which MUST include read and write. A PDP that rejects the request with HTTP 400, or treats `action` as a filter and leaves out write, fails. "
		+ "The harness first sends the same search without `action`, following every page, and warns if the two result sets differ: the two searches are separate requests, so a difference may come from results that changed between them, for example under a time-dependent policy, rather than from the PDP using `action`.\n" + AuthzenPDPActionSearchExtraActionIgnoredTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchExtraActionIgnoredTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"resource": { "type": "record", "id": "record-1" },
			"action": { "name": "read" }
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
	protected String[] additionalStatusRequirements() {
		// Section 8.6.1: an Action Search request carries subject and resource, not action;
		// Section 10.1.1: receivers MUST ignore fields not defined for the request.
		return new String[] {"AUTHZEN-8.6.1", "AUTHZEN-10.1.1"};
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
	protected BaseSearch getBaseSearch() {
		// Section 10.1.1: a field not defined for the request is ignored.
		return new BaseSearch("action", request -> {
			request.remove("action");
			return request;
		}, false, List.of("AUTHZEN-10.1.1"));
	}
}
