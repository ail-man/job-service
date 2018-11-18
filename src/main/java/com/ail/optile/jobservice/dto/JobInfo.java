package com.ail.optile.jobservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobInfo {

    private String name;
    private String command;
    private String cron;
    private Integer priority;
    private String state;
}
