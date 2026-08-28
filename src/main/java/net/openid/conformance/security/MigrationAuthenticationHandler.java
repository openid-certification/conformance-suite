package net.openid.conformance.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.openid.conformance.info.SavedConfigurationService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.info.TestPlanService;
import net.openid.conformance.token.TokenService;
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
 * <strong>Trust boundary.</strong> Ownership transfer is irreversible and has no
 * undo, so it matters where these two claims come from. At the IdP they are
 * produced by an identity-provider (broker) mapper: they are populated from the
 * upstream provider's assertion when the account is federated, and are not backed
 * by a self-editable user attribute — a user cannot set {@code idp_sub} on their
 * own profile and take over another user's tests, plans, logs and screenshots.
 * That property lives at the IdP, not here, so if the mapper is ever replaced by
 * a user-writable attribute this handler becomes a one-click account-takeover
 * primitive and must change with it.
 * <p>
 * Note that {@code OidcUser#getClaimAsString} reads the merged claim set
 * (ID token plus UserInfo), not the ID token alone, so what is being relied on is
 * that the IdP does not expose a user-controlled {@code idp_iss} / {@code idp_sub}
 * on <em>either</em> — not that the value provably came from the signed ID token.
 * <p>
 * <strong>Known breakage: private share links.</strong> Every owner-scoped store
 * is migrated here except one that cannot be. A share link is a signed JWT
 * carrying the plan's owner from issue time, and it authorizes by feeding that
 * claim back as the principal, so once the plan's owner has been rewritten below
 * the link resolves to nothing. The claim is inside a signature, so it cannot be
 * updated in place, and the links are already distributed - some in submitted
 * certification packages - so they cannot be reissued to their holders either.
 * Links have to be regenerated after the owning user's first login through the
 * IdP and sent out again. See {@code AssetSharing}.
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
	private final TokenService tokenService;
	private final SavedConfigurationService savedConfigurationService;

	public MigrationAuthenticationHandler(TestPlanService testPlanService,
										  TestInfoService testInfoService,
										  TokenService tokenService,
										  SavedConfigurationService savedConfigurationService) {
		this.testPlanService = testPlanService;
		this.testInfoService = testInfoService;
		this.tokenService = tokenService;
		this.savedConfigurationService = savedConfigurationService;
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
		// These two are broker-mapped at the IdP and not user-writable - see the
		// trust boundary note on the class.
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
			// API tokens keep authenticating as the legacy identity, so leaving them
			// behind would silently empty every /api/** response they get.
			long tokens = tokenService.migrateOwnership(legacyIssuer, legacySubject);
			// Saved test configurations, which /api/lastconfig reads back.
			long configurations = savedConfigurationService.migrateOwnership(legacyIssuer, legacySubject);

			if (plans > 0 || !tests.movedNothing() || tokens > 0 || configurations > 0) {
				// Ownership transfer is irreversible and driven by claims from the
				// IdP, so who took over what has to be reconstructable afterwards.
				log.info("Migrated ownership from legacy identity. legacyIss={} legacySub={} newSub={} plans={} tests={} logEntries={} apiTokens={} savedConfigurations={}",
					legacyIssuer, legacySubject, principal.getSubject(), plans, tests.tests(), tests.logEntries(),
					tokens, configurations);
			}

		} catch (RuntimeException e) {
			log.error("Failed to migrate ownership from legacy identity; login continues. legacyIss={} legacySub={} newSub={}",
				legacyIssuer, legacySubject, principal.getSubject(), e);
		}
	}
}
