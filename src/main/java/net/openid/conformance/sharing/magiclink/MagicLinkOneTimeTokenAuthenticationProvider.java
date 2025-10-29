package net.openid.conformance.sharing.magiclink;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ott.InvalidOneTimeTokenException;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;

public class MagicLinkOneTimeTokenAuthenticationProvider implements AuthenticationProvider {

	private final OneTimeTokenService oneTimeTokenService;

	private final UserDetailsService userDetailsService;

	public MagicLinkOneTimeTokenAuthenticationProvider(OneTimeTokenService oneTimeTokenService,
											  UserDetailsService userDetailsService) {
		Assert.notNull(oneTimeTokenService, "oneTimeTokenService cannot be null");
		Assert.notNull(userDetailsService, "userDetailsService cannot be null");
		this.userDetailsService = userDetailsService;
		this.oneTimeTokenService = oneTimeTokenService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		var otpAuthenticationToken = (OneTimeTokenAuthenticationToken) authentication;
		var consumeOneTimeToken = this.oneTimeTokenService.consume(otpAuthenticationToken);
		if (consumeOneTimeToken == null) {
			throw new InvalidOneTimeTokenException("Invalid token");
		}
		var userDetails = this.userDetailsService.loadUserByUsername(consumeOneTimeToken.getUsername());
		var authenticated = OneTimeTokenAuthenticationToken.authenticated(userDetails, userDetails.getAuthorities());
		authenticated.setDetails(consumeOneTimeToken);
		return authenticated;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return OneTimeTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}

}
