package net.openid.conformance.sharing.privatelink;

import net.openid.conformance.sharing.AssetSharing;
import net.openid.conformance.sharing.SharedAsset;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;

public class CustomOneTimeTokenService implements OneTimeTokenService {

	private final AssetSharing assetSharing;

	public CustomOneTimeTokenService(AssetSharing assetSharing) {
		this.assetSharing = assetSharing;
	}

	@Override
	@SuppressWarnings("unchecked")
	public OneTimeToken generate(GenerateOneTimeTokenRequest request) {
		// The sharing token generation is done via the share link button
		return null;
	}

	@Override
	// The OneTimeToken can be consumed indefinitely. Expiry is enforced via the 'exp' claim
	// of the token value JWT.
	public OneTimeToken consume(OneTimeTokenAuthenticationToken authenticationToken) {
		String tokenValue = authenticationToken.getTokenValue();
		SharedAsset sharedAsset = assetSharing.getSharedAssetFromSharingToken(tokenValue);
		return PrivateLinkOneTimeToken.forSharedAsset(tokenValue, sharedAsset);
	}
}
