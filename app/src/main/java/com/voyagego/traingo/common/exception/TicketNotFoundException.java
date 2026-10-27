package com.voyagego.traingo.common.exception;

/**
 * No such ticket for this customer. A ticket of someone else's booking reads
 * the same, so its code is never revealed (docs/product/auth.md).
 */
public class TicketNotFoundException extends ResourceNotFoundException {

	public TicketNotFoundException() {
		super("Không tìm thấy vé.");
	}

}
