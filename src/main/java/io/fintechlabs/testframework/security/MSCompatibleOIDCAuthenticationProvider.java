package io.fintechlabs.testframework.security;

import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.PendingOIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

public class MSCompatibleOIDCAuthenticationProvider extends OIDCAuthenticationProvider {
	private static final Logger logger = LoggerFactory.getLogger(MSCompatibleOIDCAuthenticationProvider.class);
	/**
	 * OIDCAuthenticationProvider ignores claims in id token and assumes that userinfo
	 * contains the claims but in the Microsoft case, userinfo only contains the sub
	 * and other claims like name and email are provided in id tokens.
	 * We just need the name and email if they are not set in userinfo.
	 * @param token
	 * @param authorities
	 * @param userInfo
	 * @return
	 */
	@Override
	protected Authentication createAuthenticationToken(PendingOIDCAuthenticationToken token, Collection<? extends GrantedAuthority> authorities, UserInfo userInfo) {
		try {
			Map<String, Object> idTokenClaims = token.getIdToken().getJWTClaimsSet().getClaims();
			if (idTokenClaims != null)
			{
				if (userInfo.getName() == null && idTokenClaims.containsKey("name"))
				{
					userInfo.setName(String.valueOf(idTokenClaims.get("name")));
				}
				if (userInfo.getEmail() == null && idTokenClaims.containsKey("email"))
				{
					userInfo.setEmail(String.valueOf(idTokenClaims.get("email")));
				}
			}
		}
		catch(ParseException parseException) {
			logger.error("Failed to parse id token claims", parseException);
		}
		return new OIDCAuthenticationToken(token.getSub(),
			token.getIssuer(),
			userInfo, authorities,
			token.getIdToken(), token.getAccessTokenValue(), token.getRefreshTokenValue());
	}

}
