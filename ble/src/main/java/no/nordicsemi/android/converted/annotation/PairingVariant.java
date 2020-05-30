package no.nordicsemi.android.converted.annotation;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import no.nordicsemi.android.ble.aaaaaaa.BleClientManager;

@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
		BleClientManager.PAIRING_VARIANT_PIN,
		BleClientManager.PAIRING_VARIANT_PASSKEY,
		BleClientManager.PAIRING_VARIANT_PASSKEY_CONFIRMATION,
		BleClientManager.PAIRING_VARIANT_CONSENT,
		BleClientManager.PAIRING_VARIANT_DISPLAY_PASSKEY,
		BleClientManager.PAIRING_VARIANT_DISPLAY_PIN,
		BleClientManager.PAIRING_VARIANT_OOB_CONSENT
})
public @interface PairingVariant {}