package com.ail.optile.jobservice.rest.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NativeJobRequestDto {

    private String name;
    private String command;
    private String cron;
    private Integer priority;
}
