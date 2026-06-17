package com.langchain4j.examples;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.HashMap;
import java.util.Map;

public class BankTool {

    private final Map<String, Double> accounts = new HashMap<>();

    void createAccount(String user, Double initialBalance) {
        if (accounts.containsKey(user)) {
            throw new RuntimeException("Account for user " + user + " already exists");
        }
        accounts.put(user, initialBalance);
        System.out.println("Created account for " + user + " with initial balance " + initialBalance);
    }

    double getBalance(String user) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        System.out.println("Balance: " + balance);
        return balance;
    }

    @Tool("Credit the given user with the given amount and return the new balance")
    Double credit(@P("user name") String user, @P("amount") Double amount) {
        System.out.println("Credit " + amount + " to " + user);
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        Double newBalance = balance + amount;
        accounts.put(user, newBalance);
        System.out.println("New balance: " + newBalance);
        return newBalance;
    }

    @Tool("Withdraw the given amount with the given user and return the new balance")
    Double withdraw(@P("user name") String user, @P("amount") Double amount) {
        System.out.println("Withdraw " + amount + " from " + user);
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        Double newBalance = balance - amount;
        accounts.put(user, newBalance);
        System.out.println("New balance: " + newBalance);
        return newBalance;
    }
}