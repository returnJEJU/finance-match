package com.financematch.asset.service;

import com.financematch.asset.domain.MyDataSnapshot;

/**
 * 외부 마이데이터의 경계.
 *
 * <p>현재는 고정 목데이터 구현을 사용하고, 목 서버가 준비되면 HTTP 호출 구현으로 교체한다.
 */
public interface MyDataProvider {

    MyDataSnapshot fetch(Long memberId);
}
