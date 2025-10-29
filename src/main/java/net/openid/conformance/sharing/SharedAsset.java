package net.openid.conformance.sharing;

import java.util.Collections;
import java.util.Map;

public class SharedAsset {

	protected final String tokenId;

	protected final String planId;

	protected final String testId;

	protected final Map<String, String> owner;

	protected final String redirectUri;

	public SharedAsset(String tokenId, String planId, String testId, Map<String, String> owner, String redirectUri) {
		this.tokenId = tokenId;
		this.planId = planId;
		this.testId = testId;
		this.owner = Collections.unmodifiableMap(owner);
		this.redirectUri = redirectUri;
	}

	public String getTokenId() {
		return tokenId;
	}

	public String getPlanId() {
		return planId;
	}

	public String getTestId() {
		return testId;
	}

	public Map<String, String> getOwner() {
		return owner;
	}

	public String getRedirectUri() {
		return redirectUri;
	}

	@Override
	public String toString() {
		return "SharedAsset{" +
			"tokenId='" + tokenId + '\'' +
			", planId='" + planId + '\'' +
			", testId='" + testId + '\'' +
			", owner=" + owner +
			", redirectUri='" + redirectUri + '\'' +
			'}';
	}
}
