package cz.mfcr.hazard.demo.error;

public class ClientError extends RuntimeException {
    public ClientError() {
    }

    public ClientError(String message) {
        super(message);
    }

    public ClientError(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientError(Throwable cause) {
        super(cause);
    }

    public ClientError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
