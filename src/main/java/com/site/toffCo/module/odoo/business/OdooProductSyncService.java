package com.site.toffCo.module.odoo.business;

import com.site.toffCo.module.odoo.client.OdooProductClient;
import com.site.toffCo.module.odoo.dto.OdooProductRequestDTO;
import com.site.toffCo.module.odoo.dto.OdooProductSyncEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OdooProductSyncService {

    private final OdooProductClient odooProductClient;

    public void sync(OdooProductSyncEventDTO event) {
        validate(event);

        OdooProductRequestDTO request =
                new OdooProductRequestDTO(
                        event.name(),
                        event.description(),
                        event.barcode(),
                        event.price(),
                        event.stock()
                );

        odooProductClient.createProduct(request);
    }

    private void validate(OdooProductSyncEventDTO event) {
        if (event.productId() == null) {
            throw new IllegalArgumentException(
                    "ID do produto não informado"
            );
        }

        if (event.name() == null || event.name().isBlank()) {
            throw new IllegalArgumentException(
                    "Nome do produto não informado"
            );
        }

        if (event.barcode() == null || event.barcode().isBlank()) {
            throw new IllegalArgumentException(
                    "Código de barras não informado"
            );
        }
    }
}