package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddIatToRequestObject;
import io.fintechlabs.testframework.condition.client.AddMultipleAudToRequestObject;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.SignRequestObject;

public abstract class AbstractFAPIRWEnsureServerAcceptsRequestObjectWithMultipleAud extends AbstractFAPIRWServerTestModule {

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		if (whichClient == 2) {
			callAndStopOnFailure(AddIatToRequestObject.class);
		}
		callAndStopOnFailure(AddExpToRequestObject.class);

		callAndStopOnFailure(AddMultipleAudToRequestObject.class, "RFC7519-4.1.3");

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

}
