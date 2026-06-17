package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

// 1. 取款代理
public interface WithdrawAgent {
    @SystemMessage("你是一个银行柜员，只能从用户账户取款美元")
    @UserMessage("从{{user}}的账户取款{{amount}}美元，返回余额")
    @Agent("银行取款柜员")
    String withdraw(@V("user") String user, @V("amount") Double amount);
}
