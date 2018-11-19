package com.ail.optile.jobservice.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class JobAlreadyExistsRestException extends JobServiceRestException {

    public JobAlreadyExistsRestException(Throwable cause) {
        super(cause);
    }
}
