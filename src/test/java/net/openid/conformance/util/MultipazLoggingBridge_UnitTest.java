package net.openid.conformance.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipazLoggingBridge_UnitTest {

	private final Logger multipazRoot = (Logger) LoggerFactory.getLogger("org.multipaz");
	private final Logger tagLogger = (Logger) LoggerFactory.getLogger("org.multipaz.SomeTag");
	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
	private Level previousLevel;

	@BeforeEach
	public void setUp() {
		previousLevel = multipazRoot.getLevel();
		appender.start();
		tagLogger.addAppender(appender);
	}

	@AfterEach
	public void tearDown() {
		tagLogger.detachAppender(appender);
		multipazRoot.setLevel(previousLevel);
	}

	@Test
	public void routesMultipazLogCallsToSlf4jUnderTheTagAndHonoursTheConfiguredLevel() {
		multipazRoot.setLevel(Level.INFO);
		new MultipazLoggingBridge().install();

		org.multipaz.util.Logger.INSTANCE.w("SomeTag", "something odd");
		org.multipaz.util.Logger.INSTANCE.i("SomeTag", "note");
		org.multipaz.util.Logger.INSTANCE.d("SomeTag", "chatter");
		org.multipaz.util.Logger.INSTANCE.e("SomeTag", "broken", new IllegalStateException("boom"));

		assertThat(appender.list).extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple(Level.WARN, "something odd"),
				org.assertj.core.groups.Tuple.tuple(Level.INFO, "note"),
				org.assertj.core.groups.Tuple.tuple(Level.ERROR, "broken"));
		assertThat(appender.list.get(2).getThrowableProxy().getMessage()).isEqualTo("boom");
		assertThat(org.multipaz.util.Logger.INSTANCE.isDebugEnabled()).isFalse();
	}

	@Test
	public void debugOutputFollowsTheSlf4jLevel() {
		multipazRoot.setLevel(Level.DEBUG);
		new MultipazLoggingBridge().install();

		org.multipaz.util.Logger.INSTANCE.d("SomeTag", "chatter");

		assertThat(org.multipaz.util.Logger.INSTANCE.isDebugEnabled()).isTrue();
		assertThat(appender.list).extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
			.containsExactly(org.assertj.core.groups.Tuple.tuple(Level.DEBUG, "chatter"));
	}
}
