package sk.eea.arttag.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sk.eea.arttag.GameProperties;
import sk.eea.arttag.game.model.*;
import sk.eea.arttag.game.model.GameException.GameExceptionType;

import java.util.Date;
import java.util.Iterator;

@Component
public class StateMachine {

    private static final Logger LOG = LoggerFactory.getLogger(StateMachine.class);

    @Autowired
    private GameService gameService;

    @Autowired
    private GameProperties gameProperties;

    public void triggerEvent(Game game, GameEvent event, UserInput userInput, String userToken, Player player) throws GameException {
        LOG.debug("Trigger game event");

        if (game == null) {
            LOG.info("Game null");
            return;
        }
        //verify validity of userInput (if not null)

        switch (game.getStatus()) {
            case NEW:
                if (GameEvent.GAME_CREATED == event) {
                    game.setStatus(GameStatus.CREATED);
                    //TODO: start a timer or not?
                    game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutGameCreated()));
                    gameService.getGames().put(game.getId(), game);
                }
                break;

            case CREATED:
/*                if (GameEvent.PLAYER_JOINED == event) {
                    joinGame(game, player);

                } else */if (GameEvent.ROUND_STARTED == event) {
                    // PLAYER STARTED A GAME (user input)
                    processUserInput(game, userInput, userToken);

                } else if (GameEvent.TIMEOUT == event) {
                    // start the game?
                    // or discard game due to minimal number of players not reached?
                    // or start a new timer?
                    startGame(game);
                }
                break;

            case ROUND_STARTED:
/*                if (GameEvent.PLAYER_JOINED == event) {
                    joinGame(game, player);

                } else */if (GameEvent.TAGS_SELECTED == event) {
                    processUserInput(game, userInput, userToken);
                    //verify tags set correctly
                    if (game.getTags() != null) {
                        game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutTopicSelected()));
                        game.setStatus(GameStatus.ROUND_TOPIC_SELECTED);
                    }

                } else if (GameEvent.TIMEOUT == event) {
                    // dealer did not select any tags, finish round
                    finishRound(game);
                }
                break;

            case ROUND_TOPIC_SELECTED:
/*                if (GameEvent.PLAYER_JOINED == event) {
                    joinGame(game, player);

                } else */if (GameEvent.PLAYER_OWN_CARD_SELECTED == event) {
                    processUserInput(game, userInput, userToken);
                    // check if all players already selected own card (ignoring inactive players)
                    boolean notYetAllPlayersSelected = game.getPlayers().stream().anyMatch(p -> !p.isInactive() && p.getOwnCardSelection() == null);
                    if (!notYetAllPlayersSelected) {
                        game.setStatus(GameStatus.ROUND_OWN_CARDS_SELECTED);
                        game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutOwnCardsSelected()));
                        showTable(game);
                    }
                } else if (GameEvent.TIMEOUT == event) {
                    // timeout triggered before all players selected own cards
                    // deactivate players who failed to select cards
                    game.getPlayers().stream().filter(p -> p.getOwnCardSelection() == null).forEach(p -> p.setInactive(true));

                    game.setStatus(GameStatus.ROUND_OWN_CARDS_SELECTED);
                    game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutOwnCardsSelected()));
                    showTable(game);
                }
                break;

            case ROUND_OWN_CARDS_SELECTED:
/*                if (GameEvent.PLAYER_JOINED == event) {
                    joinGame(game, player);

                } else */if (GameEvent.PLAYER_TABLE_CARD_SELECTED == event) {
                    processUserInput(game, userInput, userToken);
                    // check if all players already selected own card (ignoring inactive)
                    boolean notYetAllPlayersSelected = game.getPlayers().stream()
                            .anyMatch(p -> !p.isInactive() && !p.isDealer() && p.getTableCardSelection() == null);
                    if (!notYetAllPlayersSelected) {
                        finishRound(game);
                        showTable(game);
                    }
                } else if (GameEvent.TIMEOUT == event) {
                    // timeout triggered before all players selected table cards
                    // deactivate players who failed to select cards
                    game.getPlayers().stream().filter(p -> !p.isDealer() && p.getTableCardSelection() == null).forEach(p -> p.setInactive(true));

                    finishRound(game);
                    showTable(game);
                }
                break;

            /*		case ROUND_TABLE_CARDS_SELECTED:
            			//finishRound();
            			break;*/

            case ROUND_FINISHED:
                /*			if (GameEvent.ROUND_STARTED == event) {
                				game.setEndOfRound(timeout(GameService.ROUND_LENGTH_IN_SECONDS));
                				game.setStatus(GameStatus.ROUND_STARTED);
                
                			} else */
/*                if (GameEvent.PLAYER_JOINED == event) {
                    joinGame(game, player);

                } else */if (GameEvent.TIMEOUT == event) {
                    // start the game? or discard game due to minimal number of
                    // players not reached? or start a new timer?
                    // deactivate players who failed to press continue
                    game.getPlayers().stream().filter(p -> !p.isReadyForNextRound()).forEach(p -> p.setInactive(true));

                    startRound(game, false);

                } else if (GameEvent.PLAYER_READY_FOR_NEXT_ROUND == event) {

                    processUserInput(game, userInput, userToken);
                    if (true) {//verify all set correctly
                    }

                    boolean notYetAllPlayersSelected = game.getPlayers().stream().anyMatch(p -> !p.isReadyForNextRound());
                    if (!notYetAllPlayersSelected) {
                        startRound(game, false);
                    }
                }
                break;

            case FINISHED:
                break;

            default:
                break;
        }

        if (GameEvent.PLAYER_JOINED == event) {
            joinGame(game, player);
        } else if (GameEvent.PLAYER_JOINED == event) {
        }

        LOG.debug("All set, notify game updated");
        gameService.gameUpdated(game);
    }

    private void nextRound(Game game, boolean isNewGame) throws GameException {

        // evaluate number of players in game, possibly we can start the game
        if (game.getMinPlayers() <= game.getNumberOfActivePlayers()) {
            //OK
        } else {
            throw new GameException(GameExceptionType.MIN_NUMBER_OF_PLAYERS_NOT_REACHED);
        }

        if (isNewGame) {
            game.setDeck(gameService.getInitialDeck(gameProperties.getInitialDeckSize(), game));
        }

        game.resetRound();

        boolean dealerFound = false;
        boolean dealerAssigned = false;
        //		Iterator<Entry<String, Player>> it = game.getPlayers().entrySet().iterator();
        Iterator<Player> it = game.getPlayers().iterator();
        while (it.hasNext()) {
            //		    Map.Entry<String, Player> entry = it.next();
            Player player = it.next();
            if (dealerFound && !dealerAssigned) {
                player.roundReset();
                if (!player.isInactive()) {
                    player.setDealer(true);
                    dealerAssigned = true;
                }
            } else {
                dealerFound = player.isDealer();
                player.roundReset();
            }
        }

        if (!dealerAssigned) {
            //			it = game.getPlayers().entrySet().iterator();
            it = game.getPlayers().iterator();
            while (it.hasNext()) {
                //			    Map.Entry<String, Player> entry = it.next();
                Player player = it.next();
                if (!player.isInactive()) {
                    player.setDealer(true);
                    break;
                }
            }
        }

        dealCards(game, gameProperties.getHandSize());
        //obsolete
        /*		boolean playerWithNoCardsExists = game.getPlayers().stream()
        				.anyMatch(p -> p.getHand().size() < 1);*/
    }

    private void showTable(Game game) {
        game.generateTable();
    }

    private void startGame(Game game) throws GameException {
        LOG.debug("Trying to start the game");
/*        // evaluate number of players in game, possibly we can start the game
        if (game.getMinPlayers() <= game.getPlayers().size()) {
            //			dealCards(game, GameService.HAND_SIZE);
            game.setDeck(gameService.getInitialDeck(gameProperties.getInitialDeckSize()));
            startRound(game);
        } else {
            throw new GameException(GameExceptionType.MIN_NUMBER_OF_PLAYERS_NOT_REACHED);
        }*/
        if (GameStatus.CREATED == game.getStatus()) {
            startRound(game, true);
        }
    }

    private void startRound(Game game, boolean isNewGame) throws GameException {
        try {
            nextRound(game, isNewGame);
        } catch (GameException e) {
            gameService.processGameSummary(game);
            game.setStatus(GameStatus.FINISHED);
            throw e;
        }
        game.setStatus(GameStatus.ROUND_STARTED);
        game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutRoundStarted()));
    }

    private void finishRound(Game game) {
        RoundSummary summary = RoundSummary.create(game);
        gameService.processRoundSummary(summary);
        game.updatePlayerScoreAfterRoundFinished(summary.getPlayerSummary());
        game.setStatus(GameStatus.ROUND_FINISHED);
        game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutRoundFinished()));
    }

    private void joinGame(Game game, Player player) throws GameException {
        //game status,
        //number of active players,
        //private/public,
        //join/rejoin (evaluate if userToken is already in joined players)

        Long userId = player.getUserId();
        Player playerInGame = game.findPlayerByUserId(userId);
        if (playerInGame != null) {
            //RECONNECT A PLAYER
            //evaluate game status (all except FINISHED)
            if (GameStatus.FINISHED == game.getStatus()) {
                LOG.info("Rejecting player {} to join the game, game already finished", userId);
                throw new GameException(GameExceptionType.GAME_ALREADY_FINISHED);
            }
            if (playerInGame.isInactive()) {
                //allow a player to reconnect only if he did not fail to take mandatory action in previous stage
                if (GameStatus.ROUND_OWN_CARDS_SELECTED == game.getStatus() && playerInGame.getOwnCardSelection() == null) {
                    //game status ROUND_OWN_CARDS_SELECTED, player failed to select his own card
                    LOG.info("Cannot activate player {}, player failed to select card in previous stage", userId);
                    throw new GameException(GameExceptionType.GAME_ALREADY_FINISHED);
                } else {
                    LOG.info("Reconnecting player '{}' to game", userId);
                    playerInGame.setInactive(false);
                    playerInGame.setToken(player.getToken());
                }
            } else {
                LOG.info("Player {} already in game", userId);
                throw new GameException(GameExceptionType.PLAYER_ALREADY_IN_GAME);
            }
        } else {
            //ADD A NEW PLAYER
            //evaluate min/max players
            //evaluate game status (only CREATED)
            if (GameStatus.CREATED == game.getStatus()) {
                if (game.getMaxPlayers() > game.getPlayers().size()) {
                    LOG.info("Adding player '{}' to game", userId);
                    game.addPlayer(player);
                } else {
                    LOG.info("Rejecting player {} to join the game, max number of players reached", userId);
                    throw new GameException(GameExceptionType.MAX_NUMBER_OF_PLAYERS_REACHED);
                }
            } else {
                LOG.info("Rejecting player {} to join the game, game already in progress", userId);
                throw new GameException(GameExceptionType.GAME_ALREADY_STARTED);
            }
        }

        // try to start the game after max number of players reached
        if (game.getMaxPlayers() == game.getNumberOfActivePlayers()) {
            startGame(game);
        }
    }

    private void dealCards(Game game, int numberOfCards) {
        LOG.debug("DEAL_CARDS");
        for (Player player : game.getPlayers()) {
            for (int i = player.getHand().size(); i < numberOfCards; i++) {
                if (game.getDeck().isEmpty()) {
                    LOG.debug("Deck empty, trying to refill");
                    //fill deck
                    game.setDeck(gameService.getInitialDeck(gameProperties.getInitialDeckSize(), game));
                }
                if (!game.getDeck().isEmpty()) {
                    Card card = game.getDeck().remove(0);
                    player.getHand().add(card);
                    LOG.debug("Adding card {} to player {}", card, player.getUserId());
                } else {
                    LOG.warn("Deck empty");
                }
            }
        }
    }

    private static Date timeout(int seconds) {
        return new Date(new Date().getTime() + (seconds * 1000));
    }

    private void processUserInput(Game game, UserInput input, String userToken) throws GameException {

        // check userToken in current game
        //		Player player = game.getPlayers().get(userToken);
        Player player = game.findPlayerByUserToken(userToken);
        if (player == null) {
            // ignore
            return;
        }

        // TODO check possible states for user input to be valid
        Card card;
        switch (input.getType()) {
            case TOPIC_SELECTED:

                if (!player.isDealer()) {
                    //player is not dealer, ignore
                    return;
                }
                // TODO: refactor this parsing!!!
                String[] parts = input.getValue().split(";", 2);
                if (parts == null || parts.length != 2) {
                    //invalid input, array should be of length 2
                    return;
                }

                game.setTags(parts[0]);

                // find the card by token
                card = player.getHand().stream().filter(c -> parts[1].equalsIgnoreCase(c.getToken())).findFirst().get();
                if (card == null) {
                    // ignore
                    return;
                }
                // pop the card out of players hand
                player.getHand().remove(card);
                // set to ownSelection
                player.setOwnCardSelection(card);

                card.setCardSelectedBy(player.getUserId());
                card.setDealersCard(player.isDealer());
                break;

            case OWN_CARD_SELECTED: {

                if (player.isDealer()) {
                    //ignore
                    return;
                }

                String cardToken = input.getValue();
                // find the card by token
                card = player.getHand().stream().filter(c -> cardToken.equalsIgnoreCase(c.getToken())).findFirst().get();
                if (card == null) {
                    // ignore
                    return;
                }
                // pop the card out of players hand
                player.getHand().remove(card);
                // set to ownSelection
                player.setOwnCardSelection(card);

                card.setCardSelectedBy(player.getUserId());
                break;
            }

            case TABLE_CARD_SELECTED: {

                if (player.isDealer()) {
                    //ignore
                    return;
                }

                String cardToken = input.getValue();
                // find the card by token
                card = game.getTable().stream().filter(c -> cardToken.equalsIgnoreCase(c.getToken())).findFirst().get();
                if (card == null) {
                    // ignore
                    return;
                }
                // set to tableSelection
                player.setTableCardSelection(card);
                card.addPlayerSelection(player.getName());
                break;
            }

            case PLAYER_READY_FOR_NEXT_ROUND: {
                player.setReadyForNextRound(true);
                break;
            }

            case GAME_STARTED: {
                // verify event sender is game creator
                if (game.getCreatorUserId().equals(player.getUserId())) {
                    // try to start a game
                    if (game.getMinPlayers() <= game.getNumberOfActivePlayers()) {
                        startGame(game);
                    }
                }
                break;
            }

            default:
                break;
        }
    }
}
