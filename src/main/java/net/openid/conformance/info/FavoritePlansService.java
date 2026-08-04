package net.openid.conformance.info;

import java.util.List;

/**
 * Manages the set of favorited test-plan names for the currently logged-in user.
 *
 * <p>Favorites are a per-user set of test-plan names rendered as a chronological list
 * (most-recently-added last). Unlike {@link SavedConfigurationService}, which keeps a single
 * latest record per owner, favorites are stored one document per favorited plan.
 */
public interface FavoritePlansService {

	/**
	 * Maximum favorites one user may hold. A bound on what an authenticated client can write
	 * through this API, not a product limit anyone is expected to reach — the picker lists a few
	 * hundred plans in total.
	 */
	int MAX_FAVORITES_PER_USER = 100;

	/**
	 * @return the current user's favorited plan names in insertion order (most-recently-added
	 *     last). Never {@code null}; an empty list is returned when there is no authenticated
	 *     user or the user has no favorites.
	 */
	List<String> getFavoritePlansForCurrentUser();

	/**
	 * Add a plan to the current user's favorites. Idempotent: adding a plan that is already
	 * favorited is a no-op.
	 *
	 * @param planName the test-plan name to favorite
	 * @return the updated favorites list
	 * @throws FavoritePlansLimitExceededException when the user already holds
	 *     {@link #MAX_FAVORITES_PER_USER} favorites and {@code planName} is not one of them
	 */
	List<String> addFavoritePlanForCurrentUser(String planName);

	/**
	 * Remove a plan from the current user's favorites. Removing a plan that is not favorited is
	 * a no-op success.
	 *
	 * @param planName the test-plan name to unfavorite
	 * @return the updated favorites list
	 */
	List<String> removeFavoritePlanForCurrentUser(String planName);

	/**
	 * Create the Mongo indexes backing the favorites collection: an index on {@code owner} for
	 * per-user lookups and a unique compound index on {@code owner + planName} that enforces
	 * idempotency at the database level.
	 */
	void createIndexes();

}
