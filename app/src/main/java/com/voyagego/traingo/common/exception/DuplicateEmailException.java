package com.voyagego.traingo.common.exception;

public class DuplicateEmailException extends BusinessRuleException {

	public DuplicateEmailException() {
		super("Email này đã có tài khoản.", "email");
	}

}
