/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.converted.requests.simple.simplevalue.results;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.Handlers.RequestHandler;
import no.nordicsemi.android.converted.requests.simple.simplevalue.SimpleValueRequest;
import no.nordicsemi.android.converted.callback.request.BeforeCallback;
import no.nordicsemi.android.converted.callback.request.FailCallback;
import no.nordicsemi.android.converted.callback.request.InvalidRequestCallback;
import no.nordicsemi.android.converted.callback.request.SuccessCallback;
import no.nordicsemi.android.converted.callback.valueproperty.RssiCallback;
import no.nordicsemi.android.converted.requests.Operation;

public final class ReadRssiRequest extends SimpleValueRequest<RssiCallback> implements Operation {

	/** **************************************************************************************** **/
	public ReadRssiRequest(@NonNull final Type type) {
		super(type);
	}
	/** **************************************************************************************** **/
	@NonNull @Override public ReadRssiRequest setRequestHandler(@NonNull final RequestHandler requestHandler) {
		super.setRequestHandler(requestHandler);
		return this;
	}
	@NonNull @Override public ReadRssiRequest setHandler(@NonNull final Handler handler) {
		super.setHandler(handler);
		return this;
	}
	/** **************************************************************************************** **/
	@NonNull @Override public ReadRssiRequest before(@NonNull final BeforeCallback callback) {
		super.before(callback);
		return this;
	}
	@NonNull @Override public ReadRssiRequest done(@NonNull final SuccessCallback callback) {
		super.done(callback);
		return this;
	}
	@NonNull @Override public ReadRssiRequest fail(@NonNull final FailCallback callback) {
		super.fail(callback);
		return this;
	}
	@NonNull @Override public ReadRssiRequest invalid(@NonNull final InvalidRequestCallback callback) {
		super.invalid(callback);
		return this;
	}
	@NonNull @Override public ReadRssiRequest with(@NonNull final RssiCallback callback) {
		super.with(callback);
		return this;
	}

	public void notifyRssiRead(@NonNull final BluetoothDevice device,
                               @IntRange(from = -128, to = 20) final int rssi) {
		handler.post(() -> {
			if (valueCallback != null)
				valueCallback.onRssiRead(device, rssi);
		});
	}
}
