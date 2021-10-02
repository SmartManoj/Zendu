package it.tdlight.common.internal;

import it.tdlight.common.ClientEventsHandler;
import it.tdlight.common.ExceptionHandler;
import it.tdlight.common.ResultHandler;
import it.tdlight.common.TelegramClient;
import it.tdlight.common.UpdatesHandler;
import it.tdlight.jni.TdApi;
import it.tdlight.jni.TdApi.Error;
import it.tdlight.jni.TdApi.Function;
import it.tdlight.jni.TdApi.Object;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class InternalClient implements ClientEventsHandler, TelegramClient {

	private static final Marker TG_MARKER = MarkerFactory.getMarker("TG");
	private static final Logger logger = LoggerFactory.getLogger(TelegramClient.class);
	private final ConcurrentHashMap<Long, Handler> handlers = new ConcurrentHashMap<Long, Handler>();

	private volatile Integer clientId = null;
	private final InternalClientManager clientManager;
	private Handler updateHandler;
	private MultiHandler updatesHandler;
	private ExceptionHandler defaultExceptionHandler;

	private final AtomicBoolean isClosed = new AtomicBoolean();

	public InternalClient(InternalClientManager clientManager) {
		this.clientManager = clientManager;
	}

	@Override
	public int getClientId() {
		return Objects.requireNonNull(clientId, "Can't obtain the client id before initialization");
	}

	@Override
	public void handleEvents(boolean isClosed, long[] eventIds, Object[] events) {
		if (updatesHandler != null) {
			LongArrayList idsToFilter = new LongArrayList(eventIds);
			ObjectArrayList<Object> eventsToFilter = new ObjectArrayList<>(events);

			for (int i = eventIds.length - 1; i >= 0; i--) {
				if (eventIds[i] != 0) {
					idsToFilter.removeLong(i);
					eventsToFilter.remove(i);

					long eventId = eventIds[i];
					Object event = events[i];

					Handler handler = handlers.remove(eventId);
					handleResponse(eventId, event, handler);
				}
			}

			try {
				updatesHandler.getUpdatesHandler().onUpdates(eventsToFilter);
			} catch (Throwable cause) {
				handleException(updatesHandler.getExceptionHandler(), cause);
			}
		} else {
			for (int i = 0; i < eventIds.length; i++) {
				handleEvent(eventIds[i], events[i]);
			}
		}

		if (isClosed) {
			if (this.isClosed.compareAndSet(false, true)) {
				handleClose();
			}
		}
	}

	private void handleClose() {
		logger.trace(TG_MARKER, "Received close");
		handlers.forEach((eventId, handler) -> {
			handleResponse(eventId, new Error(500, "Instance closed"), handler);
		});
		handlers.clear();
		logger.info(TG_MARKER, "Client closed {}", clientId);
	}

	/**
	 * Handles only a response (not an update!)
	 */
	private void handleResponse(long eventId, Object event, Handler handler) {
		if (handler != null) {
			try {
				handler.getResultHandler().onResult(event);
			} catch (Throwable cause) {
				handleException(handler.getExceptionHandler(), cause);
			}
		} else {
			logger.error(TG_MARKER, "Unknown event id \"{}\", the event has been dropped! {}", eventId, event);
		}
	}

	/**
	 * Handles a response or an update
	 */
	private void handleEvent(long eventId, Object event) {
		logger.trace(TG_MARKER, "Received response {}: {}", eventId, event);
		if (updatesHandler != null || updateHandler == null) throw new IllegalStateException();
		Handler handler = eventId == 0 ? updateHandler : handlers.remove(eventId);
		handleResponse(eventId, event, handler);
	}

	private void handleException(ExceptionHandler exceptionHandler, Throwable cause) {
		if (exceptionHandler == null) {
			exceptionHandler = defaultExceptionHandler;
		}
		if (exceptionHandler != null) {
			try {
				exceptionHandler.onException(cause);
			} catch (Throwable ignored) {}
		}
	}

	@Override
	public void initialize(UpdatesHandler updatesHandler,
			ExceptionHandler updateExceptionHandler,
			ExceptionHandler defaultExceptionHandler) {
		this.updateHandler = null;
		this.updatesHandler = new MultiHandler(updatesHandler, updateExceptionHandler);
		this.defaultExceptionHandler = defaultExceptionHandler;
		createAndRegisterClient();
	}

	@Override
	public void initialize(ResultHandler updateHandler,
			ExceptionHandler updateExceptionHandler,
			ExceptionHandler defaultExceptionHandler) {
		this.updateHandler = new Handler(updateHandler, updateExceptionHandler);
		this.updatesHandler = null;
		this.defaultExceptionHandler = defaultExceptionHandler;
		createAndRegisterClient();
	}

	private void createAndRegisterClient() {
		if (clientId != null) throw new UnsupportedOperationException("Can't initialize the same client twice!");
		clientId = NativeClientAccess.create();
		clientManager.registerClient(clientId, this);
		logger.info(TG_MARKER, "Registered new client {}", clientId);

		// Send a dummy request because @levlam is too lazy to fix race conditions in a better way
		this.send(new TdApi.GetAuthorizationState(), (result) -> {}, ex -> {});
	}

	@Override
	public void send(Function query, ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
		logger.trace(TG_MARKER, "Trying to send {}", query);
		if (isClosedAndMaybeThrow(query)) {
			resultHandler.onResult(new TdApi.Ok());
		}
		if (clientId == null) {
			ExceptionHandler handler = exceptionHandler == null ? defaultExceptionHandler : exceptionHandler;
			handler.onException(new IllegalStateException("Can't send a request to TDLib before calling \"initialize\" function!"));
			return;
		}
		long queryId = clientManager.getNextQueryId();
		if (resultHandler != null) {
			handlers.put(queryId, new Handler(resultHandler, exceptionHandler));
		}
		NativeClientAccess.send(clientId, queryId, query);
	}

	@Override
	public Object execute(Function query) {
		logger.trace(TG_MARKER, "Trying to execute {}", query);
		if (isClosedAndMaybeThrow(query)) {
			return new TdApi.Ok();
		}
		return NativeClientAccess.execute(query);
	}

	/**
	 *
	 * @param function function used to check if the check will be enforced or not. Can be null
	 * @return true if closed
	 */
	private boolean isClosedAndMaybeThrow(Function function) {
		boolean closed = isClosed.get();
		if (closed) {
			if (function != null && function.getConstructor() == TdApi.Close.CONSTRUCTOR) {
				return true;
			} else {
				throw new IllegalStateException("The client is closed!");
			}
		}
		return false;
	}
}
