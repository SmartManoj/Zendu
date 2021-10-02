package it.tdlight.common;

import org.drinkless.td.libcore.telegram.TdApi;

public interface TelegramClient {

	/**
	 * Initialize the client synchronously.
	 *
	 * @param updatesHandler          Handler in which the updates are received
	 * @param updateExceptionHandler  Handler in which the errors from updates are received
	 * @param defaultExceptionHandler Handler that receives exceptions triggered in a handler
	 */
//	void initialize(UpdatesHandler updatesHandler,
//                    ExceptionHandler updateExceptionHandler,
//                    ExceptionHandler defaultExceptionHandler);

	/**
	 * Initialize the client synchronously.
	 *
	 * @param updateHandler           Handler in which the updates are received
	 * @param updateExceptionHandler  Handler in which the errors from updates are received
	 * @param defaultExceptionHandler Handler that receives exceptions triggered in a handler
	 */
//	default void initialize(ResultHandler updateHandler,
//                            ExceptionHandler updateExceptionHandler,
//                            ExceptionHandler defaultExceptionHandler) {
//		this.initialize((UpdatesHandler) updates -> updates.forEach(updateHandler::onResult),
//				updateExceptionHandler,
//				defaultExceptionHandler
//		);
//	}

	/**
	 * Sends a request to the TDLib.
	 *
	 * @param query            Object representing a query to the TDLib.
	 * @param resultHandler    Result handler with onResult method which will be called with result of the query or with
	 *                         TdApi.Error as parameter. If it is null, nothing will be called.
	 * @param exceptionHandler Exception handler with onException method which will be called on exception thrown from
	 *                         resultHandler. If it is null, then defaultExceptionHandler will be called.
	 * @throws NullPointerException if query is null.
	 */
	void send(TdApi.Function query, ResultHandler resultHandler, ExceptionHandler exceptionHandler);

	/**
	 * Sends a request to the TDLib with an empty ExceptionHandler.
	 *
	 * @param query         Object representing a query to the TDLib.
	 * @param resultHandler Result handler with onResult method which will be called with result of the query or with
	 *                      TdApi.Error as parameter. If it is null, then defaultExceptionHandler will be called.
	 * @throws NullPointerException if query is null.
	 */
	default void send(TdApi.Function query, ResultHandler resultHandler) {
		send(query, resultHandler, null);
	}

	/**
	 * Synchronously executes a TDLib request. Only a few marked accordingly requests can be executed synchronously.
	 *
	 * @param query Object representing a query to the TDLib.
	 * @return request result or {@link TdApi.Error}.
	 * @throws NullPointerException if query is null.
	 */
	static TdApi.Object execute(TdApi.Function query) {
		return null;
	}
}
