package com.sparta.coupon.application.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;


@Getter
@AllArgsConstructor
@FieldDefaults(makeFinal = true)
public enum Error {

    NOT_FOUND_USER(1000, "존재하지 않는 사용자입니다."),
    ALREADY_EXIST_EMAIL(1001, "이미 존재하는 이메일입니다."),
    NOT_CORRECT_CERTIFICATION_NUMBER(1002, "인증번호가 틀렸습니다."),
    EXPIRED_CERTIFICATION_NUMBER(1003, "인증번호가 만료되었습니다."),
    INVALID_PASSWORD(1004, "비밀번호가 맞지 않습니다."),
    ACCOUNT_NOT_ENABLED(1005, "활성화 되지 않은 계정입니다."),
    ALREADY_EXIST_ID(1006, "이미 존재하는 ID 입니다."),

    NOT_FOUND_COUPON(2000, "존재하지 않는 쿠폰입니다."),
    FOUND_ISSUED_COUPON_ERROR(2001, "발급된 쿠폰 상태 조회 중 오류가 발생했습니다."),
    NOT_FOUND_ISSUED_COUPON(2002, "발급된 쿠폰을 찾을 수 없습니다."),
    ISSUE_NOT_VALID_TIME(2100, "쿠폰 발급 기간이 아닙니다."),
    COUPON_EXHAUSTED(2101, "쿠폰이 모두 소진되었습니다."),
    USED_COUPON(2102, "이미 사용된 쿠폰입니다."),
    EXPIRED_COUPON(2103, "만료된 쿠폰입니다."),
    CANCEL_UNAVAILABLE_COUPON(2104, "사용 취소가 불가능한 쿠폰입니다."),
    UNAVAILABLE_COUPON(2105, "발급된  쿠폰을 찾을 수 없거나 접근 권한이 없습니다."),
    NOT_VALID_END_TIME(2106, "쿠폰 발급 종료 시간은 쿠폰 발급 시작 시간 이후여야 합니다."),
    ISSUE_COUPON_LATER(2107, "쿠폰 발급 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),
    NOT_VALID_EXPIRE_TIME(2108, "쿠폰 만료 시간은 쿠폰 발급 종료 시간 이후여야 합니다."),

    JSON_PROCESSING_ERROR(9996, "JSON 처리에 오류가 있습니다."),
    METHOD_ARGUMENT_NOT_VALID(9997, "유효하지 않은 값입니다."),
    FORBIDDEN(9998, "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(9999, "서버 오류입니다."),
    CIRCUIT_BREAKER_OPEN(10000, "이용량 증가로 현재 서비스가 불가능합니다."),
    SERVER_TIMEOUT(10001, "응답 시간을 초과하였습니다."),
    NOT_VALID_ROLE_ENUM(10002, "유효하지 않은 권한입니다."),
    INVALID_UPDATE_REQUEST(10003, "수정할 내용이 없습니다."),
    REQUIRED_HEADER(10004, "헤더에서 X-Id는 필수입니다."),
    INVALID_HEADER(10005, "잘못된 X-Id 형식입니다."),
    NOT_FOUND_HEADER(10006, "현재 컨텍스트에서 X-Id를 찾을 수 없습니다.");

    Integer code;
    String message;

}
