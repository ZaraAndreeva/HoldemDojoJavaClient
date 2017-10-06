package com.nedogeek;


import org.eclipse.jetty.websocket.*;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.util.resources.cldr.rwk.CalendarData_rwk_TZ;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class Client {
    private static final String userName = "Zara";
    private static final String password = "somePassword";

    private static final String SERVER = "ws://10.22.40.111:8080/ws";
    private org.eclipse.jetty.websocket.WebSocket.Connection connection;

    enum Commands {
        Check, Call, Rise, Fold, AllIn
    }

    class Card {
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

    class Player {

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

    List<Card> deskCards;

    int pot;
    String gameRound;

    String dealer;
    String mover;
    List<String> event;
    List<Player> players;

    String cardCombination;

    public Client() {
        con();
    }

    public static void main(String[] args) {
        new Client();
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















































    boolean hasEveryonesTurnBeen = false;

    private void doAnswer(String message) throws IOException {

        //my player's cards
        int counter = 0;
        Card card1 = null;
        Card card2 = null;
        for (Card c : myPlayer().cards) {
            if (counter == 0) {
                card1 = c;
            } else {
                card2 = c;
                break;
            }
            counter++;
        }

        if (gameRound.equals("BLIND")) {
            PreFlop(card1, card2);
        }

        if (gameRound.equals("THREE_CARDS")) {
           Flop(card1, card2);
        }

        if (gameRound.equals("FOUR_CARDS")) {
            Turn();
        }

        if (gameRound.equals("FIVE_CARDS")) {
            River();
        }
        if (gameRound.equals("FINAL")) {
            if (cardCombination.contains("Pair of")) {
                //TODO
                connection.sendMessage(Commands.Rise.toString() + ",50");
            } else if (cardCombination.contains("Two pairs")) {
                //TODO
                connection.sendMessage(Commands.Rise.toString() + ",50");
            } else if (cardCombination.contains("Set of")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("Straight")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("Flash")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("Full house")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("High card")) {
                connection.sendMessage(Commands.Check.toString());
            }
        }

    }

    Player myPlayer() {
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

    void PreFlop(Card card1, Card card2) throws IOException {
        int smallBlind = 0;
        int bigBlind = 0;
        for (Player p : players) {
            if (p.status.equals("SmallBLind")) {
                smallBlind = p.bet;
            }
            if (p.status.equals("BigBlind")) {
                bigBlind = p.bet;
            }
        }

        if (arePair(card1, card2)) {
            boolean doWeHaveBigPair = false;
            for (int i = 10; i < allcards().size(); i++) {
                if (card1.value.equals(allcards().get(i))) {
                    doWeHaveBigPair = true;
                    //if s.o. raise -> if raise >= balance -> call else -> if raise*3 > balance -> all in -> else pot*3
                    //else -> if pot*2 > balance -> all in -> else -> rise pot*2
                    if (howMuchHasSomeoneRaised(bigBlind) != 0) {
                        hasEveryonesTurnBeen = true;
                        if (howMuchHasSomeoneRaised(bigBlind) >= myPlayer().balance) {
                            connection.sendMessage(Commands.Call.toString());
                        } else {
                            if (pot * 3 >= myPlayer().balance) {
                                connection.sendMessage(Commands.AllIn.toString());
                            } else {
                                connection.sendMessage(Commands.Rise.toString() + "," + pot * 3);
                            }
                        }
                        break;
                    } else {
                        hasEveryonesTurnBeen = true;
                        howMuchToRaiseDependingOnPot();
                        break;
                    }
                }
            }
            if (hasEveryonesTurnBeen) {
                connection.sendMessage(Commands.Call.toString());
            }
            hasEveryonesTurnBeen = false;
            //if s.o. raise -> if raise >= balance call else -> pot*2
            //else -> if pot*2 >= balance -> call else -> pot*2
            if (!doWeHaveBigPair) {
                if (howMuchHasSomeoneRaised(bigBlind) != 0) {
                    connection.sendMessage(Commands.Call.toString());
                } else {
                    howMuchToRaiseDependingOnPot();
                }
            }
        } else if (areSuit(card1, card2)) {
            if (card1.value.equals(allcards().get(allcards().size() - 1)) || card2.value.equals(allcards().get(allcards().size() - 1))) {
                //if s.o. raise -> call
                //else raise pot*2
                if (howMuchHasSomeoneRaised(bigBlind) != 0) {
                    connection.sendMessage(Commands.Call.toString());
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
                        } else {
                            howMuchToRaiseDependingOnPot();
                        }
                    }
                }
                if (!highCard) {
                    connection.sendMessage(Commands.Fold.toString());
                }
            }

        } else if (bothAreBiggerThan10()) {
            //if no one raise -> raise pot*2, else -> call
            if (howMuchHasSomeoneRaised(bigBlind) != 0) {
                connection.sendMessage(Commands.Call.toString());
            } else {
                howMuchToRaiseDependingOnPot();
            }
        } else if (areSequential(allcards(),card1,card2)) {
            //if s.o. raise -> call
            //else raise pot*2
            if (howMuchHasSomeoneRaised(bigBlind) != 0) {
                connection.sendMessage(Commands.Call.toString());
            } else {
                howMuchToRaiseDependingOnPot();
            }
        } else {
            if (myPlayer().status.equals("SmallBlind")) {
                //if someone raise -> fold, else -> call
                if (howMuchHasSomeoneRaised(bigBlind) != 0) {
                    connection.sendMessage(Commands.Fold.toString());
                } else {
                    connection.sendMessage(Commands.Call.toString());
                }
            } else if (myPlayer().status.equals("BigBlind")) {
                //if no one raise -> check else -> if raise is small -> call else -> fold
                if (howMuchHasSomeoneRaised(bigBlind) != 0 && myPlayer().balance - howMuchHasSomeoneRaised(bigBlind) > 500) {
                    connection.sendMessage(Commands.Call.toString());
                } else {
                    connection.sendMessage(Commands.Check.toString());
                }
            } else
                connection.sendMessage(Commands.Fold.toString());
        }
    }

    void Flop(Card card1, Card card2) throws IOException {
        int bigBlind = 0;
        for (Player p : players) {
            if (p.status.equals("BigBlind")) {
                bigBlind = p.bet;
            }
        }

        if (cardCombination.contains("Straight")) {
            connection.sendMessage(Commands.AllIn.toString());
        } else if (cardCombination.contains("Flash")) {
            connection.sendMessage(Commands.AllIn.toString());
        } else if (cardCombination.contains("Full house")) {
            connection.sendMessage(Commands.AllIn.toString());
        } else if (cardCombination.contains("Pair of")) {
            if(arePair(card1, card2)){
                int index = 0;
                for(int i = 0; i < allcards().size(); i++){
                   if(card1.value.equals(allcards().get(i))){
                        index = i;
                        break;
                    }
                }
                boolean higherCardsOnFlopThanMine = false;
                for(Card c : deskCards){
                    for(int i = index; i < allcards().size(); i++){
                        if(c.value.equals(allcards().get(i))){
                            higherCardsOnFlopThanMine = true;
                        }
                    }
                }
                if(higherCardsOnFlopThanMine){
                    howMuchHasSomeoneRaised(bigBlind);
                }
                else{
                    connection.sendMessage(Commands.Call.toString());
                }
            }
            else{
                if(pairOnFlop()){
                    connection.sendMessage(Commands.Check.toString());
                }
                else{
                    if(playersThatAreNotFold() < 3){
                        connection.sendMessage(Commands.Call.toString());
                    }
                    else{
                        checkStraightOrFlush(card1, card2);
                    }
                }
            }
        } else if (cardCombination.contains("Two pairs")) {
            if(pairOnFlop()){
                if(playersThatAreNotFold() < 3){
                    howMuchToRaiseDependingOnPot();
                }
                else{
                    connection.sendMessage(Commands.Call.toString());
                }
            }
            else{
                if(!arePair(card1, card2)){
                    connection.sendMessage(Commands.AllIn.toString());
                }
            }
            connection.sendMessage(Commands.Rise.toString() + ",50");
        } else if (cardCombination.contains("Set of")) {
            ArrayList<String> cardsOnFlopValue = new ArrayList<>(3);
            for(Card c : deskCards){
                cardsOnFlopValue.add(c.value);
            }
            if(cardsOnFlopValue.get(0).equals(cardsOnFlopValue.get(1)) && cardsOnFlopValue.get(0).equals(cardsOnFlopValue.get(2))){
                if(card1.value.equals(allcards().get(allcards().size()-1)) || card2.value.equals(allcards().get(allcards().size()-1))){
                    connection.sendMessage(Commands.Call.toString());
                }
                else{
                    checkStraightOrFlush(card1, card2);
                }
            }
            else{
                if(arePair(card1, card2)){
                    connection.sendMessage(Commands.AllIn.toString());
                }
                else{
                    if(card1.value.equals(allcards().get(allcards().size()-1)) || card2.value.equals(allcards().get(allcards().size()-1)) || card1.value.equals(allcards().get(allcards().size()-2)) || card2.value.equals(allcards().get(allcards().size()-2)) || card1.value.equals(allcards().get(allcards().size()-3)) || card2.value.equals(allcards().get(allcards().size()-3))){
                        connection.sendMessage(Commands.Rise.toString()+","+ pot/0.5);
                    }
                    else{
                        connection.sendMessage(Commands.Call.toString());
                    }
                }
            }
        } else  if (cardCombination.contains("High card")) {
                    checkStraightOrFlush(card1, card2);
            }
        }

        void Turn() throws IOException {
            if (cardCombination.contains("Straight")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("Flash")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("Full house")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("Pair of")) {
                //TODO
                connection.sendMessage(Commands.Rise.toString() + ",50");
            } else if (cardCombination.contains("Two pairs")) {
                //TODO
                connection.sendMessage(Commands.Rise.toString() + ",50");
            } else if (cardCombination.contains("Set of")) {
                //TODO
                connection.sendMessage(Commands.AllIn.toString());
            } else  if (cardCombination.contains("High card")) {
                //TODO
                connection.sendMessage(Commands.Check.toString());
            }
        }

        void River() throws IOException {
            if (cardCombination.contains("Pair of")) {
                //TODO
                connection.sendMessage(Commands.Rise.toString() + ",50");
            } else if (cardCombination.contains("Two pairs")) {
                //TODO
                connection.sendMessage(Commands.Rise.toString() + ",50");
            } else if (cardCombination.contains("Set of")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("Straight")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("Flash")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("Full house")) {
                connection.sendMessage(Commands.AllIn.toString());
            } else if (cardCombination.contains("High card")) {
                connection.sendMessage(Commands.Check.toString());
            }
        }


    ArrayList<String> allcards(){
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

    boolean arePair(Card card1, Card card2){
        //if our cards are pair (22,33,44,55,66,...,KK,AA)
        if (card1.value.equals(card2.value)) {
            return true;
        }
        return false;
    }

    boolean areSuit(Card card1, Card card2){
        //if our cards are same color
        if (card1.suit.equals(card2.suit)) {
            return true;
        }
        return false;
    }

    boolean areSequential(ArrayList<String> allcards, Card card1, Card card2){
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

    boolean bothAreBiggerThan10(){
        //if two of our cards are bigger than 10
        int counter = 0;
        for(Card c : myPlayer().cards){
            if(c.value.equals("A") || c.value.equals("K") || c.value.equals("Q") || c.value.equals("J") || c.value.equals("10")){
                counter++;
            }
        }
        if(counter == 2){
            return true;
        }
        return false;
    }

    int howMuchHasSomeoneRaised(int bigBlind){
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

    void howMuchToRaiseDependingOnPot() throws IOException {
        if (pot * 2 >= myPlayer().balance) {
            connection.sendMessage(Commands.Call.toString());
        } else {
            connection.sendMessage(Commands.Rise.toString() + "," + pot * 2);
        }
    }

    int playersThatAreNotFold(){
        int counter = 0;
        for(Player p : players){
            if(p.status.equalsIgnoreCase("fold")){
                counter++;
            }
        }
        return counter;
    }

    boolean pairOnFlop(){
        ArrayList<String> cardsOnFlopValue = new ArrayList<>(3);
        for(Card c : deskCards){
            cardsOnFlopValue.add(c.value);
        }
        if(cardsOnFlopValue.get(0).equals(cardsOnFlopValue.get(1)) || cardsOnFlopValue.get(0).equals(cardsOnFlopValue.get(2)) || cardsOnFlopValue.get(2).equals(cardsOnFlopValue.get(1))){
           return true;
        }
        else{
            return false;
        }
    }

    int checkForFlushCountCards(Card card1, Card card2){
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

    int checkForStraightCounter(Card card1, Card card2) {
        int areSequentialCounter = 0;
        for (Card c : deskCards) {
            if (areSequential(allcards(), card1, card2)) {
                for (int i = 0; i < allcards().size(); i++) {
                    if (card1.value.toString().equals(allcards().get(i))) {
                        if (i != allcards().size() - 1) {
                            if (allcards().get(i + 1).equals(c.value)) {
                                areSequentialCounter++;
                            }
                        } else {
                            if (allcards().get(0).equals(c.value)) {
                                areSequentialCounter++;
                            }
                        }
                    }
                    if (card2.value.toString().equals(allcards().get(i))) {
                        if (i != allcards().size() - 1) {
                            if (allcards().get(i + 1).equals(c.value)) {
                                areSequentialCounter++;
                            }
                        } else {
                            if (allcards().get(0).equals(c.value)) {
                                areSequentialCounter++;
                            }
                        }
                    }
                    if (card1.value.toString().equals(allcards().get(i))) {
                        if (i != 0) {
                            if (allcards().get(i - 1).equals(c.value)) {
                                areSequentialCounter++;
                            }
                        } else {
                            if (allcards().get(allcards().size() - 1).equals(c.value)) {
                                areSequentialCounter++;
                            }
                        }
                    }
                    if (card2.value.toString().equals(allcards().get(i))) {
                        if (i != 0) {
                            if (allcards().get(i - 1).equals(c.value)) {
                                areSequentialCounter++;
                            }
                        } else {
                            if (allcards().get(allcards().size() - 1).equals(c.value)) {
                                areSequentialCounter++;
                            }
                        }
                    }
                }
            }
        }
        return areSequentialCounter+2;
    }

    public void checkStraightOrFlush(Card card1, Card card2) throws IOException {
        //if you wait for 2 more cards for flush
        // if two players && A -> pay
        // else -> folds
        if(checkForFlushCountCards(card1, card2) == 3){
            if(card1.value.equals(allcards().get(allcards().size()-1)) || card1.value.equals(allcards().get(allcards().size()-1))){
                connection.sendMessage(Commands.Call.toString());
            }
            else{
                connection.sendMessage(Commands.Fold.toString());
            }
        }
        else
        if(checkForFlushCountCards(card1, card2) == 4){
            connection.sendMessage(Commands.Call.toString());
        }
        else
            //if you wait for 2 cards for straight->fold
            //wait for 1 -> call
            if(checkForStraightCounter(card1, card2) == 2){
                connection.sendMessage(Commands.Call.toString());
            }
            else{
                connection.sendMessage(Commands.Fold.toString());
            }
    }
}
