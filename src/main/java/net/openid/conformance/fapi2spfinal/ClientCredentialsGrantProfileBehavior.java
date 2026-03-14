package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for FAPI client credentials grant.
 * Uses standard FAPI resource configuration, with client credentials grant flow.
 */
public class ClientCredentialsGrantProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean isClientCredentialsGrantOnly() {
		return true;
	}
}
