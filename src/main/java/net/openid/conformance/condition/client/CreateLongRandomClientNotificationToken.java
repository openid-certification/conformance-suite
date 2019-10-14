package net.openid.conformance.condition.client;

public class CreateLongRandomClientNotificationToken extends CreateRandomClientNotificationToken {
	@Override
	protected Integer requestedLength() {
		return 1024;
	}

}
