package sk.eea.arttag.game.model;

public class GameException extends Exception {

	public enum GameExceptionType {
		PLAYER_ALREADY_IN_GAME
	}

	public GameException(GameExceptionType type) {
		super(type.name());
	}

	public GameException() {
		super();
	}

	public GameException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public GameException(String message, Throwable cause) {
		super(message, cause);
	}

	public GameException(String message) {
		super(message);
	}

	public GameException(Throwable cause) {
		super(cause);
	}
}