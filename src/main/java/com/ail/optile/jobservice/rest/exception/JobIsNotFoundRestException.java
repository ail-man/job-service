package com.ail.optile.jobservice.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class JobIsNotFoundRestException extends JobServiceRestException {

    public JobIsNotFoundRestException(Throwable cause) {
        super(cause);
    }
}
