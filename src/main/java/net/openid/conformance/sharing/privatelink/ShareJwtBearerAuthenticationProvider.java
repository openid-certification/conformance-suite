package net.openid.conformance.sharing.privatelink;

import net.openid.conformance.sharing.AssetSharing;
import net.openid.conformance.sharing.SharedAsset;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class ShareJwtBearerAuthenticationProvider implements AuthenticationProvider {

	private final AssetSharing assetSharing;

	private final UserDetailsService userDetailsService;

	public ShareJwtBearerAuthenticationProvider(AssetSharing assetSharing,
												PrivateLinkUserDetailsService userDetailsService) {
		this.assetSharing = assetSharing;
		this.userDetailsService = userDetailsService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		Object credentials = authentication.getCredentials();
		if (!(credentials instanceof String tokenValue)) {
			return null;
		}

		SharedAsset sharedAsset;
		try {
			sharedAsset = assetSharing.getSharedAssetFromSharingToken(tokenValue);
		} catch (BadCredentialsException e) {
			// Not a valid share JWT (bad signature, expired, wrong aud/iss, malformed, etc.)
			// Return null so the opaque-token provider gets a chance to handle this request.
			return null;
		}

		PrivateLinkOneTimeToken privateLinkOneTimeToken =
			PrivateLinkOneTimeToken.forSharedAsset(tokenValue, sharedAsset);

		UserDetails userDetails = userDetailsService.loadUserByUsername(privateLinkOneTimeToken.getUsername());
		OneTimeTokenAuthenticationToken authenticated =
			OneTimeTokenAuthenticationToken.authenticated(userDetails, userDetails.getAuthorities());
		authenticated.setDetails(privateLinkOneTimeToken);
		return authenticated;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
