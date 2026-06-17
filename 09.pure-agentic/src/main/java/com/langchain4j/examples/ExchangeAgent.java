package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

// 3. 汇率代理
public interface ExchangeAgent {
    @SystemMessage("你是一个外汇兑换员，可以进行货币兑换")
    @UserMessage("将{{amount}}{{originalCurrency}}兑换成{{targetCurrency}}，返回兑换后的金额")
    @Agent("外汇兑换员")
    Double exchange(@V("originalCurrency") String originalCurrency, @V("amount") Double amount, @V("targetCurrency") String targetCurrency);
}
