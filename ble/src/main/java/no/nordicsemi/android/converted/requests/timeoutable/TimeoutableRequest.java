package no.nordicsemi.android.converted.requests.timeoutable;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import no.nordicsemi.android.ble.Handlers.RequestHandler;
import no.nordicsemi.android.converted.callback.request.FailCallback;
import no.nordicsemi.android.converted.callback.request.SuccessCallback;
import no.nordicsemi.android.converted.exception.BluetoothDisabledException;
import no.nordicsemi.android.converted.exception.DeviceDisconnectedException;
import no.nordicsemi.android.converted.exception.InvalidRequestException;
import no.nordicsemi.android.converted.exception.RequestFailedException;
import no.nordicsemi.android.converted.requests.Operation;
import no.nordicsemi.android.converted.requests.base.Request;
import no.nordicsemi.android.converted.requests.timeoutable.valuechange.TimeoutableValueRequest;

public abstract class TimeoutableRequest extends Request {
	private Runnable timeoutCallback;
	protected long timeout;

	public TimeoutableRequest(@NonNull final Type type) {
		super(type);
	}

	public TimeoutableRequest(@NonNull final Type type, @Nullable final BluetoothGattCharacteristic characteristic) {
		super(type, characteristic);
	}

	public TimeoutableRequest(@NonNull final Type type, @Nullable final BluetoothGattDescriptor descriptor) {
		super(type, descriptor);
	}

	/** *********************************** internal methods *********************************** **/
	@NonNull
	@Override
	public TimeoutableRequest setRequestHandler(@NonNull final RequestHandler requestHandler) {
		super.setRequestHandler(requestHandler);
		return this;
	}
	@NonNull
	@Override
	public TimeoutableRequest setHandler(@NonNull final Handler handler) {
		super.setHandler(handler);
		return this;
	}

	/** ************************************ enqueue methods *********************************** **/
	/**
	 * Sets the operation timeout.
	 * When the timeout occurs, the request will fail with {@link FailCallback#REASON_TIMEOUT}.
	 *
	 * @param timeout the request timeout in milliseconds, 0 to disable timeout.
	 * @return the callback.
	 * @throws IllegalStateException         thrown when the request has already been started.
	 * @throws UnsupportedOperationException thrown when the timeout is not allowed for this request,
	 *                                       as the callback from the system is required.
	 */
	@NonNull
	public TimeoutableRequest timeout(@IntRange(from = 0) final long timeout) {
		if (timeoutCallback != null)
			throw new IllegalStateException("Request already started");
		this.timeout = timeout;
		return this;
	}

	/**
	 * Enqueues the request for asynchronous execution.
	 * <p>
	 * Use {@link #timeout(long)} to set the maximum time the manager should wait until the device
	 * is ready. When the timeout occurs, the request will fail with
	 * {@link FailCallback#REASON_TIMEOUT} and the device will get disconnected.
	 */
	@Override
	public final void enqueue() {
		super.enqueue();
	}

	/**
	 * Synchronously waits until the request is done.
	 * <p>
	 * Use {@link #timeout(long)} to set the maximum time the manager should wait until the request
	 * is ready. When the timeout occurs, the {@link InterruptedException} will be thrown.
	 * <p>
	 * Callbacks set using {@link #done(SuccessCallback)} and {@link #fail(FailCallback)}
	 * will be ignored.
	 * <p>
	 * This method may not be called from the main (UI) thread.
	 *
	 * @throws RequestFailedException      thrown when the BLE request finished with status other
	 *                                     than {@link BluetoothGatt#GATT_SUCCESS}.
	 * @throws InterruptedException        thrown if the timeout occurred before the request has
	 *                                     finished.
	 * @throws IllegalStateException       thrown when you try to call this method from the main
	 *                                     (UI) thread.
	 * @throws DeviceDisconnectedException thrown when the device disconnected before the request
	 *                                     was completed.
	 * @throws BluetoothDisabledException  thrown when the Bluetooth adapter has been disabled.
	 * @throws InvalidRequestException     thrown when the request was called before the device was
	 *                                     connected at least once (unknown device).
	 * @see #enqueue()
	 */
	public final void await() throws RequestFailedException, DeviceDisconnectedException,
			BluetoothDisabledException, InvalidRequestException, InterruptedException {
		assertNotMainThread();

		final SuccessCallback sc = successCallback;
		final FailCallback fc = failCallback;
		try {
			syncLock.close();
			final RequestCallback callback = new RequestCallback();
			done(callback).fail(callback).invalid(callback).enqueue();

			if (!syncLock.block(timeout)) {
				throw new InterruptedException();
			}
			if (!callback.isSuccess()) {
				if (callback.status == FailCallback.REASON_DEVICE_DISCONNECTED) {
					throw new DeviceDisconnectedException();
				}
				if (callback.status == FailCallback.REASON_BLUETOOTH_DISABLED) {
					throw new BluetoothDisabledException();
				}
				if (callback.status == RequestCallback.REASON_REQUEST_INVALID) {
					throw new InvalidRequestException(this);
				}
				throw new RequestFailedException(this, callback.status);
			}
		} finally {
			successCallback = sc;
			failCallback = fc;
		}
	}
	/** ************************************ notify methods ************************************ **/
	@Override public void 	 notifyStarted(@NonNull final BluetoothDevice device) {
		if (timeout > 0L) {
			timeoutCallback = () -> {
				timeoutCallback = null;
				if (!finished) {
					notifyFail(device, FailCallback.REASON_TIMEOUT);
					requestHandler.onRequestTimeout(this);
				}
			};
			handler.postDelayed(timeoutCallback, timeout);
		}
		super.notifyStarted(device);
	}
	@Override public boolean notifySuccess(@NonNull final BluetoothDevice device) {
		if (!finished) {
			handler.removeCallbacks(timeoutCallback);
			timeoutCallback = null;
		}
		return super.notifySuccess(device);
	}
	@Override public void 	 notifyFail(@NonNull final BluetoothDevice device, final int status) {
		if (!finished) {
			handler.removeCallbacks(timeoutCallback);
			timeoutCallback = null;
		}
		super.notifyFail(device, status);
	}
	@Override public void 	 notifyInvalidRequest() {
		if (!finished) {
			handler.removeCallbacks(timeoutCallback);
			timeoutCallback = null;
		}
		super.notifyInvalidRequest();
	}
	/** ************************************* inner class ************************************** **/
	public abstract static class AwaitingRequest<T> extends TimeoutableValueRequest<T> {

		private static final int NOT_STARTED = -123456;
		private static final int STARTED = NOT_STARTED + 1;

		private Request trigger;
		private int triggerStatus = BluetoothGatt.GATT_SUCCESS;

		public AwaitingRequest(@NonNull final Type type) {
			super(type);
		}

		public AwaitingRequest(@NonNull final Type type, @Nullable final BluetoothGattCharacteristic characteristic) {
			super(type, characteristic);
		}

		public AwaitingRequest(@NonNull final Type type, @Nullable final BluetoothGattDescriptor descriptor) {
			super(type, descriptor);
		}

		/**
		 * Sets an optional request that is suppose to trigger the notification or indication.
		 * This is to ensure that the characteristic value won't change before the callback was set.
		 *
		 * @param trigger the operation that triggers the notification, usually a write characteristic
		 *                request that write some OP CODE.
		 * @return The request.
		 */
		@NonNull
		public AwaitingRequest trigger(@NonNull final Operation trigger) {
			if (trigger instanceof Request) {
				this.trigger = (Request) trigger;
				this.triggerStatus = NOT_STARTED;
				// The trigger will never receive invalid request event.
				// If the BluetoothDevice wasn't set, the whole WaitForValueChangedRequest would be invalid.
				/*this.trigger.invalid(() -> {
					// never called
				});*/
				this.trigger.internalBefore(device -> triggerStatus = STARTED);
				this.trigger.internalSuccess(device -> triggerStatus = BluetoothGatt.GATT_SUCCESS);
				this.trigger.internalFail((device, status) -> {
					triggerStatus = status;
					syncLock.open();
					notifyFail(device, status);
				});
			}
			return this;
		}

		@NonNull
		@Override
		public <E extends T> E await(@NonNull final E response)
				throws RequestFailedException, DeviceDisconnectedException, BluetoothDisabledException,
				InvalidRequestException, InterruptedException {
			assertNotMainThread();

			try {
				// Ensure the trigger request it enqueued after the callback has been set.
				if (trigger != null && trigger.enqueued) {
					throw new IllegalStateException("Trigger request already enqueued");
				}
				super.await(response);
				return response;
			} catch (final RequestFailedException e) {
				if (triggerStatus != BluetoothGatt.GATT_SUCCESS) {
					// Trigger will never have invalid request status. The outer request will.
					/*if (triggerStatus == RequestCallback.REASON_REQUEST_INVALID) {
						throw new InvalidRequestException(trigger);
					}*/
					throw new RequestFailedException(trigger, triggerStatus);
				}
				throw e;
			}
		}

		@Nullable
		public Request getTrigger() {
			return trigger;
		}

		@SuppressWarnings("BooleanMethodIsAlwaysInverted")
		public boolean isTriggerPending() {
			return triggerStatus == NOT_STARTED;
		}

		public boolean isTriggerCompleteOrNull() {
			return triggerStatus != STARTED;
		}
	}
}
