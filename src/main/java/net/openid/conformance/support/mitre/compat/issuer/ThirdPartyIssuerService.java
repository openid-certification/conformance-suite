package net.openid.conformance.support.mitre.compat.issuer;

import com.google.common.base.Strings;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.support.mitre.compat.clients.IssuerServiceResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.security.authentication.AuthenticationServiceException;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * Determines the issuer using an account chooser or other third-party-initiated login
 *
 * @author jricher
 *
 */
public class ThirdPartyIssuerService implements IssuerService {

	private String accountChooserUrl;

	private Set<String> whitelist = new HashSet<>();
	private Set<String> blacklist = new HashSet<>();

	/* (non-Javadoc)
	 * @see org.mitre.openid.connect.client.service.IssuerService#getIssuer(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public IssuerServiceResponse getIssuer(HttpServletRequest request) {

		// if the issuer is passed in, return that
		String iss = request.getParameter("iss");
		if (!Strings.isNullOrEmpty(iss)) {
			if (!whitelist.isEmpty() && !whitelist.contains(iss)) {
				throw new AuthenticationServiceException("Whitelist was nonempty, issuer was not in whitelist: " + iss);
			}

			if (blacklist.contains(iss)) {
				throw new AuthenticationServiceException("Issuer was in blacklist: " + iss);
			}

			return new IssuerServiceResponse(iss, request.getParameter("login_hint"), request.getParameter("target_link_uri"));
		} else {

			try {
				// otherwise, need to forward to the account chooser
				String redirectUri = request.getRequestURL().toString();
				URIBuilder builder = new URIBuilder(accountChooserUrl);

				builder.addParameter("redirect_uri", redirectUri);

				return new IssuerServiceResponse(builder.build().toString());

			} catch (URISyntaxException e) {
				throw new AuthenticationServiceException("Account Chooser URL is not valid", e);
			}


		}

	}

	/**
	 * @return the accountChooserUrl
	 */
	public String getAccountChooserUrl() {
		return accountChooserUrl;
	}

	/**
	 * @param accountChooserUrl the accountChooserUrl to set
	 */
	public void setAccountChooserUrl(String accountChooserUrl) {
		this.accountChooserUrl = accountChooserUrl;
	}

	/**
	 * @return the whitelist
	 */
	public Set<String> getWhitelist() {
		return whitelist;
	}

	/**
	 * @param whitelist the whitelist to set
	 */
	public void setWhitelist(Set<String> whitelist) {
		this.whitelist = whitelist;
	}

	/**
	 * @return the blacklist
	 */
	public Set<String> getBlacklist() {
		return blacklist;
	}

	/**
	 * @param blacklist the blacklist to set
	 */
	public void setBlacklist(Set<String> blacklist) {
		this.blacklist = blacklist;
	}

	@PostConstruct
	public void afterPropertiesSet() {
		if (Strings.isNullOrEmpty(this.accountChooserUrl)) {
			throw new IllegalArgumentException("Account Chooser URL cannot be null or empty");
		}

	}

}
