package com.langchain4j.examples;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExchangeTool {

    private static final Logger log = LoggerFactory.getLogger(ExchangeTool.class);

    @Tool("Exchange the given amount of money from the original to the target currency")
    Double exchange(@P("originalCurrency") String originalCurrency, @P("amount") Double amount, @P("targetCurrency") String targetCurrency) {
        // Invoke a REST service to get the exchange rate
        log.info("【工具】调用外部服务获取汇率");
        return  1.0;
    }
}