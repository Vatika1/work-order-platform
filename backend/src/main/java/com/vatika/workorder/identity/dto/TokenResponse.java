package com.vatika.workorder.identity.dto;

public record TokenResponse(String accessToken, long expiresInSeconds) {}