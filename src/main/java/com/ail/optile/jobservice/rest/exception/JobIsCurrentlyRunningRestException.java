package com.ail.optile.jobservice.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.IM_USED)
public class JobIsCurrentlyRunningRestException extends JobServiceRestException {

    public JobIsCurrentlyRunningRestException(Throwable cause) {
        super(cause);
    }
}
