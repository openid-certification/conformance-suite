package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 8.2: the PEP repeats the request "until the PDP returns a page object in which the value
 * of the next_token field is an empty string". The suite follows a bounded number of pages, and a
 * PDP may hold more matching entities than fit in them (certification scenario 4.6: "Additional
 * entities are permitted"), so a token still outstanding at the bound is flagged rather than
 * failed, and the results of the pages followed are matched. Called only when the last page
 * followed returned a page token, and intended to be called at WARNING.
 */
public class WarnSearchPageCapReached extends AbstractCondition {

	private final int maxPages;

	public WarnSearchPageCapReached(int maxPages) {
		this.maxPages = maxPages;
	}

	@Override
	@PreEnvironment(strings = "authzen_search_endpoint_request_page_token")
	public Environment evaluate(Environment env) {
		String token = env.getString("authzen_search_endpoint_request_page_token");
		throw error("The PDP was still returning a page token after " + maxPages + " pages, so the suite stopped "
			+ "following them; only the results of those pages are matched",
			args("next_token", token, "pages", maxPages));
	}
}
