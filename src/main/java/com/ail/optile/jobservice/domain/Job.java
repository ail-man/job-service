package com.ail.optile.jobservice.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    private String name;
    private String command;
    private String cron;
    private Integer priority;
    private State state;

    public enum State {
        QUEUED,
        RUNNING,
        SUCCESS,
        FAILED
    }
}
