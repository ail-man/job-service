package com.ail.optile.jobservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequest {

    private String name;
    private String command;
    private String cron;
    private Integer priority;
}
