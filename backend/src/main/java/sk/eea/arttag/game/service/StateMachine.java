package sk.eea.arttag.game.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sk.eea.arttag.game.model.Card;
import sk.eea.arttag.game.model.Game;
import sk.eea.arttag.game.model.GameEvent;
import sk.eea.arttag.game.model.GameException;
import sk.eea.arttag.game.model.GameException.GameExceptionType;
import sk.eea.arttag.game.model.GameStatus;
import sk.eea.arttag.game.model.Player;
import sk.eea.arttag.game.model.RoundSummary;
import sk.eea.arttag.game.model.UserInput;

@Component
public class StateMachine {

	private static final Logger LOG = LoggerFactory.getLogger(StateMachine.class);

    @Autowired
	private GameService gameService;

	public void triggerEvent(Game game, GameEvent event, UserInput userInput, String userToken, Player player) throws GameException {

		if (game == null) {
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
			if (GameEvent.PLAYER_JOINED == event) {
				joinGame(game, player);

			} else if (GameEvent.ROUND_STARTED == event) {
				// PLAYER STARTED A GAME (user input)
				// verify event sender is game creator
				Player p = game.findPlayerByUserId(userInput.getPlayerName());
				if (p != null && game.getCreatorUserId().equalsIgnoreCase(userInput.getPlayerName())) {
					// try to start a game
					startGame(game);
				}

			} else if (GameEvent.TIMEOUT == event) {
				// start the game? 
				// or discard game due to minimal number of players not reached? 
				// or start a new timer?
				startGame(game);
			}
			break;

		case ROUND_STARTED:
			if (GameEvent.PLAYER_JOINED == event) {
				joinGame(game, player);

			} else if (GameEvent.TAGS_SELECTED == event) {
				processUserInput(game, userInput, userToken);
				if (true) {//verify tags set correctly
				}
				game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutTopicSelected()));
				game.setStatus(GameStatus.ROUND_TAGS_SELECTED);

			} else if (GameEvent.TIMEOUT == event) {
				// dealer did not select any tags, finish round
				finishRound(game);
			}
			break;

		case ROUND_TAGS_SELECTED:
			if (GameEvent.PLAYER_JOINED == event) {
				joinGame(game, player);

			} else if (GameEvent.PLAYER_OWN_CARD_SELECTED == event) {
				processUserInput(game, userInput, userToken);
				// check if all players already selected own card
				boolean notYetAllPlayersSelected = game.getPlayers().stream()
						.anyMatch(p -> p.getOwnCardSelection() == null);
				if (!notYetAllPlayersSelected) {
					game.setStatus(GameStatus.ROUND_OWN_CARDS_SELECTED);
					game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutOwnCardsSelected()));
					showTable(game);
				}
			} else if (GameEvent.TIMEOUT == event) {
				// timeout triggered before all players selected own cards
				game.setStatus(GameStatus.ROUND_OWN_CARDS_SELECTED);
				game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutOwnCardsSelected()));
				showTable(game);
			}
			break;

		case ROUND_OWN_CARDS_SELECTED:
			if (GameEvent.PLAYER_JOINED == event) {
				joinGame(game, player);

			} else if (GameEvent.PLAYER_TABLE_CARD_SELECTED == event) {
				processUserInput(game, userInput, userToken);
				// check if all players already selected own card
				boolean notYetAllPlayersSelected = game.getPlayers().stream()
						.anyMatch(p -> p.getTableCardSelection() == null);
				if (!notYetAllPlayersSelected) {
					finishRound(game);
				}
			} else if (GameEvent.TIMEOUT == event) {
				// timeout triggered before all players selected table cards
				finishRound(game);
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
			if (GameEvent.PLAYER_JOINED == event) {
				joinGame(game, player);

			} else if (GameEvent.TIMEOUT == event) {
				// start the game? or discard game due to minimal number of
				// players not reached? or start a new timer?
				startRound(game);

			} else if (GameEvent.PLAYER_READY_FOR_NEXT_ROUND == event) {

				processUserInput(game, userInput, userToken);
				if (true) {//verify all set correctly
				}

				boolean notYetAllPlayersSelected = game.getPlayers().stream()
						.anyMatch(p -> !p.isReadyForNextRound());
				if (!notYetAllPlayersSelected) {
					startRound(game);
				}
			}
			break;

		case FINISHED:
			break;

		default:
			break;
		}
	}

	private void nextRound(Game game) {

		game.resetRound();

		boolean dealerFound = false;
		boolean dealerAssigned = false;
//		Iterator<Entry<String, Player>> it = game.getPlayers().entrySet().iterator();
		Iterator<Player> it = game.getPlayers().iterator();
		while (it.hasNext()) {
//		    Map.Entry<String, Player> entry = it.next();
		    Player player = it.next();
		    if (dealerFound) {
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

		dealCards(game, GameService.HAND_SIZE);
		//obsolete
/*		boolean playerWithNoCardsExists = game.getPlayers().stream()
				.anyMatch(p -> p.getHand().size() < 1);*/
	}

	private void showTable(Game game) {
		List<Card> table = game.getPlayers().stream().filter(p -> p.getOwnCardSelection() != null).map(p -> p.getOwnCardSelection()).collect(Collectors.toList());
		game.setTable(table);
	}

	private void startGame(Game game) throws GameException {
		// evaluate number of players in game, possibly we can start the game
		if (game.getMinPlayers() <= game.getPlayers().size()) {
			dealCards(game, GameService.HAND_SIZE);
			startRound(game);
		} else {
			throw new GameException(GameExceptionType.MIN_NUMBER_OF_PLAYERS_NOT_REACHED);
		}
	}

	private void startRound(Game game) {
		nextRound(game);
		game.setStatus(GameStatus.ROUND_STARTED);
		game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutRoundStarted()));
	}

	private void finishRound(Game game) {
		RoundSummary summary = RoundSummary.create(game);
		gameService.processRoundSummary(summary);
		game.setStatus(GameStatus.ROUND_FINISHED);
		game.setEndOfRound(timeout(game.getGameTimeout().getTimeoutRoundFinished()));
	}

	private void joinGame(Game game, Player player) throws GameException {
		//game status, 
		//number of active players, 
		//private/public, 
		//join/rejoin (evaluate if userToken is already in joined players)

		String userId = player.getUserId();
		Player playerInGame = game.findPlayerByUserId(userId);
		if (playerInGame != null) {
			//RECONNECT A PLAYER
			//evaluate game status (all except FINISHED)
			if (GameStatus.FINISHED == game.getStatus()) {
				LOG.info("Rejecting player {} to join the game, game already finished", userId);
				throw new GameException(GameExceptionType.GAME_ALREADY_FINISHED);
			}
			if (playerInGame.isInactive()) {
				LOG.info("Reconnecting player '{}' to game", userId);
				playerInGame.setInactive(false);
				playerInGame.setToken(player.getToken());
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
		};

		// try to start the game after max number of players reached
		if (game.getMaxPlayers() == game.getPlayers().size()) {
			startGame(game);
		}
	}

/*	private List<Card> getInitialDeck() {
		LOG.debug("INITIAL_DECK");
        List<Card> deck = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            final String cardToken = String.format("%02d.jpeg", i);
            final String cardSource = String.format("%s/%s", applicationProperties.getHostname(), cardToken);
            deck.add(new Card(cardToken, cardSource));
        }
        Collections.shuffle(deck);
		return deck;
	}*/

	private void dealCards(Game game, int numberOfCards) {
		LOG.debug("DEAL_CARDS");
		for (Player player : game.getPlayers()) {
			List<Card> cards = new ArrayList<>();
			for (int i = player.getHand().size(); i < numberOfCards; i++) {
				cards.add(gameService.getCard(game));
			}
			player.setHand(cards);
		}
	}

	private static Date timeout(int seconds) {
		return new Date(new Date().getTime() + (seconds * 1000));
	}

	private void processUserInput(Game game, UserInput input, String userToken) {

		// check userToken in current game
//		Player player = game.getPlayers().get(userToken);
		Player player = game.findPlayerByUserToken(userToken);
		if (player == null) {
			// ignore
			return;
		}

		// TODO check possible states for user input to be valid

		switch (input.getType()) {
		case TAGS_SELECTED:
			game.setTags(input.getValue());
			break;

		case OWN_CARD_SELECTED: {
			String cardToken = input.getValue();
			// find the card by token
			Card card = player.getHand().stream().filter(c -> cardToken.equalsIgnoreCase(c.getToken())).findFirst()
					.get();
			if (card == null) {
				// ignore
				return;
			}
			// pop the card out of players hand
			player.getHand().remove(card);
			// set to ownSelection
			player.setOwnCardSelection(card);
			break;
		}

		case TABLE_CARD_SELECTED: {
			String cardToken = input.getValue();
			// find the card by token
			Card card = game.getTable().stream().filter(c -> cardToken.equalsIgnoreCase(c.getToken())).findFirst().get();
			if (card == null) {
				// ignore
				return;
			}
			// set to tableSelection
			player.setTableCardSelection(card);
			break;
		}

		case PLAYER_READY_FOR_NEXT_ROUND : {
			player.setReadyForNextRound(true);
			break;
		}

		default:
			break;
		}
	}
}
