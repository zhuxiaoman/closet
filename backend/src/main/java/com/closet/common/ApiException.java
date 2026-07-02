package com.closet.common;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final int code;
    public ApiException(int code, String message) { super(message); this.code = code; }
    public ApiException(String message) { this(500, message); }
}
