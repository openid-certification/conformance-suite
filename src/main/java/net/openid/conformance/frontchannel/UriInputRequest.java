package net.openid.conformance.frontchannel;

/**
 * A request for the user to paste a URI (e.g. an openid4vp:// authorization request) whose
 * query string should be submitted to the given endpoint.
 */
public class UriInputRequest {

	private String submitUrl;
	private String description;

	public UriInputRequest(String submitUrl, String description) {
		this.submitUrl = submitUrl;
		this.description = description;
	}

	public String getSubmitUrl() {
		return submitUrl;
	}

	public String getDescription() {
		return description;
	}
}
