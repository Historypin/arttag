package sk.eea.arttag.game.model;

import java.util.ArrayList;
import java.util.List;

public class GamePlayerView {

	private String userToken;
	private boolean dealer;
	private List<Card> hand = new ArrayList<>();
	private GameView gameView;

	public GamePlayerView() {
	}

	public String getUserToken() {
		return userToken;
	}
	public void setUserToken(String userToken) {
		this.userToken = userToken;
	}
	public boolean isDealer() {
		return dealer;
	}
	public void setDealer(boolean dealer) {
		this.dealer = dealer;
	}
	public List<Card> getHand() {
		return hand;
	}
	public void setHand(List<Card> hand) {
		this.hand = hand;
	}
	public GameView getGameView() {
		return gameView;
	}
	public void setGameView(GameView gameView) {
		this.gameView = gameView;
	}

	@Override
	public String toString() {
		return "GamePlayerView [userToken=" + userToken + ", dealer=" + dealer + ", hand=" + hand + ", gameView="
				+ gameView + "]";
	}
}
