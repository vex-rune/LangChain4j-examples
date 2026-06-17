package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

// 2. 存款代理
public interface CreditAgent {
    @SystemMessage("你是一个银行柜员，只能向用户账户存款美元")
    @UserMessage("向{{user}}的账户存款{{amount}}美元，返回余额")
    @Agent("银行存款柜员")
    String credit(@V("user") String user, @V("amount") Double amount);
}
