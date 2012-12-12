package ies.retry;

/**
 * On addition of a retry, if no callback has been registered this
 * exception will be thrown.
 * It will be considered a client bug.
 * 
 * @author msimonsen
 *
 */
public class NoCallbackException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5413587505843500104L;

	public NoCallbackException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public NoCallbackException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public NoCallbackException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public NoCallbackException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	
}
