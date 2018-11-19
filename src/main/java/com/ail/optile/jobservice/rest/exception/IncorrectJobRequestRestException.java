package com.ail.optile.jobservice.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class IncorrectJobRequestRestException extends JobServiceRestException {

    public IncorrectJobRequestRestException(Throwable cause) {
        super(cause);
    }
}
