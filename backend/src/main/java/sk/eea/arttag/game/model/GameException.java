package sk.eea.arttag.game.model;

public class GameException extends Exception {

	public enum GameExceptionType {
		PLAYER_ALREADY_IN_GAME,
		GAME_ALREADY_STARTED,
		GAME_ALREADY_FINISHED,
		MAX_NUMBER_OF_PLAYERS_REACHED,
		MIN_NUMBER_OF_PLAYERS_NOT_REACHED,
		CANNOT_ACTIVATE_PLAYER_FAILED_TO_SELECT_OWN_CARD,
		GAME_NAME_ALREADY_IN_USE,
		GAME_NOT_FOUND
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
