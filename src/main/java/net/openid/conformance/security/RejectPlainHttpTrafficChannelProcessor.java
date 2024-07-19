package net.openid.conformance.security;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.channel.ChannelProcessor;

import java.util.Collection;

/**
 * Ensures that only https traffic is passed through the filter chain.
 */
public class RejectPlainHttpTrafficChannelProcessor implements ChannelProcessor {

	@Override
	public void decide(FilterInvocation invocation, Collection<ConfigAttribute> config) {

		if (!invocation.getHttpRequest().getScheme().equals("https")) {
			// It's important that the reverse proxy settings are correct - if we receive a request that appears to be http here, then, e.g., we will send the user to the http version of the login page when they logout.
			throw new RuntimeException("A non-https request has been received by the conformance suite. The external interface should always use https; if https is in use then there may be a problem with the reverse-proxy apache in front of the suite not setting the X-Forwarded-Proto (etc) http headers correctly.");
		}
	}

	@Override
	public boolean supports(ConfigAttribute attribute) {
		return true;
	}
}
