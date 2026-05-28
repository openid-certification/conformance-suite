package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-accept-content-type-with-charset",
	displayName = "Authzen Resource Search API - Spec 10.1-2: Accept Content-Type with charset",
	summary = "Per spec 10.1-2 and RFC 9110, `application/json; charset=utf-8` is a valid form of the JSON Content-Type. The PDP MUST accept it and return the expected response.",
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchAcceptContentTypeWithCharsetTest extends AbstractAuthzenPDPResourceSearchTest {

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
	protected String getRequestContentTypeOverride() {
		return "application/json; charset=utf-8";
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
}
