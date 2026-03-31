package net.openid.conformance.testmodule;

/**
 * Callback interface for releasing and reacquiring the test module's lock around
 * blocking operations (outgoing HTTP calls, sleeps, etc.).
 *
 * Releasing the lock allows incoming HTTP requests to be processed while the
 * blocking operation is in progress, preventing deadlocks when the remote party
 * calls back to this test's endpoints.
 */
public interface TestLockManager {
	void releaseLock();
	void reacquireLock();

	/**
	 * Permanently disable lock release/reacquire. After this call, releaseLock() and
	 * reacquireLock() become no-ops. Used during final cleanup when the test is
	 * transitioning to FINISHED/INTERRUPTED and the status machine must not be disturbed.
	 */
	void disable();
}
