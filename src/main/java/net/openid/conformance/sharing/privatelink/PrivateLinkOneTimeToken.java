package net.openid.conformance.sharing.privatelink;

import net.openid.conformance.sharing.SharedAsset;
import org.springframework.security.authentication.ott.OneTimeToken;

import java.io.Serial;
import java.time.Instant;

public class PrivateLinkOneTimeToken implements OneTimeToken {

	@Serial
	private static final long serialVersionUID = 1L;

	protected String tokenValue;

	protected String username;

	protected Instant expiresAt;

	protected transient SharedAsset sharedAsset;

	public static PrivateLinkOneTimeToken forSharedAsset(String tokenValue, SharedAsset sharedAsset) {
		PrivateLinkOneTimeToken token = new PrivateLinkOneTimeToken();
		token.setTokenValue(tokenValue);
		token.setUsername("Guest " + Integer.toString(sharedAsset.getTokenId().hashCode(), 36));
		token.setSharedAsset(sharedAsset);
		return token;
	}

	@Override
	public String getTokenValue() {
		return tokenValue;
	}

	public void setTokenValue(String tokenValue) {
		this.tokenValue = tokenValue;
	}

	@Override
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public SharedAsset getSharedAsset() {
		return sharedAsset;
	}

	public void setSharedAsset(SharedAsset sharedAsset) {
		this.sharedAsset = sharedAsset;
	}
}
