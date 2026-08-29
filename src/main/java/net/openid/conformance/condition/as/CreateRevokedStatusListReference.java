package net.openid.conformance.condition.as;

/**
 * Allocates a status list reference at an index the served status list marks as revoked.
 */
public class CreateRevokedStatusListReference extends AbstractCreateStatusListReference {

	@Override
	protected boolean revoked() {
		return true;
	}
}
