package com.miduo.cloud.frontend.config;

import static com.miduo.cloud.frontend.util.ProductionLineLicenseFileDecryptor.generatePublicKeyFromPrivate;

public class ProductionLineLicenseCryptoProperties {

    public String getAesKeyBase64() {
        return "Li43uFlTdZ0MYrMCyKhIKX8sJEkBnPxtMp0+Gg0bdxQ=";
    }


    public String getRsaPublicKeyBase64() {
        return "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsHtDrK6OM4laFmXK/45P\n" +
                "XtLaUyJYtgS8XuPHgfaJisi0JvquxG6kOKuub/LW90ARqtoG1pR6GSMy4eEvww/p\n" +
                "WnS5KS/LpSE4aVyq3em72tawexdctzJJmlMnQI8ZyRvXrXZfCffepd+5/E7xXS9J\n" +
                "GtesiS8Jq1ZcrJEyLO9kt14lk1jYRKx2rSg4p1Nnd9PcTjgPDq/pk29gaLxZeakl\n" +
                "istvI6jTujtRmEeuNCmkyoutHzPJv5kBEjDO2Kqc+fS+ruKtgoCRF+AcVZaRFzdH\n" +
                "A5wR5beQ/SWkqw6gsVH6C0PXHlZmpw5H+mTDaIdy3EFlaUZO48SbOkkLbh0tDDHU\n" +
                "hwIDAQAB\n" +
                "-----END PUBLIC KEY-----";
    }
}
