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
import no.nordicsemi.android.converted.callback.valueproperty.MtuCallback;
import no.nordicsemi.android.converted.requests.Operation;

public final class MtuRequest extends SimpleValueRequest<MtuCallback> implements Operation {

	private final int value;

	public MtuRequest(@NonNull final Type type, @IntRange(from = 23, to = 517) int mtu) {
		super(type);
		if (mtu < 23)
			mtu = 23;
		if (mtu > 517)
			mtu = 517;
		this.value = mtu;
	}
	/** **************************************************************************************** **/
	@NonNull @Override public MtuRequest setRequestHandler(@NonNull final RequestHandler requestHandler) {
		super.setRequestHandler(requestHandler);
		return this;
	}
	@NonNull @Override public MtuRequest setHandler(@NonNull final Handler handler) {
		super.setHandler(handler);
		return this;
	}
	/** **************************************************************************************** **/
	@NonNull @Override public MtuRequest before(@NonNull final BeforeCallback callback) {
		super.before(callback);
		return this;
	}
	@NonNull @Override public MtuRequest done(@NonNull final SuccessCallback callback) {
		super.done(callback);
		return this;
	}
	@NonNull @Override public MtuRequest fail(@NonNull final FailCallback callback) {
		super.fail(callback);
		return this;
	}
	@NonNull @Override public MtuRequest invalid(@NonNull final InvalidRequestCallback callback) {
		super.invalid(callback);
		return this;
	}
	@NonNull @Override public MtuRequest with(@NonNull final MtuCallback callback) {
		super.with(callback);
		return this;
	}
	/** **************************************************************************************** **/
	public void notifyMtuChanged(@NonNull final BluetoothDevice device, @IntRange(from = 23, to = 517) final int mtu) {
		handler.post(() -> {
			if (valueCallback != null)
				valueCallback.onMtuChanged(device, mtu);
		});
	}
	public int getRequiredMtu() {
		return value;
	}
}
