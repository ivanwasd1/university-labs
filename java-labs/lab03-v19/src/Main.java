import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Map;

public class Main {
    private static final Scanner scanner =
            new Scanner(System.in);

    public static void main(String[] args) {
        run();
    }

    private static void run() {
        System.out.println("Введите количество предложений," +
                "чьи анаграммы вы хотите посмотреть: ");

        int count = scanner.nextInt();
        scanner.nextLine();

        for (int i = 0; i < count; i++) {
            Map<String, Integer> words = inputTextAndGetSet();
            printAnagramsFromWords(words);
        }
    }

    private static Map<String, Integer> inputTextAndGetSet() {

        System.out.print("Введите текст: ");
        String text = scanner.nextLine();

        Map<String, Integer> words = new HashMap<>();

        for (String word : text.strip().split("\\P{L}+")) {
            words.merge(word, 1, Integer::sum);
        }

        return words;
    }

    private static void printAnagramsFromWords(Map<String, Integer> words) {

        Boolean atLeastOneSeen = false;
        Iterator<String> iterator = words.keySet().iterator();
        System.out.println();

        while (iterator.hasNext()) {

            String word = iterator.next();

            if (isAnagramPair(word, words)) {

                String reversed = new StringBuilder(word)
                        .reverse().toString();

                System.out.printf("%s <-> %s\n",
                        word,
                        reversed);

                iterator.remove();
                atLeastOneSeen = true;
            }
        }
        System.out.println(atLeastOneSeen ? "" : "None\n");
    }

    static boolean isAnagramPair(String word, Map<String, Integer> words) {
        String reversed = new StringBuilder(word).reverse().toString();

        return words.containsKey(reversed) &&
                (!word.equals(reversed) || words.get(word) >= 2);
    }
}