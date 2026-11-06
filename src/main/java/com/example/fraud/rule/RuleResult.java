package com.example.fraud.rule;

import com.example.fraud.entity.Severity;

/**
 * What a rule returns when it decides a transaction looks suspicious.
 *
 * @param ruleCode  stable identifier of the rule, e.g. HIGH_AMOUNT
 * @param severity  how serious it is
 * @param riskScore 0-100
 * @param reason    human-readable explanation shown to the analyst
 */
public record RuleResult(String ruleCode, Severity severity, int riskScore, String reason) {
}
