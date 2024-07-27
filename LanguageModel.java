import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LanguageModel {
    
    // The map of this model.
    // Maps windows to lists of character data objects.
    HashMap<String, List> CharDataMap;
    
    // The window length used in this model.
    int windowLength;
    
    // The random number generator used by this model. 
    private Random randomGenerator;

    /** Constructs a language model with the given window length and a given
     *  seed value. Generating texts from this model multiple times with the 
     *  same seed value will produce the same random texts. Good for debugging. */
    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    /** Constructs a language model with the given window length.
     * Generating texts from this model multiple times will produce
     * different random texts. Good for production. */
    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
    public void train(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            // Initialize the sliding window
            StringBuilder window = new StringBuilder();
            int ch;
            
            // Read characters from the file
            while ((ch = reader.read()) != -1) {
                char currentChar = (char) ch;
                
                // Add the current character to the window
                window.append(currentChar);
                
                // If the window is longer than the desired length, slide it
                if (window.length() > windowLength) {
                    String windowStr = window.substring(0, windowLength);
                    char nextChar = window.charAt(windowLength);
                    
                    // Update the map with the next character after the window
                    List list = CharDataMap.get(windowStr);
                    if (list == null) {
                        list = new List();
                        CharDataMap.put(windowStr, list);
                    }
                    list.update(nextChar);
                    
                    // Slide the window by one character
                    window.deleteCharAt(0);
                }
            }
            
            // Handle the last window if there are remaining characters
            if (window.length() == windowLength) {
                String windowStr = window.toString();
                CharDataMap.putIfAbsent(windowStr, new List());
            }
            
            // Calculate probabilities for all lists in CharDataMap
            for (Map.Entry<String, List> entry : CharDataMap.entrySet()) {
                calculateProbabilities(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Computes and sets the probabilities (p and cp fields) of all the
     * characters in the given list. */
    public static void calculateProbabilities(List probs) {
        Node current = probs.first;
        int totalCharacters = 0;

        while (current != null) {
            totalCharacters += current.cp.count;
            current = current.next;
        }

        current = probs.first;
        double cumulativeProbability = 0.0;

        while (current != null) {
            CharData charData = current.cp;
            charData.p = (double) charData.count / totalCharacters;
            cumulativeProbability += charData.p;
            charData.cp = cumulativeProbability;

            current = current.next;
        }
    }

    /** Returns a random character from the given probabilities list. */
    public char getRandomChar(List probs) {
        // Generate a random number between 0 and 1
        double randomValue = randomGenerator.nextDouble();

        // Traverse the list to find the character corresponding to the random number
        Node current = probs.first;
        while (current != null) {
            if (randomValue < current.cp.cp) {
                return current.cp.chr;
            }
            current = current.next;
        }

        // This should not happen if probabilities are correctly computed
        throw new IllegalStateException("No character found for the generated random value.");
    }

    /** Generates a random text, based on the probabilities that were learned during training. */
    public String generate(String initialText, int textLength) {
        if (initialText.length() < windowLength) {
            return initialText;
        }

        StringBuilder generatedText = new StringBuilder(initialText);
        String window = initialText.substring(initialText.length() - windowLength);

        while (generatedText.length() < textLength) {
            List list = CharDataMap.get(window);
            if (list == null) {
                break; // Stop if the current window is not found in the map
            }
            char nextChar = getRandomChar(list);
            generatedText.append(nextChar);
            window = generatedText.substring(generatedText.length() - windowLength);
        }

        return generatedText.toString();
    }

    /** Returns a string representing the map of this language model. */
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String key : CharDataMap.keySet()) {
            List keyProbs = CharDataMap.get(key);
            str.append(key).append(" : ").append(keyProbs).append("\n");
        }
        return str.toString();
    }

    public static void main(String[] args) {

        if (args.length != 5) {
            System.out.println("Usage: java LanguageModel <windowLength> <initialText> <generatedTextLength> <random/fixed> <fileName>");
            return;
        }

        int windowLength = Integer.parseInt(args[0]);
       String initialText = args[1];
       int generatedTextLength = Integer.parseInt(args[2]);
       Boolean randomGeneration = args[3].equals("random");
       String fileName = args[4];
       // Create the LanguageModel object
       LanguageModel lm;
       if (randomGeneration)
           lm = new LanguageModel(windowLength);
       else
           lm = new LanguageModel(windowLength, 20);
       // Trains the model, creating the map.
       lm.train(fileName);
       // Generates text, and prints it.
       System.out.println(lm.generate(initialText, generatedTextLength));
    }
    
}