package com.financematch.asset.domain;

import java.time.LocalDateTime;
import java.util.List;

public record MyDataSnapshot(
        List<MyDataAsset> assets,
        List<MyDataLoan> loans,
        MyDataPensionIsa pensionIsa,
        LocalDateTime fetchedAt) {

    public MyDataSnapshot {
        if (assets == null || loans == null) {
            throw new IllegalArgumentException("마이데이터 자산·대출 목록이 필요합니다.");
        }
        assets = List.copyOf(assets);
        loans = List.copyOf(loans);

        if (pensionIsa == null || fetchedAt == null) {
            throw new IllegalArgumentException("마이데이터 스냅샷 정보가 올바르지 않습니다.");
        }
    }
}
