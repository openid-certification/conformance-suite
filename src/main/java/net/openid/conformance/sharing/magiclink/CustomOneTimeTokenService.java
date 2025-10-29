package net.openid.conformance.sharing.magiclink;

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
		// This is just a dummy, we call the sharing token generation with a share link button
		return assetSharing.generateSharingToken(null, null, null);
	}

	@Override
	public OneTimeToken consume(OneTimeTokenAuthenticationToken authenticationToken) {

		String tokenValue = authenticationToken.getTokenValue();

		SharedAsset sharedAsset = assetSharing.getSharedAssetFromSharingToken(tokenValue);
		MagicLinkOneTimeToken magicLinkOneTimeToken = new MagicLinkOneTimeToken();
		magicLinkOneTimeToken.setTokenValue(tokenValue);

		String username = "Guest " + Integer.toString(sharedAsset.getTokenId().hashCode(), 36);
		magicLinkOneTimeToken.setUsername(username);
		magicLinkOneTimeToken.setSharedAsset(sharedAsset);

		return magicLinkOneTimeToken;
	}
}
