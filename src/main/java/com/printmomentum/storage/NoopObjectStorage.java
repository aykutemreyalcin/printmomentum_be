package com.printmomentum.storage;

public final class NoopObjectStorage implements ObjectStorage {

	@Override
	public void put(String key, byte[] bytes, String contentType) {
		// Local / default: keep the Etsy image URL; do not upload.
	}

	@Override
	public boolean writable() {
		return false;
	}
}
