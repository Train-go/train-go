package com.voyagego.traingo.common.exception;

public class DuplicateEmailException extends RuntimeException {
	public DuplicateEmailException() {
		super("Email này đã có tài khoản.");
	}

	public String getField() {
		return "email";
	}
}
