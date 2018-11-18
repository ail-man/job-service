package com.ail.optile.jobservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class JobServiceException extends RuntimeException {

    public JobServiceException(Throwable cause) {
        super(cause);
    }
}
