package org.example.cardgame;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class HelloController {
    //where the player enters an arithmetic expression.
    @FXML
    private TextField equationInput;

    //validation messages.
    @FXML
    private TextField resultField;

    //image views used to display hand.
    @FXML
    private ImageView cardImage1;

    @FXML
    private ImageView cardImage2;

    @FXML
    private ImageView cardImage3;

    @FXML
    private ImageView cardImage4;

    //File names for the cards currently shown
    private List<String> displayedCards = Collections.emptyList();

    //Java calls this after loading the FXML.
    @FXML
    private void initialize() {
        dealCards();
    }

    //Checks that the expression uses the card values and evaluates to 24.
    @FXML
    protected void onVerifyButtonClick() {
        try {
            //Parse and evaluate the expression while recording each literal
            EquationParser parser = new EquationParser(equationInput.getText());
            double result = parser.parse();

            //Compare sorted values so the expression may use cards in any order.
            List<Integer> equationNumbers = parser.getNumbers();
            List<Integer> cardNumbers = new ArrayList<>(4);
            for (String card : displayedCards)
            {
                cardNumbers.add(cardValue(card));
            }
            equationNumbers.sort(Comparator.naturalOrder());
            cardNumbers.sort(Comparator.naturalOrder());

            //Check cards before checking the result.
            if (!equationNumbers.equals(cardNumbers))
            {
                resultField.setText("Error: Card mismatch");
            }
            else if (Math.abs(result - 24.0) > 0.00000000001)
            {
                resultField.setText("Error: Number mismatch (must be 24)");
            }
            else
            {
                resultField.setText("Congrats!");
            }
        }
        catch (IllegalArgumentException exception)
        {
            resultField.setText("Error: Invalid equation");
        }
    }

    //replaces the hand when the Refresh button is clicked
    @FXML
    private void onRefreshButtonClick()
    {
        dealCards();
    }

    //deals four cards without reusing any card from the previous hand.
    private void dealCards()
    {
        List<String> availableCards = createCardList();
        availableCards.removeAll(displayedCards);
        Collections.shuffle(availableCards);

        displayedCards = new ArrayList<>(availableCards.subList(0, 4));
        cardImage1.setImage(loadCard(displayedCards.get(0)));
        cardImage2.setImage(loadCard(displayedCards.get(1)));
        cardImage3.setImage(loadCard(displayedCards.get(2)));
        cardImage4.setImage(loadCard(displayedCards.get(3)));
    }

    //builds filenames for the 52 rank-and-suit combinations in the resource folder.
    private List<String> createCardList()
    {
        List<String> cards = new ArrayList<>(52);
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "jack", "queen", "king", "ace"};
        String[] suits = {"clubs", "diamonds", "hearts", "spades"};
        //Copilot suggested using two arrays and adding them to get the cards
        for (String rank : ranks)
        {
            for (String suit : suits)
            {
                cards.add(rank + "_of_" + suit + ".png");
            }
        }

        return cards;
    }

    //Loads a card image from the folder
    private Image loadCard(String fileName)
    {
        URL imageUrl = Objects.requireNonNull(
                HelloController.class.getResource("/org/example/cardgame/" + fileName), "Missing card image: " + fileName);
        return new Image(imageUrl.toExternalForm());
    }

    //Maps card filename's rank to its the game value.
    private int cardValue(String fileName) {
        String rank = fileName.substring(0, fileName.indexOf("_of_"));
        switch (rank) {
            case "ace":
                return 1;
                //how its done in blackjack
            case "jack":
            case "queen":
            case "king":
                return 10;
            default:
                return Integer.parseInt(rank);
        }
    }


    private static final class EquationParser
    {
        //expression being parsed takes numbers and the current position.
        private final String input;
        private final List<Integer> numbers = new ArrayList<>();
        private int position;

        //Placeholder
        private EquationParser(String input)
        {
            this.input = input == null ? "" : input;
        }

        //Parses a full equation and rejects trailing input.
        private double parse()
        {
            double value = parseExpression();
            skipWhitespace();
            if (position != input.length())
            {
                throw new IllegalArgumentException("Invalid equation");
            }
            return value;
        }

        //Returns the actual numbers
        private List<Integer> getNumbers()
        {
            return numbers;
        }

        //Parses addition and subtraction uses pemdas right
        private double parseExpression()
        {
            double value = parseTerm();
            while (true)
            {
                skipWhitespace();
                if (consume('+'))
                {
                    value += parseTerm();
                }
                else if (consume('-'))
                {
                    value -= parseTerm();
                }
                else
                {
                    return value;
                }
            }
        }

        //Parses multiplication and division before addition or subtraction because pemdas
        private double parseTerm()
        {
            double value = parseFactor();
            while (true)
            {
                skipWhitespace();
                if (consume('*'))
                {
                    value *= parseFactor();
                }
                else if (consume('/'))
                {
                    //Division by zero is not allowed
                    double divisor = parseFactor();
                    if (divisor == 0.0)
                    {
                        throw new IllegalArgumentException("Division by zero");
                    }
                    value /= divisor;
                }
                else
                {
                    return value;
                }
            }
        }

        //Parses Signs, parenthesis, and integers.
        private double parseFactor()
        {
            skipWhitespace();
            if (consume('+'))
            {
                return parseFactor();
            }
            if (consume('-'))
            {
                return -parseFactor();
            }
            if (consume('('))
            {
                double value = parseExpression();
                skipWhitespace();
                if (!consume(')'))
                {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                return value;
            }

            //Read the next integer and keep it for card matching.
            skipWhitespace();
            int start = position;
            while (position < input.length() && Character.isDigit(input.charAt(position)))
            {
                position++;
            }
            if (start == position)
            {
                throw new IllegalArgumentException("Expected a number");
            }

            int number;
            try
            {
                number = Integer.parseInt(input.substring(start, position));
            }
            catch (NumberFormatException exception)
            {
                throw new IllegalArgumentException("Number is too large", exception);
            }
            numbers.add(number);
            return number;
        }

        //Advances past the expected character when it is next in the input.
        private boolean consume(char expected)
        {
            if (position < input.length() && input.charAt(position) == expected)
            {
                position++;
                return true;
            }
            return false;
        }

        //Ignores whitespace.
        private void skipWhitespace()
        {
            while (position < input.length() && Character.isWhitespace(input.charAt(position)))
            {
                position++;
            }
        }
    }
}
