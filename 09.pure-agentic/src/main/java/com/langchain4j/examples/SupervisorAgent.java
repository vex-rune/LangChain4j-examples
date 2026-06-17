package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface SupervisorAgent {
    @Agent
    String invoke(@V("request") String request);
}