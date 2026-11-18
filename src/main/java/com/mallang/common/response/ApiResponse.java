package com.mallang.common.response;

public record ApiResponse<T>(boolean success, T data) {

    public static <T> ApiResponse<T> onSuccess(T data) {
        return new ApiResponse<>(true, data);
    }

    // 돌려줄 데이터가 없는 성공 (예: 삭제)
    public static ApiResponse<Void> onSuccess() {
        return new ApiResponse<>(true, null);
    }
}