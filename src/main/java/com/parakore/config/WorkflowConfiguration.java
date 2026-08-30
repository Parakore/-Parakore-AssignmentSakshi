package com.parakore.config;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowConfiguration {

    private List<Transition> transitions;

    @Data
    public static class Transition {

        private String from;
        private String action;
        private String to;
        private List<String> roles;
    }
}