package com.ail.optile.jobservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.IM_USED)
public class JobIsCurrentlyRunningException extends RuntimeException {
}
