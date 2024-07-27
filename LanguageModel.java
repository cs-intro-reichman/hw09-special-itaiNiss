import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

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
        StringBuilder window = new StringBuilder();
        char c;

        try (Scanner in = new Scanner(new File(fileName))) {
            in.useDelimiter("");

            // Reads just enough characters to form the first window
            for (int i = 0; i < windowLength && in.hasNext(); i++) {
                window.append(in.next());
            }

            while (in.hasNext()) {
                // Gets the next character
                c = in.next().charAt(0);

                // Checks if the window is already in the map
                List<CharData> probs = CharDataMap.get(window.toString());

                // If the window was not found in the map
                if (probs == null) {
                    // Creates a new empty list, and adds (window,list) to the map
                    probs = new LinkedList<>();
                    CharDataMap.put(window.toString(), probs);
                }

                // Calculates the counts of the current character.
                updateProbs(probs, c);

                // Advances the window: adds c to the window’s end, and deletes the window's first character.
                window.append(c);
                if (window.length() > windowLength) {
                    window.deleteCharAt(0);
                }
            }

            // The entire file has been processed, and all the characters have been counted.
            // Proceeds to compute and set the p and cp fields of all the CharData objects in each linked list in the map.
            for (List<CharData> probs : CharDataMap.values()) {
                calculateProbabilities(probs);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    // public void train(String corpus) {
    //     StringBuilder window = new StringBuilder();
    //     char c;
    //     int corpusLength = corpus.length();
    //     int i = 0;    
    //     // Reads just enough characters to form the first window
    //     for (int j = 0; j < windowLength; j++) {
    //         if (i < corpusLength) {
    //             window.append(corpus.charAt(i++));
    //         } else {
    //             return; // Not enough characters to form the first window
    //         }
    //     }   
    //     // Processes the entire text, one character at a time
    //     while (i < corpusLength) {
    //         c = corpus.charAt(i++);
    //         // Checks if the window is already in the map
    //         List probs = CharDataMap.get(window.toString());
    //         // If the window was not found in the map
    //         if (probs == null) {
    //             // Creates a new empty list, and adds (window, list) to the map
    //             probs = new List();
    //             CharDataMap.put(window.toString(), probs);
    //         }
    //         // Calculates the counts of the current character.
    //         probs.update(c);
    //         // Advances the window: adds c to the window’s end, and deletes the window's first character.
    //         window.append(c);
    //         window.deleteCharAt(0);
    //     }  
    //     // The entire file has been processed, and all the characters have been counted.
    //     // Proceeds to compute and set the p and cp fields of all the CharData objects in each linked list in the map.
    //     for (List probs : CharDataMap.values()) {
    //         calculateProbabilities(probs);
    //     }
    // }

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
           lm = new LanguageModel(windowLength, 4);
       // Trains the model, creating the map.
       lm.train(fileName);
       // Generates text, and prints it.
       System.out.println(lm.generate(initialText, generatedTextLength));


    // String testCorpus = "you cannot teach a man anything; you can only help him find it within himself.";
    //     LanguageModel lm = new LanguageModel(2);
    //     lm.train(testCorpus);
    //     System.out.println(lm);

    //     // Stress test getRandomChar method
    //     List testList = lm.CharDataMap.get("yo"); // Choose an example window from the corpus
    //     if (testList != null) {
    //         HashMap<Character, Integer> frequencyMap = new HashMap<>();
    //         int testCount = 100000;

    //         for (int i = 0; i < testCount; i++) {
    //             char randomChar = lm.getRandomChar(testList);
    //             frequencyMap.put(randomChar, frequencyMap.getOrDefault(randomChar, 0) + 1);
    //         }

    //         System.out.println("Character frequencies after " + testCount + " trials:");
    //         for (Map.Entry<Character, Integer> entry : frequencyMap.entrySet()) {
    //             System.out.println(entry.getKey() + ": " + entry.getValue());
    //         }
    //     } else {
    //         System.out.println("Window 'yo' not found in the map.");
    //     }

    }
    
}