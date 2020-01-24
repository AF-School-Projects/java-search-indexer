import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * Stores all corpus statistics regarding a given word.
 * Included are its total frequency within the corpus, 
 * and a map containing the {@link Document}s and the locations
 * within those {@link Document}s where the word is found.
 * 
 * @author AlexF
 *
 */
public class WordStats {
	int freqC;
	Map<Document, List<Integer>> locations;
	WordStats() {
		freqC = 0;
		locations = new HashMap<>();
	}
	WordStats addLocation(Document doc, int pos) {
		List<Integer> list = locations.getOrDefault(doc, new ArrayList<>());
		list.add(pos);
		locations.put(doc, list);
		return inc();
	}
	private WordStats inc() { freqC++; return this;}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Corpus frequency: ").append(freqC).append("\n");
		locations.entrySet().stream().forEach(entry -> {
			sb.append("  ").append(entry.getKey().name).append(" (").append(entry.getValue().size()).append(") :");
			entry.getValue().stream().forEach(i -> sb.append(i).append(" "));
			sb.append("\n");
		});
		return sb.toString(); 
	}
}
