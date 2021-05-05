package com.jslib.wood.apps;

public class AppsException extends RuntimeException {
	private static final long serialVersionUID = -4469058426561464601L;

	public AppsException(String message, Object... args) {
		super(String.format(message, args));
	}
}
