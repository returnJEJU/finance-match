package com.financematch.recommendation.service;

import com.financematch.recommendation.type.RecommendationReasonCode;
import org.springframework.stereotype.Component;

@Component
public class RecommendationReasonResolver {

    public String resolve(RecommendationReasonCode reasonCode) {
        if (reasonCode == null) {
            return null;
        }

        return switch (reasonCode) {
            case DEPOSIT_TERM_AND_RATE ->
                    "두 분의 목표 기간에 맞고 기본금리가 높은 예금 상품이에요.";
            case SAVINGS_KB_STAR_PRIORITY ->
                    "KB스타적금 가입 대상이며 목표 기간과 월 저축액에도 맞아요.";
            case SAVINGS_TERM_AND_RATE ->
                    "목표 기간과 월 저축액에 맞고 기본금리가 높은 적금 상품이에요.";
            case INVESTMENT_RETIREMENT_TDF_PRIORITY ->
                    "노후 목표와 투자성향에 맞는 TDF 상품을 우선 골랐어요.";
            case INVESTMENT_RISK_AND_AUM ->
                    "투자성향에 맞는 위험등급 중 순자산이 큰 상품을 골랐어요.";
            case LOAN_TARGET_GROUP_RATE_AND_CHANNEL ->
                    "대출 목적과 조건에 맞고 금리와 신청 편의성이 좋은 상품이에요.";
            case TAX_SAVING_ISA_KNOWLEDGE ->
                    "계좌 현황과 금융 이해도에 잘 맞는 ISA 상품을 골랐어요.";
            case TAX_SAVING_PENSION_RETIREMENT ->
                    "노후 목표와 계좌 현황에 잘 맞는 연금저축 상품을 골랐어요.";
            case TAX_SAVING_IRP_INVESTMENT_TYPE ->
                    "노후 목표와 투자성향에 잘 맞는 IRP 상품을 골랐어요.";
        };
    }
}