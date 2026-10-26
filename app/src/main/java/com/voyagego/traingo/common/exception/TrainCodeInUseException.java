package com.voyagego.traingo.common.exception;

/**
 * A deleted train cannot come back while another train uses its code: two
 * trains in use would then share one code.
 */
public class TrainCodeInUseException extends BusinessRuleException {

	public TrainCodeInUseException(String code) {
		super("Không khôi phục được: mã " + code + " đang được một tàu khác dùng. Đổi mã tàu đó trước.");
	}

}
