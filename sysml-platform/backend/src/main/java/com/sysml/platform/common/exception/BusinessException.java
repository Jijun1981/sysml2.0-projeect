package com.sysml.platform.common.exception;

import lombok.Getter;

/** 业务异常 - 对应错误码 */
@Getter
public class BusinessException extends RuntimeException {

  private final String code;
  private final String messageKey;

  public BusinessException(String code, String message) {
    super(message);
    this.code = code;
    this.messageKey = "error." + code.toLowerCase().replace("_", ".");
  }

  public BusinessException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.messageKey = "error." + code.toLowerCase().replace("_", ".");
  }
}
