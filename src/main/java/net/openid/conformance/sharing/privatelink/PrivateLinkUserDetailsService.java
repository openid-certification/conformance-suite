package net.openid.conformance.sharing.privatelink;

import net.openid.conformance.security.OIDCAuthenticationFacade;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class PrivateLinkUserDetailsService implements UserDetailsService {

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		var user = User.withUsername(username)
			.authorities(OIDCAuthenticationFacade.ROLE_PRIVATE_LINK_USER, OIDCAuthenticationFacade.ROLE_USER)
			.password("<nopassword>")
			.build();
		return user;
	}

}
