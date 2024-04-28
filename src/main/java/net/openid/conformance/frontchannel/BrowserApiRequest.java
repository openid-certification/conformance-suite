package net.openid.conformance.frontchannel;

import com.google.gson.JsonObject;

public class BrowserApiRequest {

	private JsonObject request;
	private String submitUrl;

	public BrowserApiRequest(JsonObject request, String submitUrl) {
		this.request = request;
		this.submitUrl = submitUrl;
	}

	public JsonObject getRequest() {
		return request;
	}

	public String getSubmitUrl() {
		return submitUrl;
	}
}
