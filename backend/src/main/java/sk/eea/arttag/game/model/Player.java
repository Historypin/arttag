package sk.eea.arttag.game.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class Player {

	private String token;
	private String name;
	private Long userId;
    @JsonIgnore
	private List<Card> hand = new ArrayList<>();
    @JsonIgnore
    private Card ownCardSelection;
    @JsonIgnore
	private Card tableCardSelection;
    private boolean ownCardSelected = false;
    private boolean tableCardSelected = false;
	private boolean readyForNextRound;
	private boolean dealer;
	private boolean inactive;//disconnected
	private boolean skipThisRound;//user failed to act
	private int gameScore;
	private int lastRoundScore;

	public Player() {
	}
	public Player(String token, String name, Long userId) {
		this.token = token;
		this.name = name;
		this.userId = userId;
		this.gameScore = 0;
		this.lastRoundScore = 0;
	}

	public List<Card> getHand() {
		return hand;
	}
	public void setHand(List<Card> hand) {
		this.hand = hand;
	}
	public boolean isDealer() {
		return dealer;
	}
	public void setDealer(boolean dealer) {
		this.dealer = dealer;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	public Card getOwnCardSelection() {
		return ownCardSelection;
	}
	public void setOwnCardSelection(Card ownCardSelection) {
		this.ownCardSelection = ownCardSelection;
		this.ownCardSelected = ownCardSelection == null ? false : true;
	}
	public Card getTableCardSelection() {
		return tableCardSelection;
	}
	public void setTableCardSelection(Card tableCardSelection) {
		this.tableCardSelection = tableCardSelection;
        this.tableCardSelected = tableCardSelection == null ? false : true;
	}
	public boolean isReadyForNextRound() {
		return readyForNextRound;
	}
	public void setReadyForNextRound(boolean readyForNextRound) {
		this.readyForNextRound = readyForNextRound;
	}
	public boolean isInactive() {
		return inactive;
	}
	public void setInactive(boolean inactive) {
		this.inactive = inactive;
	}
	public int getGameScore() {
		return gameScore;
	}
	public void setGameScore(int gameScore) {
		this.gameScore = gameScore;
	}
	public int getLastRoundScore() {
		return lastRoundScore;
	}
	public void setLastRoundScore(int lastRoundScore) {
		this.lastRoundScore = lastRoundScore;
	}
	public boolean isOwnCardSelected() {
        return ownCardSelected;
    }
    public boolean isTableCardSelected() {
        return tableCardSelected;
    }

    @Override
	public String toString() {
		return "Player [token=" + token + ", name=" + name + ", userId=" + userId + ", hand=" + hand
				+ ", ownCardSelection=" + ownCardSelection + ", tableCardSelection=" + tableCardSelection
				+ ", readyForNextRound=" + readyForNextRound + ", dealer=" + dealer + ", inactive=" + inactive
				+ ", gameScore=" + gameScore + ", lastRoundScore=" + lastRoundScore + "]";
	}

	public void roundReset() {
		this.ownCardSelection = null;
		this.tableCardSelection = null;
		this.ownCardSelected = false;
		this.tableCardSelected = false;
		this.readyForNextRound = false;
		this.dealer = false;
		this.lastRoundScore = 0;
	}
}
