package net.openid.conformance.condition.as;

/**
 * Allocates a status list reference at an index the served status list marks as VALID, so that
 * verifiers exercise the status fetch on a credential whose status is good.
 */
public class CreateValidStatusListReference extends AbstractCreateStatusListReference {

	@Override
	protected boolean revoked() {
		return false;
	}
}
