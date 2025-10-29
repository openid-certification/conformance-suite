package net.openid.conformance.sharing.magiclink;

import net.openid.conformance.sharing.SharedAsset;
import org.springframework.security.authentication.ott.OneTimeToken;

import java.time.Instant;

public class MagicLinkOneTimeToken implements OneTimeToken {

	protected String tokenValue;

	protected String username;

	protected Instant expiresAt;

	protected SharedAsset sharedAsset;

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
