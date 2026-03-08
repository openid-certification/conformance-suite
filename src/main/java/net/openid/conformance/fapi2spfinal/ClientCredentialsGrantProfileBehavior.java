package net.openid.conformance.fapi2spfinal;

/**
 * Client credentials grant profile — plain FAPI but with client credentials grant only.
 */
public class ClientCredentialsGrantProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean isClientCredentialsGrantOnly() {
		return true;
	}
}
