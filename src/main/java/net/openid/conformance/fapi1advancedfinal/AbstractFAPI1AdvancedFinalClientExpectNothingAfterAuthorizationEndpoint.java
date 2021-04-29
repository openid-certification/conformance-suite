package net.openid.conformance.fapi1advancedfinal;

public abstract class AbstractFAPI1AdvancedFinalClientExpectNothingAfterAuthorizationEndpoint extends AbstractFAPI1AdvancedFinalClientTest {

	@Override
	protected Object authorizationEndpoint(String requestId){

		Object returnValue = super.authorizationEndpoint(requestId);

		startWaitingForTimeout();

		return returnValue;
	}

}
