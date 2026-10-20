package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.AbstractCondition;

/**
 * Changes one member of a search request before it is sent again with a page token, which Section
 * 8.2 requires to repeat the preceding request's entities and pagination parameters.
 */
public abstract class AbstractChangeAuthzenSearchRequest extends AbstractCondition {

	/** The member this condition changes, as named in the log, e.g. "page.limit" or "action". */
	public abstract String getChangedMember();
}
