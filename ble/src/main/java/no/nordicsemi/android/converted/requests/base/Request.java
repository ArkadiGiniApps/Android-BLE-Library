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

package no.nordicsemi.android.converted.requests.base;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import no.nordicsemi.android.converted.callback.CallbackHandler;
import no.nordicsemi.android.ble.Handlers.RequestHandler;
import no.nordicsemi.android.converted.requests.timeoutable.valuechange.ConditionalWaitRequest;
import no.nordicsemi.android.converted.requests.timeoutable.ConnectRequest;
import no.nordicsemi.android.converted.requests.timeoutable.DisconnectRequest;
import no.nordicsemi.android.converted.requests.Operation;
import no.nordicsemi.android.converted.requests.queue.ReliableWriteRequest;
import no.nordicsemi.android.converted.requests.simple.SetValueRequest;
import no.nordicsemi.android.converted.requests.simple.SimpleRequest;
import no.nordicsemi.android.converted.requests.timeoutable.valuechange.WaitForReadRequest;
import no.nordicsemi.android.converted.requests.timeoutable.valuechange.WaitForValueChangedRequest;
import WriteRequest;
import no.nordicsemi.android.ble.aaaaaaa.BleClientManager;
import no.nordicsemi.android.converted.callback.request.BeforeCallback;
import no.nordicsemi.android.converted.callback.request.FailCallback;
import no.nordicsemi.android.converted.callback.request.InvalidRequestCallback;
import no.nordicsemi.android.converted.callback.request.SuccessCallback;

/**
 * On Android, when multiple BLE operations needs to be done, it is required to wait for a proper
 * {@link BluetoothGattCallback} callback before calling another operation.
 * In order to make BLE operations easier the BleManager allows to enqueue a request containing all
 * data necessary for a given operation. Requests are performed one after another until the queue
 * is empty.
 */
@SuppressWarnings({"unused", "WeakerAccess", "deprecation", "DeprecatedIsStillUsed"})
public abstract class Request {

	public enum Type {
		SET,
		CONNECT,
		DISCONNECT,
		CREATE_BOND,
		REMOVE_BOND,
		WRITE,
		NOTIFY,
		INDICATE,
		READ,
		WRITE_DESCRIPTOR,
		READ_DESCRIPTOR,
		BEGIN_RELIABLE_WRITE,
		EXECUTE_RELIABLE_WRITE,
		ABORT_RELIABLE_WRITE,
		ENABLE_NOTIFICATIONS,
		ENABLE_INDICATIONS,
		DISABLE_NOTIFICATIONS,
		DISABLE_INDICATIONS,
		WAIT_FOR_NOTIFICATION,
		WAIT_FOR_INDICATION,
		WAIT_FOR_READ,
		WAIT_FOR_WRITE,
		WAIT_FOR_CONDITION,
		SET_VALUE,
		SET_DESCRIPTOR_VALUE,
		ENABLE_SERVICE_CHANGED_INDICATIONS,
		REQUEST_MTU,
		REQUEST_CONNECTION_PRIORITY,
		SET_PREFERRED_PHY,
		READ_PHY,
		READ_RSSI,
		REFRESH_CACHE,
		SLEEP,
	}
	/** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** */
	protected RequestHandler requestHandler;
	public CallbackHandler handler;
	/** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** */
	protected final ConditionVariable syncLock;
	public final Type type;
	public final BluetoothGattCharacteristic characteristic;
	public final BluetoothGattDescriptor descriptor;
	/** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** */
	protected BeforeCallback	beforeCallback;
	protected SuccessCallback 	successCallback;
	protected FailCallback 	  	failCallback;
	/** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** */
	InvalidRequestCallback 	invalidRequestCallback;
	BeforeCallback	internalBeforeCallback;
	SuccessCallback	internalSuccessCallback;
	FailCallback 	internalFailCallback;
	/** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** */
	public boolean enqueued;
	boolean started;
	protected boolean finished;

	/** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** */
	public Request(@NonNull final Type type) {
		this.type = type;
		this.characteristic = null;
		this.descriptor = null;
		this.syncLock = new ConditionVariable(true);
	}
	public Request(@NonNull final Type type, @Nullable final BluetoothGattCharacteristic characteristic) {
		this.type = type;
		this.characteristic = characteristic;
		this.descriptor = null;
		this.syncLock = new ConditionVariable(true);
	}
	public Request(@NonNull final Type type, @Nullable final BluetoothGattDescriptor descriptor) {
		this.type = type;
		this.characteristic = null;
		this.descriptor = descriptor;
		this.syncLock = new ConditionVariable(true);
	}
	/** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** */
	/**
	 * Sets the {@link BleClientManager} instance.
	 *  @param requestHandler the requestHandler in which the request will be executed.
	 *
	 */
	@NonNull
	public Request setRequestHandler(@NonNull final RequestHandler requestHandler) {
		this.requestHandler = requestHandler;
		if (this.handler == null) {
			this.handler = requestHandler;
		}
		return this;
	}
	/**
	 * Sets the handler that will be used to invoke callbacks. By default, the handler set in
	 * {@link BleClientManager} will be used.
	 *
	 * @param handler The handler to invoke callbacks for this request.
	 * @return The request.
	 */
	@NonNull
	public Request setHandler(@NonNull final Handler handler) {
		this.handler = new CallbackHandler() {
			@Override
			public void post(@NonNull final Runnable r) {
				handler.post(r);
			}

			@Override
			public void postDelayed(@NonNull final Runnable r, final long delayMillis) {
				handler.postDelayed(r, delayMillis);
			}

			@Override
			public void removeCallbacks(@NonNull final Runnable r) {
				handler.removeCallbacks(r);
			}
		};
		return this;
	}

	/** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** */
	/**
	 * Creates a new connect request. This allows to set a callback to the connect event,
	 * just like any other request.
	 *
	 * @param device the device to connect to.
	 * @return The new connect request.
	 */
	@NonNull
	public static ConnectRequest connect(@NonNull final BluetoothDevice device) {
		return new ConnectRequest(Type.CONNECT, device);
	}
	/**
	 * Creates a new disconnect request. This allows to set a callback to a disconnect event,
	 * just like any other request.
	 *
	 * @return The new disconnect request.
	 */
	@NonNull
	public static DisconnectRequest disconnect() {
		return new DisconnectRequest(Type.DISCONNECT);
	}
	/**
	 * Creates new Reliable Write request. All operations that need to be executed
	 * reliably should be enqueued inside the returned request before enqueuing it in the
	 * BleManager. The library will automatically verify the data sent
	 *
	 * @return The new request.
	 */
	@NonNull
	public static ReliableWriteRequest newReliableWriteRequest() {
		return new ReliableWriteRequest();
	}
	/**
	 * Creates new Begin Reliable Write request.
	 *
	 * @return The new request.
	 */
	@NonNull
	public static SimpleRequest newBeginReliableWriteRequest() {
		return new SimpleRequest(Type.BEGIN_RELIABLE_WRITE);
	}
	/**
	 * Executes Reliable Write sub-procedure. At lease one Write Request must be performed
	 * before the Reliable Write is to be executed, otherwise error
	 * {@link no.nordicsemi.android.ble.error.GattError#GATT_INVALID_OFFSET} will be returned.
	 *
	 * @return The new request.
	 */
	@NonNull
	public static SimpleRequest newExecuteReliableWriteRequest() {
		return new SimpleRequest(Type.EXECUTE_RELIABLE_WRITE);
	}
	/**
	 * Aborts Reliable Write sub-procedure. All write requests performed during Reliable Write will
	 * be cancelled. At lease one Write Request must be performed before the Reliable Write
	 * is to be executed, otherwise error
	 * {@link no.nordicsemi.android.ble.error.GattError#GATT_INVALID_OFFSET} will be returned.
	 *
	 * @return The new request.
	 */
	@NonNull
	public static SimpleRequest newAbortReliableWriteRequest() {
		return new SimpleRequest(Type.ABORT_RELIABLE_WRITE);
	}
	/**
	 * Creates new Send Notification request. The request will not be executed if given
	 * characteristic is null or does not have NOTIFY property.
	 * After the operation is complete a proper callback will be invoked.
	 *
	 * @param characteristic characteristic to be notified.
	 * @param value          value to be sent. The array is copied into another buffer so it's
	 *                       safe to reuse the array again.
	 * @return The new request.
	 */
	@NonNull
	public static WriteRequest newNotificationRequest(@Nullable final BluetoothGattCharacteristic characteristic, @Nullable final byte[] value) {
		return new WriteRequest(Type.NOTIFY, characteristic, value, 0,
				value != null ? value.length : 0);
	}

	/**
	 * Creates new Send Notification request. The request will not be executed if given
	 * characteristic is null or does not have NOTIFY property.
	 * After the operation is complete a proper callback will be invoked.
	 *
	 * @param characteristic characteristic to be notified.
	 * @param value          value to be sent. The array is copied into another buffer so it's
	 *                       safe to reuse the array again.
	 * @param offset         the offset from which value has to be copied.
	 * @param length         number of bytes to be copied from the value buffer.
	 * @return The new request.
	 */
	@NonNull
	public static WriteRequest newNotificationRequest(@Nullable final BluetoothGattCharacteristic characteristic, @Nullable final byte[] value, @IntRange(from = 0) final int offset, @IntRange(from = 0) final int length) {
		return new WriteRequest(Type.NOTIFY, characteristic, value, offset, length);
	}

	/**
	 * Creates new Send Indication request. The request will not be executed if given
	 * characteristic is null or does not have INDICATE property.
	 * After the operation is complete a proper callback will be invoked.
	 *
	 * @param characteristic characteristic to be indicated.
	 * @param value          value to be sent. The array is copied into another buffer so it's
	 *                       safe to reuse the array again.
	 * @return The new request.
	 */
	@NonNull
	public static WriteRequest newIndicationRequest(@Nullable final BluetoothGattCharacteristic characteristic, @Nullable final byte[] value) {
		return new WriteRequest(Type.INDICATE, characteristic, value, 0,
				value != null ? value.length : 0);
	}

	/**
	 * Creates new Send Indication request. The request will not be executed if given
	 * characteristic is null or does not have INDICATE property.
	 * After the operation is complete a proper callback will be invoked.
	 *
	 * @param characteristic characteristic to be indicated.
	 * @param value          value to be sent. The array is copied into another buffer so it's
	 *                       safe to reuse the array again.
	 * @param offset         the offset from which value has to be copied.
	 * @param length         number of bytes to be copied from the value buffer.
	 * @return The new request.
	 */
	@NonNull
	public static WriteRequest newIndicationRequest(@Nullable final BluetoothGattCharacteristic characteristic, @Nullable final byte[] value, @IntRange(from = 0) final int offset, @IntRange(from = 0) final int length) {
		return new WriteRequest(Type.INDICATE, characteristic, value, offset, length);
	}

	/**
	 * Creates new Wait For Write request. The request will not be executed if given
	 * characteristic is null, or does not have WRITE property.
	 * After the operation is complete a proper callback will be invoked.
	 * <p>
	 * If the write should be triggered by another operation (for example writing an
	 * op code), set it with {@link WaitForValueChangedRequest#trigger(Operation)}.
	 *
	 * @param characteristic characteristic that should be written by the remote device.
	 * @return The new request.
	 */
	@NonNull
	public static WaitForValueChangedRequest newWaitForWriteRequest(@Nullable final BluetoothGattCharacteristic characteristic) {
		return new WaitForValueChangedRequest(Type.WAIT_FOR_WRITE, characteristic);
	}

	/**
	 * Creates new Wait For Write request. The request will not be executed if given
	 * descriptor is null After the operation is complete a proper callback will be invoked.
	 * <p>
	 * If the write should be triggered by another operation (for example writing an
	 * op code), set it with {@link WaitForValueChangedRequest#trigger(Operation)}.
	 *
	 * @param descriptor descriptor that should be written by the remote device.
	 * @return The new request.
	 */
	@NonNull
	public static WaitForValueChangedRequest newWaitForWriteRequest( @Nullable final BluetoothGattDescriptor descriptor) {
		return new WaitForValueChangedRequest(Type.WAIT_FOR_WRITE, descriptor);
	}

	/**
	 * Creates new Wait For Read request. The request will not be executed if given server
	 * characteristic is null, or does not have READ property.
	 * After the operation is complete a proper callback will be invoked.
	 * <p>
	 * If the read should be triggered by another operation (for example writing an
	 * op code), set it with {@link WaitForValueChangedRequest#trigger(Operation)}.
	 *
	 * @param characteristic characteristic that should be read by the remote device.
	 * @return The new request.
	 */
	@NonNull
	public static WaitForReadRequest newWaitForReadRequest(@Nullable final BluetoothGattCharacteristic characteristic) {
		return new WaitForReadRequest(Type.WAIT_FOR_READ, characteristic);
	}

	/**
	 * Creates new Wait For Read request. The request will not be executed if given server
	 * characteristic is null, or does not have READ property.
	 * After the operation is complete a proper callback will be invoked.
	 * <p>
	 * If the read should be triggered by another operation (for example writing an
	 * op code), set it with {@link WaitForValueChangedRequest#trigger(Operation)}.
	 *
	 * @param characteristic characteristic that should be read by the remote device.
	 * @param value          value to be sent. The array is copied into another buffer so it's
	 *                       safe to reuse the array again.
	 * @return The new request.
	 */
	@NonNull
	public static WaitForReadRequest newWaitForReadRequest(@Nullable final BluetoothGattCharacteristic characteristic, @Nullable final byte[] value) {
		return new WaitForReadRequest(Type.WAIT_FOR_READ, characteristic, value, 0,
				value != null ? value.length : 0);
	}

	/**
	 * Creates new Wait For Read request. The request will not be executed if given server
	 * characteristic is null, or does not have READ property.
	 * After the operation is complete a proper callback will be invoked.
	 * <p>
	 * If the read should be triggered by another operation (for example writing an
	 * op code), set it with {@link WaitForValueChangedRequest#trigger(Operation)}.
	 *
	 * @param characteristic characteristic that should be read by the remote device.
	 * @param value          value to be sent. The array is copied into another buffer so it's
	 *                       safe to reuse the array again.
	 * @param offset         the offset from which value has to be copied.
	 * @param length         number of bytes to be copied from the value buffer.
	 * @return The new request.
	 */
	@NonNull
	public static WaitForReadRequest newWaitForReadRequest(@Nullable final BluetoothGattCharacteristic characteristic, @Nullable final byte[] value, @IntRange(from = 0) final int offset, @IntRange(from = 0) final int length) {
		return new WaitForReadRequest(Type.WAIT_FOR_READ, characteristic, value, offset, length);
	}

	/**
	 * Creates new Wait For Read request. The request will not be executed if given server
	 * characteristic is null, or does not have READ property.
	 * After the operation is complete a proper callback will be invoked.
	 * <p>
	 * If the read should be triggered by another operation (for example writing an
	 * op code), set it with {@link WaitForValueChangedRequest#trigger(Operation)}.
	 *
	 * @param descriptor descriptor that should be read by the remote device.
	 * @return The new request.
	 */
	@NonNull
	public static WaitForReadRequest newWaitForReadRequest(@Nullable final BluetoothGattDescriptor descriptor) {
		return new WaitForReadRequest(Type.WAIT_FOR_READ, descriptor);
	}

	/**
	 * Creates new Wait For Read request. The request will not be executed if given server
	 * characteristic is null, or does not have READ property.
	 * After the operation is complete a proper callback will be invoked.
	 * <p>
	 * If the read should be triggered by another operation (for example writing an
	 * op code), set it with {@link WaitForValueChangedRequest#trigger(Operation)}.
	 *
	 * @param descriptor descriptor that should be read by the remote device.
	 * @param value      value to be sent. The array is copied into another buffer so it's
	 *                   safe to reuse the array again.
	 * @return The new request.
	 */
	@NonNull
	public static WaitForReadRequest newWaitForReadRequest(@Nullable final BluetoothGattDescriptor descriptor, @Nullable final byte[] value) {
		return new WaitForReadRequest(Type.WAIT_FOR_READ, descriptor, value, 0,
				value != null ? value.length : 0);
	}

	/**
	 * Creates new Wait For Read request. The request will not be executed if given server
	 * characteristic is null, or does not have READ property.
	 * After the operation is complete a proper callback will be invoked.
	 * <p>
	 * If the read should be triggered by another operation (for example writing an
	 * op code), set it with {@link WaitForValueChangedRequest#trigger(Operation)}.
	 *
	 * @param descriptor descriptor that should be read by the remote device.
	 * @param value      value to be sent. The array is copied into another buffer so it's
	 *                   safe to reuse the array again.
	 * @param offset     the offset from which value has to be copied.
	 * @param length     number of bytes to be copied from the value buffer.
	 * @return The new request.
	 */
	@NonNull
	public static WaitForReadRequest newWaitForReadRequest(@Nullable final BluetoothGattDescriptor descriptor, @Nullable final byte[] value, @IntRange(from = 0) final int offset, @IntRange(from = 0) final int length) {
		return new WaitForReadRequest(Type.WAIT_FOR_READ, descriptor, value, offset, length);
	}

	/**
	 * Creates new Conditional Wait Request. The request will wait until the condition is fulfilled.
	 *
	 * @param condition the condition to check.
	 * @param parameter an optional parameter.
	 * @return The new request.
	 */
	@NonNull
	public static <T> ConditionalWaitRequest<T> newConditionalWaitRequest(@NonNull final ConditionalWaitRequest.Condition<T> condition, @Nullable final T parameter) {
		return new ConditionalWaitRequest<>(Type.WAIT_FOR_CONDITION, condition, parameter);
	}

	/**
	 * Creates new Set Characteristic Value request.
	 *
	 * @param characteristic the target characteristic for value set.
	 * @param value          the new data to be assigned.
	 * @return The new request.
	 */
	@NonNull
	public static SetValueRequest newSetValueRequest(@Nullable final BluetoothGattCharacteristic characteristic, @Nullable final byte[] value) {
		return new SetValueRequest(Type.SET_VALUE, characteristic, value, 0,
				value != null ? value.length : 0);
	}

	/**
	 * Creates new Set Characteristic Value request.
	 *
	 * @param characteristic the target characteristic for value set.
	 * @param value          the new data to be assigned.
	 * @param offset         the offset from which value has to be copied.
	 * @param length         number of bytes to be copied from the value buffer.
	 * @return The new request.
	 */
	@NonNull
	public static SetValueRequest newSetValueRequest(@Nullable final BluetoothGattCharacteristic characteristic, @Nullable final byte[] value, @IntRange(from = 0) final int offset, @IntRange(from = 0) final int length) {
		return new SetValueRequest(Type.SET_VALUE, characteristic, value, offset, length);
	}

	/**
	 * Creates new Set Descriptor Value request.
	 *
	 * @param descriptor the target descriptor for value set.
	 * @param value      the new data to be assigned.
	 * @return The new request.
	 */
	@NonNull
	public static SetValueRequest newSetValueRequest(@Nullable final BluetoothGattDescriptor descriptor, @Nullable final byte[] value) {
		return new SetValueRequest(Type.SET_DESCRIPTOR_VALUE, descriptor, value, 0,
				value != null ? value.length : 0);
	}

	/**
	 * Creates new Set Descriptor Value request.
	 *
	 * @param descriptor the target descriptor for value set.
	 * @param value      the new data to be assigned.
	 * @param offset     the offset from which value has to be copied.
	 * @param length     number of bytes to be copied from the value buffer.
	 * @return The new request.
	 */
	@NonNull
	public static SetValueRequest newSetValueRequest(@Nullable final BluetoothGattDescriptor descriptor, @Nullable final byte[] value, @IntRange(from = 0) final int offset, @IntRange(from = 0) final int length) {
		return new SetValueRequest(Type.SET_DESCRIPTOR_VALUE, descriptor, value, offset, length);
	}

	/**
	 * Creates new Enable Indications on Service Changed characteristic. It is a NOOP if such
	 * characteristic does not exist in the Generic Attribute service.
	 * It is required to enable those notifications on bonded devices on older Android versions to
	 * be informed about attributes changes.
	 * Android 7+ (or 6+) handles this automatically and no action is required.
	 *
	 * @return The new request.
	 */
	@NonNull
	public static WriteRequest newEnableServiceChangedIndicationsRequest() {
		return new WriteRequest(Type.ENABLE_SERVICE_CHANGED_INDICATIONS);
	}
	/** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** **** */

	/**
	 * Use to set a completion callback. The callback will be invoked when the operation has
	 * finished successfully unless the request was executed synchronously, in which case this
	 * callback will be ignored.
	 *
	 * @param callback the callback.
	 * @return The request.
	 */
	@NonNull
	public Request done(@NonNull final SuccessCallback callback) {
		this.successCallback = callback;
		return this;
	}

	/**
	 * Use to set a callback that will be called in case the request has failed.
	 * If the target device wasn't set before executing this request
	 * ({@link BleClientManager#connect(BluetoothDevice)} was never called), the
	 * {@link #invalid(InvalidRequestCallback)} will be used instead, as the
	 * {@link BluetoothDevice} is not known.
	 * <p>
	 * This callback will be ignored if request was executed synchronously, in which case
	 * the error will be returned as an exception.
	 *
	 * @param callback the callback.
	 * @return The request.
	 */
	@NonNull
	public Request fail(@NonNull final FailCallback callback) {
		this.failCallback = callback;
		return this;
	}

	/**
	 * Used to set internal callback what will be executed before the request is executed.
	 *
	 * @param callback the callback.
	 */
	public void internalBefore(@NonNull final BeforeCallback callback) {
		this.internalBeforeCallback = callback;
	}

	/**
	 * Used to set internal success callback. The callback will be notified in case the request
	 * has completed.
	 *
	 * @param callback the callback.
	 */
	public void internalSuccess(@NonNull final SuccessCallback callback) {
		this.internalSuccessCallback = callback;
	}


	/**
	 * Used to set internal fail callback. The callback will be notified in case the request
	 * has failed.
	 *
	 * @param callback the callback.
	 */
	public void internalFail(@NonNull final FailCallback callback) {
		this.internalFailCallback = callback;
	}

	/**
	 * Use to set a callback that will be called in case the request was invalid, for example
	 * called before the device was connected.
	 * This callback will be ignored if request was executed synchronously, in which case
	 * the error will be returned as an exception.
	 *
	 * @param callback the callback.
	 * @return The request.
	 */
	@NonNull
	public Request invalid(@NonNull final InvalidRequestCallback callback) {
		this.invalidRequestCallback = callback;
		return this;
	}

	/**
	 * Sets a callback that will be executed before the execution of this operation starts.
	 *
	 * @param callback the callback.
	 * @return The request.
	 */
	@NonNull
	public Request before(@NonNull final BeforeCallback callback) {
		this.beforeCallback = callback;
		return this;
	}

	/**
	 * Enqueues the request for asynchronous execution.
	 */
	public void enqueue() {
		requestHandler.enqueue(this);
	}

	public void	   notifyStarted(@NonNull final BluetoothDevice device) {
		if (!started) {
			started = true;

			if (internalBeforeCallback != null)
				internalBeforeCallback.onRequestStarted(device);
			handler.post(() -> {
				if (beforeCallback != null)
					beforeCallback.onRequestStarted(device);
			});
		}
	}
	public boolean notifySuccess(@NonNull final BluetoothDevice device) {
		if (!finished) {
			finished = true;

			if (internalSuccessCallback != null)
				internalSuccessCallback.onRequestCompleted(device);

			handler.post(() -> {
				if (successCallback != null)
					successCallback.onRequestCompleted(device);
			});
			return true;
		}
		return false;
	}
	public void    notifyFail(@NonNull final BluetoothDevice device, final int status) {
		if (!finished) {
			finished = true;

			if (internalFailCallback != null)
				internalFailCallback.onRequestFailed(device, status);
			handler.post(() -> {
				if (failCallback != null)
					failCallback.onRequestFailed(device, status);
			});
		}
	}
	public void    notifyInvalidRequest() {
		if (!finished) {
			finished = true;

			handler.post(() -> {
				if (invalidRequestCallback != null)
					invalidRequestCallback.onInvalidRequest();
			});
		}
	}

	/**
	 * Asserts that the synchronous method was not called from the UI thread.
	 *
	 * @throws IllegalStateException when called from a UI thread.
	 */
	public static void assertNotMainThread() throws IllegalStateException {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			throw new IllegalStateException("Cannot execute synchronous operation from the UI thread.");
		}
	}

	public final class RequestCallback implements SuccessCallback, FailCallback, InvalidRequestCallback {
		public final static int REASON_REQUEST_INVALID = -1000000;
		public int status = BluetoothGatt.GATT_SUCCESS;

		@Override public void onRequestCompleted(@NonNull final BluetoothDevice device) {
			syncLock.open();
		}
		@Override public void onRequestFailed(@NonNull final BluetoothDevice device, final int status) {
			this.status = status;
			syncLock.open();
		}
		@Override public void onInvalidRequest() {
			this.status = REASON_REQUEST_INVALID;
			syncLock.open();
		}

		@SuppressWarnings("BooleanMethodIsAlwaysInverted")
		public boolean isSuccess() {
			return this.status == BluetoothGatt.GATT_SUCCESS;
		}
	}
}
