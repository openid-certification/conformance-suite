package net.openid.conformance.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.info.TestPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * On login through the IdP, hands tests and plans that were created under the
 * user's pre-migration identity over to the identity the IdP now issues.
 * <p>
 * When the IdP brokers an upstream provider (the accounts users had before this
 * migration - Google, GitLab), it reports that provider's issuer and subject in
 * the {@code idp_iss} / {@code idp_sub} claims. Those are exactly the (iss, sub)
 * pair the old login path stored as the {@code owner} of every test and plan, so
 * they are what we look the legacy records up by.
 * <p>
 * The claims are read from the ID token the IdP signed, so they are trusted input:
 * a user cannot ask to take over another user's records by supplying them.
 * <p>
 * <strong>Temporary.</strong> This runs on every login, including the many after
 * the user's records have already been migrated, where it costs two indexed
 * updates that match nothing. That is deliberate: there is no marker saying a
 * given user has been migrated, and adding one to carry a one-off data fix-up is
 * not worth it. Once the pre-IdP accounts have had time to log in at least once,
 * this handler and the {@code migrateOwnership} methods behind it should be
 * deleted outright.
 */
@Component
public class MigrationAuthenticationHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private static final Logger log = LoggerFactory.getLogger(MigrationAuthenticationHandler.class);

	private final TestPlanService testPlanService;
	private final TestInfoService testInfoService;

	public MigrationAuthenticationHandler(TestPlanService testPlanService, TestInfoService testInfoService) {
		this.testPlanService = testPlanService;
		this.testInfoService = testInfoService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

		migrateOwnership(authentication);

		super.onAuthenticationSuccess(request, response, authentication);
	}

	private void migrateOwnership(Authentication authentication) {
		// Not every principal reaching this handler carries an ID token, so this
		// must be a type test rather than a cast - a ClassCastException here would
		// surface as a failed login.
		if (!(authentication.getPrincipal() instanceof OidcUser principal)) {
			return;
		}

		// Gate on the claims actually used below. The previous gate was a separate
		// "idp" claim, which let a token carrying "idp" but neither sub-claim fall
		// through to a no-op, and skipped a token that had the pair without "idp".
		String legacyIssuer = principal.getClaimAsString("idp_iss");
		String legacySubject = principal.getClaimAsString("idp_sub");
		if (legacyIssuer == null || legacySubject == null) {
			return;
		}

		// Best effort: ownership migration is a data fix-up, not a precondition for
		// being logged in, so a database problem must not lock the user out. It is
		// retried on their next login.
		try {
			long plans = testPlanService.migrateOwnership(legacyIssuer, legacySubject);
			// Also moves the test's log entries and screenshots, which are
			// access-controlled on their own copy of the owner.
			TestInfoService.MigrationCounts tests = testInfoService.migrateOwnership(legacyIssuer, legacySubject);
			if (plans > 0 || !tests.movedNothing()) {
				// Ownership transfer is irreversible and driven by claims from the
				// IdP, so who took over what has to be reconstructable afterwards.
				log.info("Migrated ownership from legacy identity. legacyIss={} legacySub={} newSub={} plans={} tests={} logEntries={}",
					legacyIssuer, legacySubject, principal.getSubject(), plans, tests.tests(), tests.logEntries());
			}
		} catch (RuntimeException e) {
			log.error("Failed to migrate ownership from legacy identity; login continues. legacyIss={} legacySub={} newSub={}",
				legacyIssuer, legacySubject, principal.getSubject(), e);
		}
	}
}
