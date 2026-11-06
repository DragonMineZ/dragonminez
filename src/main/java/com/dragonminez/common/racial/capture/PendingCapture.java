package com.dragonminez.common.racial.capture;

import java.util.UUID;

public record PendingCapture(UUID requesterId, String requesterName, long expiresAtMs) {

	public boolean isExpired() {
		return System.currentTimeMillis() > expiresAtMs;
	}
}
