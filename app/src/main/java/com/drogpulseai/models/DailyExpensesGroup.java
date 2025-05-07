package com.drogpulseai.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DailyExpensesGroup {
    private String dateStr;
    private Date date;
    private List<Expense> expenses;
    private double totalAmount;

    public DailyExpensesGroup(String dateStr, Date date) {
        this.dateStr = dateStr;
        this.date = date;
        this.expenses = new ArrayList<>();
        this.totalAmount = 0.0;
    }

    public String getDateStr() {
        return dateStr;
    }

    public Date getDate() {
        return date;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
        totalAmount += expense.getAmount();
    }

    public int getExpenseCount() {
        return expenses.size();
    }
}