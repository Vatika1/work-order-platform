package com.vatika.workorder.shared.exception;

import java.util.UUID;

public class UploadNotCompletedException extends RuntimeException {
    public UploadNotCompletedException(UUID fileAssetId) {
        super("Upload not completed for file " + fileAssetId);
    }
}
