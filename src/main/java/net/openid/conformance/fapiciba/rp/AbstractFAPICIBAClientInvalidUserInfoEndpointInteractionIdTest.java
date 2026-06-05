package net.openid.conformance.fapiciba.rp;

public abstract class AbstractFAPICIBAClientInvalidUserInfoEndpointInteractionIdTest
	extends AbstractFAPICIBAClientTest {

	@Override
	protected void resourceEndpointCallComplete() {
		startWaitingForTimeout();
		setStatus(Status.WAITING);
	}
}
