package net.openid.conformance.fapiciba.rp;

public abstract class AbstractFAPICIBAClientInvalidTokenEndpointInteractionIdTest
	extends AbstractFAPICIBAClientTest {

	@Override
	protected void tokenEndpointCallComplete() {
		startWaitingForTimeout();
		setStatus(Status.WAITING);
	}
}
