package com.financematch.asset.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.asset.dto.AssetAccountResponse;
import com.financematch.asset.dto.AssetLinkResponse;
import com.financematch.asset.service.AssetService;
import com.financematch.common.ApiResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    @Mock
    private AssetService assetService;

    @InjectMocks
    private AssetController assetController;

    @Test
    void initialLinkReturnsCreatedAndAssetLocation() {
        AssetLinkResponse linked = response();
        when(assetService.link(1L)).thenReturn(linked);

        ResponseEntity<ApiResponse<AssetLinkResponse>> response =
                assetController.link(1L);

        verify(assetService).link(1L);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("/api/v1/members/me/assets", response.getHeaders().getLocation().toString());
        assertTrue(response.getBody().isSuccess());
        assertSame(linked, response.getBody().getData());
    }

    @Test
    void getAssetsReturnsLinkedAssetData() {
        AssetLinkResponse linked = response();
        when(assetService.getAssets(1L)).thenReturn(linked);

        ApiResponse<AssetLinkResponse> response =
                assetController.getAssets(1L);

        verify(assetService).getAssets(1L);
        assertTrue(response.isSuccess());
        assertSame(linked, response.getData());
    }

    @Test
    void refreshReturnsUpdatedAssetData() {
        AssetLinkResponse refreshed = response();
        when(assetService.refresh(1L)).thenReturn(refreshed);

        ApiResponse<AssetLinkResponse> response =
                assetController.refresh(1L);

        verify(assetService).refresh(1L);
        assertTrue(response.isSuccess());
        assertSame(refreshed, response.getData());
    }

    private AssetLinkResponse response() {
        return new AssetLinkResponse(
                new BigDecimal("84200000"),
                BigDecimal.ZERO,
                5,
                List.of(),
                new AssetAccountResponse(false, true, true),
                LocalDateTime.of(2026, 7, 31, 12, 0));
    }
}
