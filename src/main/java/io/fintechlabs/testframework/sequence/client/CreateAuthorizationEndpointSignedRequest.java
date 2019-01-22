package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.SignRequestObject;

/**
 * @author jricher
 *
 */
public class CreateAuthorizationEndpointSignedRequest extends CreateAuthorizationEndpointRequest {

	public CreateAuthorizationEndpointSignedRequest() {

		super();

		this.with("authorization_redirect",
			condition(ConvertAuthorizationEndpointRequestToRequestObject.class),
			condition(SignRequestObject.class),
			condition(BuildRequestObjectRedirectToAuthorizationEndpoint.class));

	}
}
