package io.fintechlabs.testframework.openid.nonvariantversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * response mode
 */
public class ResponseMode {
	private Logger logger = LoggerFactory.getLogger(ResponseMode.class);

	public enum ResponseModeValue {query, fragment, form_post};
	private ResponseModeValue responseModeValue;

	public ResponseMode(String responseModeStr, ResponseType responseType) {
		try {
			this.responseModeValue = ResponseModeValue.valueOf(responseModeStr);
		} catch (IllegalArgumentException ex) {
			logger.error("Illegal response_mode value:'" +responseModeStr+ "'", ex);
			this.responseModeValue = responseType.getDefaultResponseMode();
		}
	}

	public String getResponseModeString() {
		return this.responseModeValue.toString();
	}

	public boolean isQuery()
	{
		return responseModeValue == ResponseModeValue.query;
	}

	public boolean isFragment()
	{
		return responseModeValue == ResponseModeValue.fragment;
	}

	public boolean isFormPost()
	{
		return responseModeValue == ResponseModeValue.form_post;
	}
}
