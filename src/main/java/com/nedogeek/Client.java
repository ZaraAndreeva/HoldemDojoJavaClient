package com.nedogeek;


import org.eclipse.jetty.websocket.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class Client {
    private static final String userName = "Zara";
    private static final String password = "somePassword";

    private static final String SERVER = "ws://10.22.40.111:8080/ws";
    private org.eclipse.jetty.websocket.WebSocket.Connection connection;

    private enum Commands {
        Check, Call, Rise, Fold, AllIn
    }

    private class Card {
        final String suit;
        final String value;

        Card(String suit, String value) {
            this.suit = suit;
            this.value = value;
        }
    }


    private void con() {
        WebSocketClientFactory factory = new WebSocketClientFactory();
        try {
            factory.start();

            WebSocketClient client = factory.newWebSocketClient();

            connection = client.open(new URI(SERVER + "?user=" + userName + "&password=" + password), new org.eclipse.jetty.websocket.WebSocket.OnTextMessage() {
                public void onOpen(Connection connection) {
                    System.out.println("Opened");
                }

                public void onClose(int closeCode, String message) {
                    System.out.println("Closed");
                }

                public void onMessage(String data) {
                    parseMessage(data);
                    System.out.println(data);
                    if (userName.equals(mover)) {
                        try {
                            doAnswer(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class Player {

        final String name;
        final int balance;
        final int bet;
        final String status;
        final List<Card> cards;

        Player(String name, int balance, int bet, String status, List<Card> cards) {
            this.name = name;
            this.balance = balance;
            this.bet = bet;
            this.status = status;
            this.cards = cards;
        }

    }

    private List<Card> deskCards;

    private int pot;
    private String gameRound;

    private String dealer;
    private String mover;
    private List<String> event;
    private List<Player> players;

    private String cardCombination;

    private Client() {
        con();
    }

    public static void main(String[] args) {
        new Client();
//        ArrayList<String> cards = new ArrayList<>();
//
//        cards.add("7");
//        cards.add("3");
//        cards.add("9");
//        cards.add("Q");
//        cards.add("5");
//        cards.add("A");
////       cards.add("4");
//        System.out.println(sequentialCards(cards));
    }

    private void parseMessage(String message) {
        JSONObject json = new JSONObject(message);

        if (json.has("deskPot")) {
            pot = json.getInt("deskPot");
        }
        if (json.has("mover")) {
            mover = json.getString("mover");
        }
        if (json.has("dealer")) {
            dealer = json.getString("dealer");
        }
        if (json.has("gameRound")) {
            gameRound = json.getString("gameRound");
        }
        if (json.has("event")) {
            event = parseEvent(json.getJSONArray("event"));
        }
        if (json.has("players")) {
            players = parsePlayers(json.getJSONArray("players"));
        }

        if (json.has("deskCards")) {
            deskCards = parseCards(((JSONArray) json.get("deskCards")));
        }

        if (json.has("combination")) {
            cardCombination = json.getString("combination");
        }
    }

    private List<String> parseEvent(JSONArray eventJSON) {
        List<String> events = new ArrayList<>();

        for (int i = 0; i < eventJSON.length(); i++) {
            events.add(eventJSON.getString(i));
        }

        return events;
    }

    private List<Player> parsePlayers(JSONArray playersJSON) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < playersJSON.length(); i++) {
            JSONObject playerJSON = (JSONObject) playersJSON.get(i);
            int balance = 0;
            int bet = 0;
            String status = "";
            String name = "";
            List<Card> cards = new ArrayList<>();

            if (playerJSON.has("balance")) {
                balance = playerJSON.getInt("balance");
            }
            if (playerJSON.has("pot")) {
                bet = playerJSON.getInt("pot");
            }
            if (playerJSON.has("status")) {
                status = playerJSON.getString("status");
            }
            if (playerJSON.has("name")) {
                name = playerJSON.getString("name");
            }
            if (playerJSON.has("cards")) {
                cards = parseCards((JSONArray) playerJSON.get("cards"));
            }

            players.add(new Player(name, balance, bet, status, cards));
        }

        return players;
    }

    private List<Card> parseCards(JSONArray cardsJSON) {
        List<Card> cards = new ArrayList<>();

        for (int i = 0; i < cardsJSON.length(); i++) {
            String cardSuit = ((JSONObject) cardsJSON.get(i)).getString("cardSuit");
            String cardValue = ((JSONObject) cardsJSON.get(i)).getString("cardValue");

            cards.add(new Card(cardSuit, cardValue));
        }

        return cards;
    }



    private boolean hasEveryonesTurnBeen = false;

    private void doAnswer(String message) throws IOException {

        //my player's cards
        int counter = 0;
        Card card1 = null;
        Card card2 = null;
        for (Card c : myPlayer().cards) {
            if (counter == 0) {
                card1 = c;
                System.out.println(c.value + "  " + c.suit);
            } else {
                card2 = c;
                System.out.println(c.value + "  " + c.suit);
                break;
            }
            counter++;
        }

        int bigBlind = 0;
        for (Player p : players) {
            if (p.status.equals("BigBlind")) {
                bigBlind = p.bet;
            }
        }

        if (gameRound.equals("BLIND")) {
            PreFlop(card1, card2);
        }

        if (gameRound.equals("THREE_CARDS")) {
           Flop(card1, card2);
        }

        if (gameRound.equals("FOUR_CARDS")) {
            Turn(card1, card2, bigBlind);
        }

        if (gameRound.equals("FIVE_CARDS")) {
            River(card1, card2, bigBlind);
        }
        if (gameRound.equals("FINAL")) {
            Final(card1, card2, bigBlind);
        }

    }

    private Player myPlayer() {
        //my player
        Player myPlayer = null;
        for (Player player : players) {
            if (player.name.equals(userName)) {
                myPlayer = player;
                break;
            }
        }
        return myPlayer;
    }

    private void PreFlop(Card card1, Card card2) {
        try {
            int bigBlind = 0;
            for (Player p : players) {
                if (p.status.equals("BigBlind")) {
                    bigBlind = p.bet;
                }
            }

            if (arePair(card1, card2)) {
//                boolean doWeHaveBigPair = false;
//                for (int i = 10; i < allcards().size(); i++) {
//                    if (card1.value.equals(allcards().get(i))) {
//                        doWeHaveBigPair = true;
//                        //if s.o. raise -> if raise >= balance -> call else -> if raise*3 > balance -> all in -> else pot*3
//                        //else -> if pot*2 > balance -> all in -> else -> rise pot*2
//                        if (howMuchHasSomeoneRaised(bigBlind) != 0) {
//                            hasEveryonesTurnBeen = true;
//                            if (howMuchHasSomeoneRaised(bigBlind) >= myPlayer().balance) {
//                                connection.sendMessage(Commands.Call.toString());
//                            } else {
//                                if (pot * 3 >= myPlayer().balance) {
//                                    connection.sendMessage(Commands.AllIn.toString());
//                                } else {
//                                    connection.sendMessage(Commands.Rise.toString() + "," + pot / 2);
//                                }
//                            }
//                            break;
//                        } else {
//                            hasEveryonesTurnBeen = true;
//                            howMuchToRaiseDependingOnPot();
//                            break;
//                        }
//                    }
//                }
//                if (hasEveryonesTurnBeen) {
//                    connection.sendMessage(Commands.Call.toString());
//                }
//                hasEveryonesTurnBeen = false;
//                //if s.o. raise -> if raise >= balance call else -> pot*2
//                //else -> if pot*2 >= balance -> call else -> pot*2
//                if (!doWeHaveBigPair) {
//                    if (howMuchHasSomeoneRaised(bigBlind) != 0) {
//                        connection.sendMessage(Commands.Call.toString());
//                    } else {
//                        howMuchToRaiseDependingOnPot();
//                    }
//                }

                boolean doWeHaveBigPair = false;
                for (int i = 10; i < allcards().size(); i++) {
                    if (card1.value.equals(allcards().get(i))) {
                        doWeHaveBigPair = true;
                    }
                }
                if(doWeHaveBigPair){
                    connection.sendMessage(Commands.Rise.toString() + "," + pot*3);
                }
                else{
                    connection.sendMessage(Commands.Rise.toString() + "," + pot*2);
                }
            } else if (areSuit(card1, card2)) {
                if (card1.value.equals(allcards().get(allcards().size() - 1)) || card2.value.equals(allcards().get(allcards().size() - 1))) {
                    //if s.o. raise -> call
                    //else raise pot*2
                    if (howMuchHasSomeoneRaised(bigBlind) != 0) {
                        connection.sendMessage(Commands.Check.toString());
                    } else {
                        howMuchToRaiseDependingOnPot();
                    }
                } else {
                    boolean highCard = false;
                    for (int i = 8; i < allcards().size(); i++) {
                        if (card1.value.equals(allcards().get(i)) || card2.value.equals(allcards().get(i))) {
                            highCard = true;
                            if (howMuchHasSomeoneRaised(bigBlind) != 0) {
                                connection.sendMessage(Commands.Call.toString());
                                break;
                            } else {
                                howMuchToRaiseDependingOnPot();
                            }
                        }
                    }
                    if (!highCard) {
                        checkIfNoOneRaisedToPlay();
                    }
                }

            } else if (bothAreBiggerThan10()) {
                //if no one raise -> raise pot*2, else -> call
                if (howMuchHasSomeoneRaised(bigBlind) != 0) {
                    connection.sendMessage(Commands.Call.toString());
                } else {
                    howMuchToRaiseDependingOnPot();
                }
            } else if (areSequential(allcards(), card1, card2)) {
                //if s.o. raise -> call
                //else raise pot*2
                connection.sendMessage(Commands.Check.toString());
            } else {
                switch (myPlayer().status) {
                    case "SmallBlind":
                        //if someone raise -> fold, else -> call
                        if (howMuchHasSomeoneRaised(bigBlind) != 0) {
                            connection.sendMessage(Commands.Check.toString());
                        } else {
                            connection.sendMessage(Commands.Check.toString());
                        }
                        break;
                    case "BigBlind":
                        //if no one raise -> check else -> if raise is small -> call else -> fold
                        if (howMuchHasSomeoneRaised(bigBlind) != 0 && myPlayer().balance - howMuchHasSomeoneRaised(bigBlind) > 500) {
                            connection.sendMessage(Commands.Check.toString());
                        } else {
                            connection.sendMessage(Commands.Check.toString());
                        }
                        break;
                    default:
                        checkIfNoOneRaisedToPlay();
                        break;
                }
            }
        }
        catch (IOException e){
            System.out.println("PreFlop exception");
            System.out.println(e.getMessage());
        }
    }

    private void Flop(Card card1, Card card2){
        try{
                if (cardCombination.contains("Straight")) {
                    connection.sendMessage(Commands.Call.toString());
                } else if (cardCombination.contains("Four of")) {
                    //todo
                    connection.sendMessage(Commands.AllIn.toString());
                } else if (cardCombination.contains("Flash")) {
                    connection.sendMessage(Commands.AllIn.toString());
                } else if (cardCombination.contains("Full house")) {
                    connection.sendMessage(Commands.AllIn.toString());
                } else if (cardCombination.contains("Pair of")) {
                    if(pairOnFlop()){
                        if(checkForFlushCountCards(card1, card2) == 4){
                            connection.sendMessage(Commands.Call.toString());
                        }
                        else{
                            checkIfNoOneRaisedToPlay();
                        }
                    }
                    else{
                        connection.sendMessage(Commands.Rise.toString() + "," + pot / 2);
                    }
                } else if (cardCombination.contains("Two pairs")) {
                    if (pairOnFlop()) {
                        if (playersThatAreNotFold() < 3) {
                            howMuchToRaiseDependingOnPot();
                        } else {
                            connection.sendMessage(Commands.Call.toString());
                        }
                    } else {
                        if (!arePair(card1, card2)) {
                            connection.sendMessage(Commands.Rise.toString() + "," + pot);
                        }
                        else{
                            connection.sendMessage(Commands.Rise.toString() + "," + pot / 2);
                        }
                    }
                } else if (cardCombination.contains("Set of")) {
                    ArrayList<String> cardsOnFlopValue = new ArrayList<>(3);
                    for (Card c : deskCards) {
                        cardsOnFlopValue.add(c.value);
                    }
                    if (cardsOnFlopValue.get(0).equals(cardsOnFlopValue.get(1)) && cardsOnFlopValue.get(0).equals(cardsOnFlopValue.get(2))) {
                        connection.sendMessage(Commands.Call.toString());
                    } else {
                        if (arePair(card1, card2)) {
                            connection.sendMessage(Commands.Rise.toString() + "," + pot/2);
                        } else {
                            if (card1.value.equals(allcards().get(allcards().size() - 1)) || card2.value.equals(allcards().get(allcards().size() - 1)) || card1.value.equals(allcards().get(allcards().size() - 2)) || card2.value.equals(allcards().get(allcards().size() - 2)) || card1.value.equals(allcards().get(allcards().size() - 3)) || card2.value.equals(allcards().get(allcards().size() - 3))) {
                                connection.sendMessage(Commands.Rise.toString() + "," + pot / 2);
                            } else {
                                ArrayList<String> cards = new ArrayList<>();
                                for(Card c : deskCards){
                                    cards.add(c.value);
                                }
                                cards.add(card1.value);
                                cards.add(card2.value);
                                if(sequentialCards(cards) == 4 ){
                                    connection.sendMessage(Commands.Check.toString());
                                }
                                else{
                                    if(checkForFlushCountCards(card1, card2) == 4){
                                        connection.sendMessage(Commands.Call.toString());
                                    }
                                    else {
                                        connection.sendMessage(Commands.Check.toString());
                                    }
                                }
                            }
                        }
                    }
                } else if (cardCombination.contains("High card")) {
                    ArrayList<String> cards = new ArrayList<>();
                    for(Card c : deskCards){
                        cards.add(c.value);
                    }
                    cards.add(card1.value);
                    cards.add(card2.value);
                    if(sequentialCards(cards) == 4 || checkForFlushCountCards(card1, card2) == 4){
                        connection.sendMessage(Commands.Check.toString());
                    }
                    else{
                        connection.sendMessage(Commands.Check.toString());
                    }
                }
                else{
                    checkIfNoOneRaisedToPlay();
                }
            }
             catch (IOException e){
                System.out.println("Flop exception");
                System.out.println(e.getMessage());
            }
        }

    private void Turn(Card card1, Card card2, int bigBlind){
            try {
                if (cardCombination.contains("Straight")) {
                    connection.sendMessage(Commands.Call.toString());
                } else if (cardCombination.contains("Four of")) {
                   checkForFourEqualCardsOnTable();
                } else if (cardCombination.contains("Flash")) {
                    connection.sendMessage(Commands.AllIn.toString());
                } else if (cardCombination.contains("Full house")) {
                    connection.sendMessage(Commands.AllIn.toString());
                } else if (cardCombination.contains("Pair of")) {
                    checkForPairOnTable(card1, card2, bigBlind);
                } else if (cardCombination.contains("Two pairs")) {
                    //if the pairs are on table
                    checkIfTwoPairsOnTable(card1, card2);
                } else if (cardCombination.contains("Set of")) {
                    //if set is on table
                    checkForSetOnTable();
                } else if (cardCombination.contains("High card")) {
                    ArrayList<String> cards = new ArrayList<>();
                    for(Card c : deskCards){
                        cards.add(c.value);
                    }
                    cards.add(card1.value);
                    cards.add(card2.value);
                    if(sequentialCards(cards) == 4 || checkForFlushCountCards(card1, card2) == 4){
                        connection.sendMessage(Commands.Check.toString());
                    }
                    else{
                        connection.sendMessage(Commands.Check.toString());
                    }
                }
                else{
                    checkIfNoOneRaisedToPlay();
                }
            }
            catch (IOException e){
                System.out.println("TURN Exception");
                System.out.println(e.getMessage());
            }
        }

         private void River(Card card1, Card card2, int bigBlind){
            try {
                if (cardCombination.contains("Straight")) {
                    //check if straight is on the table
                    ArrayList<String> cardsOnTable = new ArrayList<>(5);
                    for(Card c : deskCards){
                        cardsOnTable.add(c.value);
                    }
                    if(sequentialCards(cardsOnTable) == 5){
                        connection.sendMessage(Commands.Check.toString());
                    }
                    else {
                        connection.sendMessage(Commands.Call.toString());
                    }
                } else if (cardCombination.contains("Four of")) {
                    checkForFourEqualCardsOnTable();
                } else if (cardCombination.contains("Flash")) {
                    checkForFlashOnTheTable();
                } else if (cardCombination.contains("Full house")) {
                    checkForFulHouseOnTable();
                } else if (cardCombination.contains("Pair of")) {
                    checkForPairOnTable(card1, card2, bigBlind);
                } else if (cardCombination.contains("Two pairs")) {
                    checkIfTwoPairsOnTable(card1, card2);
                } else if (cardCombination.contains("Set of")) {
                    checkForSetOnTable();
                } else if (cardCombination.contains("High card")) {
                    checkIfNoOneRaisedToPlay();
                }
            }
            catch (IOException e){
                System.out.println("River exception");
                System.out.println(e.getMessage());
            }
        }

         private void Final(Card card1, Card card2, int bigBlind){
            try {
                if (cardCombination.contains("Straight")) {
                    //check if straight is on the table
                    ArrayList<String> cardsOnTable = new ArrayList<>(5);
                    for(Card c : deskCards){
                        cardsOnTable.add(c.value);
                    }
                    if(sequentialCards(cardsOnTable) == 5){
                        connection.sendMessage(Commands.Check.toString());
                    }
                    else {
                        connection.sendMessage(Commands.Call.toString());
                    }
                } else if (cardCombination.contains("Four of")) {
                    checkForFourEqualCardsOnTable();
                } else if (cardCombination.contains("Flash")) {
                    checkForFlashOnTheTable();
                } else if (cardCombination.contains("Full house")) {
                    checkForFulHouseOnTable();
                } else if (cardCombination.contains("Pair of")) {
                    checkForPairOnTable(card1, card2, bigBlind);
                } else if (cardCombination.contains("Two pairs")) {
                    checkIfTwoPairsOnTable(card1, card2);
                } else if (cardCombination.contains("Set of")) {
                    checkForSetOnTable();
                } else if (cardCombination.contains("High card")) {
                    checkIfNoOneRaisedToPlay();
                }
            }
            catch (IOException e){
                System.out.println("final exception");
                System.out.println(e.getMessage());
            }
        }


    private ArrayList<String> allcards(){
        //list of all cards
        ArrayList<String> allcards = new ArrayList<>(13);
        allcards.add("2");
        allcards.add("3");
        allcards.add("4");
        allcards.add("5");
        allcards.add("6");
        allcards.add("7");
        allcards.add("8");
        allcards.add("9");
        allcards.add("10");
        allcards.add("J");
        allcards.add("Q");
        allcards.add("K");
        allcards.add("A");
        return allcards;
    }

    private boolean arePair(Card card1, Card card2){
        //if our cards are pair (22,33,44,55,66,...,KK,AA)
        return card1.value.equals(card2.value);
    }

    private boolean areSuit(Card card1, Card card2){
        //if our cards are same color
        return card1.suit.equals(card2.suit);
    }

    private boolean areSequential(ArrayList<String> allcards, Card card1, Card card2){
        //if our cards are sequential (45,78,10J,KA,A2);
        for (int i = 0; i < allcards.size(); i++) {
            if (i != 0 && i != allcards.size() - 1) {
                if (card1.value.equals(allcards.get(i))) {
                    if (card2.value.equals(allcards.get(i + 1)) || card2.value.equals(allcards.get(i - 1))) {
                        return true;
                    }
                }
            }
            if (i == 0) {
                if (card1.value.equals(allcards.get(i))) {
                    if (card2.value.equals(allcards.get(allcards.size() - 1))) {
                        return true;
                    }
                }
                if (card2.value.equals(allcards.get(i))) {
                    if (card1.value.equals(allcards.get(allcards.size() - 1))) {
                        return true;
                    }
                }
            }
            if (i == allcards.size() - 1) {
                if (card1.value.equals(allcards.get(i))) {
                    if (card2.value.equals(allcards.get(0))) {
                        return true;
                    }
                }
                if (card2.value.equals(allcards.get(i))) {
                    if (card1.value.equals(allcards.get(0))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean bothAreBiggerThan10(){
        //if two of our cards are bigger than 10
        int counter = 0;
        for(Card c : myPlayer().cards){
            if(c.value.equals("A") || c.value.equals("K") || c.value.equals("Q") || c.value.equals("J") || c.value.equals("10")){
                counter++;
            }
        }
        return counter == 2;
    }

    private int howMuchHasSomeoneRaised(int bigBlind){
        int otherBetRaise = 0;
        for (Player p : players) {
            if (!p.name.equals(myPlayer().name)) {
                if (p.bet > bigBlind) {
                    otherBetRaise = p.bet;
                }
            }
        }
        return otherBetRaise;
    }

    private void howMuchToRaiseDependingOnPot() throws IOException {
        if (pot * 2 >= myPlayer().balance) {
            connection.sendMessage(Commands.Check.toString());
        } else {
            connection.sendMessage(Commands.Rise.toString() + "," + pot / 4);
        }
    }

    private int playersThatAreNotFold(){
        int counter = 0;
        for(Player p : players){
            if(p.status.equalsIgnoreCase("fold")){
                counter++;
            }
        }
        return counter;
    }

    private boolean pairOnFlop(){
        ArrayList<String> cardsOnFlopValue = new ArrayList<>(3);
        for(Card c : deskCards){
            cardsOnFlopValue.add(c.value);
        }
        return cardsOnFlopValue.get(0).equals(cardsOnFlopValue.get(1)) || cardsOnFlopValue.get(0).equals(cardsOnFlopValue.get(2)) || cardsOnFlopValue.get(2).equals(cardsOnFlopValue.get(1));
    }

    private boolean pairOnTable() {
        ArrayList<String> cardsOnFlopValue = new ArrayList<>(5);
        for (Card c : deskCards) {
            cardsOnFlopValue.add(c.value);
        }
        return (cardsOnFlopValue.get(0).equals(cardsOnFlopValue.get(3)) || cardsOnFlopValue.get(0).equals(cardsOnFlopValue.get(4)) || cardsOnFlopValue.get(3).equals(cardsOnFlopValue.get(1)) || cardsOnFlopValue.get(1).equals(cardsOnFlopValue.get(4)) || cardsOnFlopValue.get(2).equals(cardsOnFlopValue.get(3)) || cardsOnFlopValue.get(2).equals(cardsOnFlopValue.get(4)) || cardsOnFlopValue.get(3).equals(cardsOnFlopValue.get(4))) && pairOnFlop();
    }

    private int checkForFlushCountCards(Card card1, Card card2){
        int suitCounterWhenWeHaveSuit = 0;
        int suitCounterForCard1 = 0;
        int suitCounterForCard2 = 0;
        for (Card c : deskCards) {
            if (areSuit(card1, card2)) {
                if (card1.suit.equals(c.suit)) {
                    suitCounterWhenWeHaveSuit++;
                }
            }
            else{
                if(card1.suit.equals(c.suit)){
                    suitCounterForCard1++;
                }
                if(card2.suit.equals(c.suit)){
                    suitCounterForCard2++;
                }
            }
        }
        if(suitCounterWhenWeHaveSuit == 0){
            if(suitCounterForCard1 > suitCounterForCard2){
                return suitCounterForCard1+1;
            }
            else{
                return suitCounterForCard2+1;
            }
        }
        else{
            return suitCounterWhenWeHaveSuit+2;
        }
    }

    private int checkForStraightCounter(Card card1, Card card2) {
        int areSequentialCounter = 0;
        for (Card c : deskCards) {
            if (areSequential(allcards(), card1, card2)) {
                for (int i = 0; i < allcards().size(); i++) {
                    if (card1.value.equals(allcards().get(i))) {
                        if (i != allcards().size() - 1) {
                            if (allcards().get(i + 1).equals(c.value))
                                areSequentialCounter++;
                        } else {
                            if (allcards().get(0).equals(c.value))
                                areSequentialCounter++;
                        }
                    }
                    if (card1.value.toString().equals(allcards().get(i))) if (i != 0) {
                        if (allcards().get(i - 1).equals(c.value))
                            areSequentialCounter++;
                        else {
                            if (allcards().get(allcards().size() - 1).equals(c.value))
                                areSequentialCounter++;
                        }
                    }
                    if (card2.value.toString().equals(allcards().get(i))) {
                        if (i != allcards().size() - 1 && allcards().get(i + 1).equals(c.value)) {
                            areSequentialCounter++;
                        } else {
                            if (allcards().get(0).equals(c.value))
                                areSequentialCounter++;
                        }
                    }
                }
            }


        }
        return areSequentialCounter+2;
    }

    private void checkStraightOrFlush(Card card1, Card card2) throws IOException {
        if (gameRound.equals("THREE_CARDS")) {
            //if you wait for 2 more cards for flush
            // if two players && A -> pay
            // else -> folds
            if (checkForFlushCountCards(card1, card2) == 3) {
                if (card1.value.equals(allcards().get(allcards().size() - 1)) || card1.value.equals(allcards().get(allcards().size() - 1))) {
                    connection.sendMessage(Commands.Call.toString());
                } else {
                    checkIfNoOneRaisedToPlay();
                }
            } else if (checkForFlushCountCards(card1, card2) == 4) {
                connection.sendMessage(Commands.Call.toString());
            } else
                //if you wait for 2 cards for straight->fold
                //wait for 1 -> call
                if (checkForStraightCounter(card1, card2) == 4) {
                    connection.sendMessage(Commands.Call.toString());
                } else {
                    checkIfNoOneRaisedToPlay();
                }
        }
        else{
            if(gameRound.equals("FOUR_CARDS")){
                if (checkForFlushCountCards(card1, card2) == 4) {
                    if (card1.value.equals(allcards().get(allcards().size() - 1)) || card1.value.equals(allcards().get(allcards().size() - 1))) {
                        connection.sendMessage(Commands.Call.toString());
                    }
                    else{
                        checkIfNoOneRaisedToPlay();
                    }
                }
                else{
                    if (checkForStraightCounter(card1, card2) == 4) {
                        connection.sendMessage(Commands.Call.toString());
                    } else {
                        checkIfNoOneRaisedToPlay();
                    }
                }
            }
            else{
                if(gameRound.equals("FIVE_CARDS")){
                    checkIfNoOneRaisedToPlay();
                }
            }
        }
    }

    private void checkForFourEqualCardsOnTable(){
        try {
            if(gameRound.equals("FOUR_CARDS")) {
                ArrayList<String> cardsOnDesk = new ArrayList<>(4);
                for (Card c : deskCards) {
                    cardsOnDesk.add(c.value);
                }
                if (cardsOnDesk.get(0).equals(cardsOnDesk.get(1)) && cardsOnDesk.get(1).equals(cardsOnDesk.get(2)) && cardsOnDesk.get(2).equals(cardsOnDesk.get(3))) {
                    checkIfNoOneRaisedToPlay();
                } else {
                    connection.sendMessage(Commands.AllIn.toString());
                }
            }
            if(gameRound.equals("FIVE_CARDS")){
                ArrayList<String> cardsOnDesk = new ArrayList<>(5);
                for (Card c : deskCards) {
                    cardsOnDesk.add(c.value);
                }
                if (cardsOnDesk.get(0).equals(cardsOnDesk.get(1)) && cardsOnDesk.get(1).equals(cardsOnDesk.get(2)) && cardsOnDesk.get(2).equals(cardsOnDesk.get(3)) && cardsOnDesk.get(3).equals(cardsOnDesk.get(4))) {
                    checkIfNoOneRaisedToPlay();
                } else {
                    connection.sendMessage(Commands.AllIn.toString());
                }
            }
        }
        catch(IOException e){
            System.out.println("checkForFourEqualCardsOnTable");
            System.out.println(e.getMessage());
        }
    }

    private void checkForFlashOnTheTable(){
        try {
            ArrayList<String> cardsOnDesk = new ArrayList<>(5);
            for (Card c : deskCards) {
                cardsOnDesk.add(c.suit);
            }
            if (cardsOnDesk.get(0).equals(cardsOnDesk.get(1)) && cardsOnDesk.get(1).equals(cardsOnDesk.get(2)) && cardsOnDesk.get(2).equals(cardsOnDesk.get(3)) && cardsOnDesk.get(3).equals(cardsOnDesk.get(4))) {
                checkIfNoOneRaisedToPlay();
            } else {
                connection.sendMessage(Commands.AllIn.toString());
            }
        }
        catch(IOException e){
            System.out.println("checkForFlashOnTheTable");
            System.out.println(e.getMessage());
        }
    }

    private void checkForSetOnTable(){
        try {
            if(gameRound.equals("FOUR_CARDS")) {
                ArrayList<String> cardsOnDesk = new ArrayList<>(4);
                for (Card c : deskCards) {
                    cardsOnDesk.add(c.value);
                }
                if ((cardsOnDesk.get(0).equals(cardsOnDesk.get(1)) && cardsOnDesk.get(0).equals(cardsOnDesk.get(2))) || (cardsOnDesk.get(0).equals(cardsOnDesk.get(1)) && cardsOnDesk.get(0).equals(cardsOnDesk.get(3))) || (cardsOnDesk.get(0).equals(cardsOnDesk.get(2)) && cardsOnDesk.get(0).equals(cardsOnDesk.get(3))) || (cardsOnDesk.get(1).equals(cardsOnDesk.get(2)) && cardsOnDesk.get(2).equals(cardsOnDesk.get(3)))) {
                    checkIfNoOneRaisedToPlay();
                } else {
                    connection.sendMessage(Commands.AllIn.toString());
                }
            }
            if(gameRound.equals("FIVE_CARDS")) {
                //TODO
            }
        }
        catch(IOException e){
            System.out.println("checkForSetOnTable");
            System.out.println(e.getMessage());
        }
    }

    private void checkIfTwoPairsOnTable(Card card1, Card card2){
        try {
            if(arePair(card1, card2)){
                connection.sendMessage(Commands.Rise.toString() + "," + pot/2);
            }
            else{
                int counter = 0;
                for(Card c : deskCards){
                    if(card1.value.equals(c.value)){
                        counter++;
                    }
                    if(card2.value.equals(c.value)){
                        counter++;
                    }
                }
                if(counter == 2){
                    connection.sendMessage(Commands.AllIn.toString());
                }
                else{
                    checkIfNoOneRaisedToPlay();
                }
            }
        }
        catch(IOException e){
            System.out.println("checkIfTwoPairsOnTable");
            System.out.println(e.getMessage());
        }
    }

    private void checkForPairOnTable(Card card1, Card card2, int bigBlind){
        try {
            ArrayList<String> valuesOnTable = new ArrayList<>(5);
            for(Card c : deskCards){
                valuesOnTable.add(c.value);
            }
            if (arePair(card1, card2)) {
                //check if our pair is higher than all cards on table
                int ourCardIndex = 0;
                int card1Index = 0;
                int card2Index = 0;
                int card3Index = 0;
                int card4Index = 0;
                int card5Index = 0;
                for(int i = 0; i < allcards().size(); i++){
                    if(card1.value.equals(allcards().get(i))){
                        ourCardIndex = i;
                    }
                    if(valuesOnTable.get(0).equals(allcards().get(i))){
                        card1Index = i;
                    }
                    if(valuesOnTable.get(1).equals(allcards().get(i))){
                        card2Index = i;
                    }
                    if(valuesOnTable.get(2).equals(allcards().get(i))){
                        card3Index = i;
                    }
                    if(valuesOnTable.get(3).equals(allcards().get(i))){
                        card4Index = i;
                    }
                    if(gameRound.equals("FIVE_CARDS")){
                        card5Index = i;
                    }
                }
                if(gameRound.equals("FOUR_CARDS")) {
                    if (ourCardIndex > card1Index && ourCardIndex > card2Index && ourCardIndex > card3Index && ourCardIndex > card4Index) {
                        connection.sendMessage(Commands.Rise.toString() + "," + pot / 2);
                    } else {

                        if (howMuchHasSomeoneRaised(bigBlind) >= myPlayer().balance) {
                            connection.sendMessage(Commands.Call.toString());
                        } else {
                            if (pot * 3 >= myPlayer().balance) {
                                connection.sendMessage(Commands.AllIn.toString());
                            } else {
                                connection.sendMessage(Commands.Rise.toString() + "," + pot / 2);
                            }
                        }
                    }
                }
                if(gameRound.equals("FIVE_CARDS")) {
                    if (ourCardIndex > card1Index && ourCardIndex > card2Index && ourCardIndex > card3Index && ourCardIndex > card4Index && ourCardIndex > card5Index) {
                        connection.sendMessage(Commands.Rise.toString() + "," + pot / 2);
                    } else {
                        connection.sendMessage(Commands.Call.toString());
                    }
                }
            } else {
                //if pair on table
                //if one card in our hand
                connection.sendMessage(Commands.Rise.toString() + "," + pot/3);
            }
        }
        catch(IOException e){
            System.out.println("checkForPairOnTable");
            System.out.println(e.getMessage());
        }
    }

    private void checkForFulHouseOnTable() throws IOException {
        ArrayList<String> cardsOnDesk = new ArrayList<>(5);
        for (Card c : deskCards) {
            cardsOnDesk.add(c.value);
        }
        if (((cardsOnDesk.get(0).equals(cardsOnDesk.get(1)) && cardsOnDesk.get(0).equals(cardsOnDesk.get(2))) || (cardsOnDesk.get(0).equals(cardsOnDesk.get(1)) && cardsOnDesk.get(0).equals(cardsOnDesk.get(3))) || (cardsOnDesk.get(0).equals(cardsOnDesk.get(2)) && cardsOnDesk.get(0).equals(cardsOnDesk.get(3))) || (cardsOnDesk.get(1).equals(cardsOnDesk.get(2)) && cardsOnDesk.get(2).equals(cardsOnDesk.get(3))) || (cardsOnDesk.get(0).equals(cardsOnDesk.get(3)) && cardsOnDesk.get(0).equals(cardsOnDesk.get(4))) || (cardsOnDesk.get(1).equals(cardsOnDesk.get(3)) && cardsOnDesk.get(4).equals(cardsOnDesk.get(3))) || (cardsOnDesk.get(3).equals(cardsOnDesk.get(2)) && cardsOnDesk.get(2).equals(cardsOnDesk.get(4))) || (cardsOnDesk.get(0).equals(cardsOnDesk.get(1)) && cardsOnDesk.get(1).equals(cardsOnDesk.get(4)))) && pairOnFlop()) {
            if(pairOnTable()) {
                checkIfNoOneRaisedToPlay();
            }
        } else {
            connection.sendMessage(Commands.AllIn.toString());
        }
    }

    private void checkIfNoOneRaisedToPlay() throws IOException {
        //TODO
//        boolean noOneRaised = false;
//        if(event.contains("All in") || event.contains("bet")){
//            noOneRaised = true;
//        }
//        if(!noOneRaised){
//            connection.sendMessage(Commands.Call.toString());
//        }
//        else {
//            connection.sendMessage(Commands.Check.toString());
//        }
        connection.sendMessage(Commands.Check.toString());
    }

    private static List<String> cardsOrder = Arrays.asList("A","2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A");
    //returns sorted list of sequential card indexes
    public static int sequentialCards(List<String> cards){
        boolean isSequential = false;
        int countConsequalCards = 0;
        ArrayList<Integer> sequentialCardIndexes = new ArrayList<>();

        for(int i = 0; i < cards.size()-1; i++){
            int card1Index = cardsOrder.indexOf(cards.get(i));

            // System.out.println("i: "+i+"\ncard 1: " + card1Index);

            for(int j = i+1; j < cards.size(); j++){
                int card2Index = cardsOrder.indexOf(cards.get(j));

                //    System.out.println("card 2: " + card2Index);

                if(card1Index == card2Index) {
                    continue;
                }

                else if(card1Index == 0 || card2Index == 0) {
                    if (card1Index == 0) {
                        card1Index = aceCalculation(card1Index, card2Index);
                    }
                    else if(card2Index == 0) {
                        card2Index = aceCalculation(card1Index, card2Index);
                    }
                    if(!sequentialCardIndexes.contains(card1Index) && card1Index != -1) {
                        sequentialCardIndexes.add(card1Index);
                    }
                    if(!sequentialCardIndexes.contains(card2Index) && card2Index != -1) {
                        sequentialCardIndexes.add(card2Index);
                    }
                }

                else  if(card1Index-card1Index == 1 || card1Index-card2Index == -1) {
                    if(!sequentialCardIndexes.contains(card1Index)) {
                        sequentialCardIndexes.add(card1Index);
                    }
                    if(!sequentialCardIndexes.contains(card2Index)) {
                        sequentialCardIndexes.add(card2Index);
                    }

                }

            }
        }


        Collections.sort(sequentialCardIndexes);
        System.out.println("Sequential Card Indexes: ");
        System.out.println(sequentialCardIndexes);
        return sequentialListOfIndexes(sequentialCardIndexes).size();
    }

    // parameter of this method is list of card indexes, it returns the longest sequence of card indexes, as an arrayList
    public static ArrayList<Integer> longestSequentialCardIndexes(ArrayList<Integer> cardIntexes){
        ArrayList<ArrayList<Integer>> matrixOfIndexes = new ArrayList<>();
        int startIndex = 0;
        int endIndex = 0;
        for(int i = 0; i < cardIntexes.size()-1; i ++){

            if(i == cardIntexes.size()-2 || cardIntexes.get(i+1) - cardIntexes.get(i) != 1){
                ArrayList<Integer> temp = new ArrayList<>();
                temp.add(startIndex);
                if(i == cardIntexes.size()-2){
                    endIndex= cardIntexes.size()-1;
                }
                temp.add(endIndex);
                temp.add(endIndex-startIndex+1);

                matrixOfIndexes.add(temp);
                startIndex = i+1;
            }
            else  if(cardIntexes.get(i+1) - cardIntexes.get(i) == 1){
                endIndex = i+1;
            }


        }

        ArrayList<Integer> biggestSequel = new ArrayList<>();
        ArrayList<Integer> biggestSequelIndexes = biggestSequence(matrixOfIndexes);
        if(!biggestSequelIndexes.isEmpty()) {
            for (int i = biggestSequelIndexes.get(0); i <= biggestSequelIndexes.get(1); i++) {
                biggestSequel.add(cardIntexes.get(i));
            }
        }
        return biggestSequel;
    }


    //calculates on which indexes the cards have greater sequence  { (1,3,2) } -> {(startIdx, endIdx, startIdx-endIdx+1)}
    private static ArrayList<Integer> biggestSequence(ArrayList<ArrayList<Integer>> values){
        int maxElement = 0;
        int index = -1;


        for(int i = 0; i < values.size(); i ++){
            int startEndCalculation = values.get(i).get(2);
            if(startEndCalculation == 1){
                continue;
            }
            if(startEndCalculation > maxElement){
                maxElement = startEndCalculation;
                index = i;
            }
        }

        if(index == -1){
            return new ArrayList<>();
        }
        return values.get(index);
    }


    private static ArrayList<Integer> sequentialListOfIndexes(ArrayList<Integer> cardIndexes){
        Collections.sort(cardIndexes);
        ArrayList<Integer> newList = new ArrayList<>();

        for(int i = 0; i < cardIndexes.size()-1; i++){
            int cardIndexCalculation = cardIndexes.get(i) - cardIndexes.get(i+1);
            if(cardIndexCalculation == 1 || cardIndexCalculation == -1){
                if(!newList.contains(cardIndexes.get(i))) {
                    newList.add(cardIndexes.get(i));
                }
                if(!newList.contains(cardIndexes.get(i+1))) {
                    newList.add(cardIndexes.get(i + 1));
                }
            }
        }

        System.out.println("****************************");
        System.out.println(longestSequentialCardIndexes(newList));
        return longestSequentialCardIndexes(newList);

    }


    //returns true if ace in the sequence
    private static int aceCalculation(int card1, int card2){
        int aceAtBeginning = card1 - card2;
        if(card1 == 0 ) {
            int aceAtEnd = cardsOrder.size()-1 - card2;
            if (aceAtBeginning == 1 || aceAtBeginning == -1) {
                return 0;
            }
            else if(aceAtEnd == 1){
                return cardsOrder.size()-1;
            }
        }
        if(card2 == 0 ) {
            int aceAtEnd = cardsOrder.size()-1 - card1;
            if (aceAtBeginning == 1 || aceAtBeginning == -1) {
                return 0;
            }
            else if(aceAtEnd == 1 || aceAtEnd == -1){
                return cardsOrder.size()-1;
            }
        }
        return -1;
    }

}
