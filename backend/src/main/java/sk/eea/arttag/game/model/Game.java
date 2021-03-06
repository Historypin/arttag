package sk.eea.arttag.game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Game {

    private static final Logger LOG = LoggerFactory.getLogger(Game.class);

    private final String id;
    private final String name;
    private Date created;

    private final int minPlayers;
    private final int maxPlayers;
    private final boolean privateGame;
    private final GameTimeout gameTimeout;

    private GameStatus status;
    private List<Player> players = new LinkedList<>();
    private Date endOfRound;
    private String tags;
    private List<Card> deck = new ArrayList<>();
    private List<Card> table = new ArrayList<>();
    private List<Card> tablePublic = new ArrayList<>();
    private Long creatorUserId;

    public Game(String id, String name, int minPlayers, int maxPlayers, boolean privateGame, Long creatorUserId, GameTimeout gameTimeout) {
        this.id = id;
        this.name = name;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.created = new Date();
        this.status = GameStatus.NEW;
        this.privateGame = privateGame;
        this.creatorUserId = creatorUserId;
        this.gameTimeout = gameTimeout;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public Date getEndOfRound() {
        return endOfRound;
    }

    public void setEndOfRound(Date endOfRound) {
        this.endOfRound = endOfRound;
    }

    public int getRemainingTime() {
        return (int) (endOfRound.getTime() - System.currentTimeMillis()) / 1000;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public List<Card> getDeck() {
        return deck;
    }

    public void setDeck(List<Card> deck) {
        this.deck = deck;
    }

    public List<Card> getTable() {
        return table;
    }

/*    public List<Card> getTablePublic() {
        return tablePublic;
    }*/

    public List<Card> getTablePublic(Long userId) {
        if (GameStatus.ROUND_OWN_CARDS_SELECTED == getStatus()) {
            return table.stream().map(c -> new Card(c, userId)).collect(Collectors.toList());
        } else if (GameStatus.ROUND_FINISHED == getStatus()) {
            return table;
        }
        return new ArrayList<>();
    }

    public void generateTable() {
        LOG.debug("Generate table for game {}, status {}", id, status);
/*        List<Card> table = new ArrayList<>();
//        List<Card> tablePublic = new ArrayList<>();
        if (GameStatus.ROUND_OWN_CARDS_SELECTED == getStatus()) {
            table = getPlayers().stream().filter(p -> p.getOwnCardSelection() != null).map(p -> p.getOwnCardSelection())
                    .collect(Collectors.toList());
            Collections.shuffle(table);
//            tablePublic = table.stream().map(c -> new Card(c)).collect(Collectors.toList());
        } else if (GameStatus.ROUND_FINISHED == getStatus()) {
//            tablePublic = getTable();
        }
        setTable(table);
//        setTablePublic(tablePublic);
*/
        table = getPlayers().stream().filter(p -> p.getOwnCardSelection() != null).map(p -> p.getOwnCardSelection())
                .collect(Collectors.toList());
        Collections.shuffle(table);
    }

    public void setTable(List<Card> table) {
        this.table = table;
    }

/*    public void setTablePublic(List<Card> tablePublic) {
        this.tablePublic = tablePublic;
    }*/

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isPrivateGame() {
        return privateGame;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }

    public GameTimeout getGameTimeout() {
        return gameTimeout;
    }

    public void resetRound() {
        table = new ArrayList<>();
        tablePublic = new ArrayList<>();
        tags = null;
    }

    public Player findPlayerByUserId(Long userId) {
        /*		List<Player> players = getPlayers().values().stream()
                        .filter(p -> userId.equalsIgnoreCase(p.getUserId()))
        				.collect(Collectors.toList());*/
        List<Player> players = getPlayers().stream().filter(p -> userId.equals(p.getUserId())).collect(Collectors.toList());
        return players.size() > 0 ? players.get(0) : null;
    }

    public Player findPlayerByUserToken(String userToken) {
        List<Player> players = getPlayers().stream().filter(p -> userToken.equalsIgnoreCase(p.getToken())).collect(Collectors.toList());
        return players.size() > 0 ? players.get(0) : null;
    }

    @Override
    public String toString() {
        return "Game [getId()=" + getId() + ", getName()=" + getName() + ", getCreated()=" + getCreated() + ", getStatus()=" + getStatus() + ", getPlayers()="
                + getPlayers() + ", getEndOfRound()=" + getEndOfRound() + ", getRemainingTime()=" + getRemainingTime() + "]";
    }

    public List<GamePlayerView> createGameViews() {

        List<GamePlayerView> gamePlayerViews = new ArrayList<>();
        for (Player player : players) {

            GameView view = new GameView();
            view.setRemainingTime(getRemainingTime());
            view.setTags(getTags());
            view.setCreated(getCreated());
            view.setEndOfRound(getEndOfRound());
            view.setId(getId());
            view.setName(getName());
            view.setPlayers(getPlayers());
            view.setStatus(getStatus());
            view.setStartGameEnabled(GameStatus.CREATED == getStatus() && getMinPlayers() <= getNumberOfActivePlayers());
            view.setCreatorUserId(getCreatorUserId());
            view.setTable(getTablePublic(player.getUserId()));

            GamePlayerView gamePlayerView = new GamePlayerView();
            gamePlayerView.setGameView(view);
            gamePlayerView.setUserToken(player.getToken());
            gamePlayerView.setDealer(player.isDealer());
            gamePlayerView.setHand(player.getHand());
            gamePlayerViews.add(gamePlayerView);
            LOG.debug("GPV: {}", gamePlayerView.toString());
        }

        return gamePlayerViews;
    }

    public void updatePlayerScoreAfterRoundFinished(Map<Long, Integer> summary) {
        getPlayers().forEach(p -> {
            int score = summary.getOrDefault(p.getUserId(), 0);
            p.setLastRoundScore(score);
            p.setGameScore(p.getGameScore() + score);
        });
    }

    public long getNumberOfActivePlayers() {
        return players.stream().filter(p -> !p.isInactive()).count();
    }
}
