package no.nordicsemi.android.converted.requests.timeoutable.valuechange;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import no.nordicsemi.android.ble.Handlers.RequestHandler;
import no.nordicsemi.android.converted.requests.Operation;
import no.nordicsemi.android.converted.requests.timeoutable.TimeoutableRequest;
import no.nordicsemi.android.converted.callback.request.BeforeCallback;
import no.nordicsemi.android.converted.callback.request.FailCallback;
import no.nordicsemi.android.converted.callback.request.InvalidRequestCallback;
import no.nordicsemi.android.converted.callback.request.SuccessCallback;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class ConditionalWaitRequest<T> extends TimeoutableRequest.AwaitingRequest implements Operation {

	/**
	 * The condition object.
	 */
	public interface Condition<T> {
		boolean predicate(@Nullable final T parameter);
	}

	@NonNull
	private final Condition<T> condition;
	@Nullable
	private final T parameter;
	/** Expected value of the condition to stop waiting. */
	private boolean expected = false;

	public ConditionalWaitRequest(@NonNull final Type type, @NonNull final Condition<T> condition,
								  @Nullable final T parameter) {
		super(type);
		this.condition = condition;
		this.parameter = parameter;
	}

	@NonNull
	@Override
    public ConditionalWaitRequest<T> setRequestHandler(@NonNull final RequestHandler requestHandler) {
		super.setRequestHandler(requestHandler);
		return this;
	}

	@NonNull
	@Override
	public ConditionalWaitRequest<T> setHandler(@NonNull final Handler handler) {
		super.setHandler(handler);
		return this;
	}

	@Override
	@NonNull
	public ConditionalWaitRequest<T> done(@NonNull final SuccessCallback callback) {
		super.done(callback);
		return this;
	}

	@Override
	@NonNull
	public ConditionalWaitRequest<T> fail(@NonNull final FailCallback callback) {
		super.fail(callback);
		return this;
	}

	@NonNull
	@Override
	public ConditionalWaitRequest<T> invalid(@NonNull final InvalidRequestCallback callback) {
		super.invalid(callback);
		return this;
	}

	@Override
	@NonNull
	public ConditionalWaitRequest<T> before(@NonNull final BeforeCallback callback) {
		super.before(callback);
		return this;
	}

	/**
	 * Negates the expected value of the predicate.
	 *
	 * @return The request.
	 */
	@NonNull
	public ConditionalWaitRequest<T> negate() {
		expected = true;
		return this;
	}

	public boolean isFulfilled() {
		try {
			return condition.predicate(parameter) == expected;
		} catch (final Exception e) {
			Log.e("ConditionalWaitRequest", "Error while checking predicate", e);
			return true;
		}
	}
}
