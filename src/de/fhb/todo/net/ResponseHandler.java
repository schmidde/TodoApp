package de.fhb.todo.net;

/**
 * Interface für erfolgreiche bzw. nicht erfolgreiche Serverkommunikation
 *
 */
public interface ResponseHandler {

	/** 
	 * Konstante für den Fall das keine Internetverbindung vorhanden ist
	 */
	public static int FAILURE_REASON_NO_INTERNET = 1;

	/** 
	 * Konstante für den Fall das keine Internetverbindung vorhanden ist
	 */
	public static int FAILURE_REASON_WRONG_CREDENTIALS = 2;
	
	/**
	 * Callback für erfolgreiche Client-Server Kommunikation
	 */
	public void successfull();
	
	/**
	 * Callback für fehlgeschlagene Server Kommunikation
	 * @param reason - der Grund, warum es Fehlgeschlagen ist zb. {@link ResponseHandler#FAILURE_REASON_NO_INTERNET}
	 */
	public void failure(int reason);
}
