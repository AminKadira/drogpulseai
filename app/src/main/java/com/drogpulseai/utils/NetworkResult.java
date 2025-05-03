package com.drogpulseai.utils;

import com.google.gson.annotations.SerializedName;

/**
 * Generic class for standardized API responses
 * @param <T> Type of data returned by the API
 */
public class NetworkResult<T> {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data;

    // Constructors
    public NetworkResult() {
    }

    public NetworkResult(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Factory methods for common responses
    public static <T> NetworkResult<T> success(T data) {
        return new NetworkResult<>(true, "Success", data);
    }

    public static <T> NetworkResult<T> error(String message) {
        return new NetworkResult<>(false, message, null);
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}