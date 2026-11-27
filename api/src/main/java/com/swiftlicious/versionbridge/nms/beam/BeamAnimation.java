package com.swiftlicious.versionbridge.nms.beam;

public interface BeamAnimation {

	/**
	 * Executes one frame of the beam animation.
	 */
	void run();

	/**
	 * @return true if the beam animation has finished and should be removed/stopped
	 */
	boolean isFinished();

	/**
	 * Stops the animation and restores any client-side changes. Must be called on
	 * the server thread.
	 */
	default void cancel() {
		// Implementations with client-side state must override this.
	}
}