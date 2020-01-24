import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

/**
 * 
 * Back-end class for the Java simulation of a search engine
 * 
 * @author Alex Feaser 2019
 *
 */
class Indexer implements UsesStemmer {

	Set<String> stopwords;
	Map<String, WordStats> corpusWords;
	int corpusN;

	/**
	 * Creates a new {@link Indexer} object
	 */
	Indexer() {
		corpusWords = new HashMap<>();
		createStopWordsSet();
		processCorpus();
	}
	
	/**
	 * Reads up to 2^14 characters from the file as a single string.  That string is then sanitized and split into words using regex.
	 * Each word in the corpus is mapped to a {@link WordStats} object, which contains a map of the {@link Document}s which contain it and  
	 * each of its locations within that {@link Document}'s sanitized string.
	 * 
	 * @param filename
	 */
	private void processDocument(String filename) {
		Document doc = new Document(filename, new File(filename).getName());
		String rawSource = readFileAsString(doc.path);
		final String source = rawSource.substring(0, Math.min(rawSource.length(), 16384)).toLowerCase().replaceAll("\\W+", " ");
		int pos[] = {0, 0};
		Stream.of(source.split(" ")).forEach(word -> {
			String w = word.trim().toLowerCase();
			if (!w.equals("") && !stopwords.contains(w) && !w.matches("\\d+")) {
				pos[0] += source.substring(pos[0]).indexOf(w);
				w = Stemmer.stem(w);
				WordStats stats = corpusWords.containsKey(w) ? corpusWords.get(w) : new WordStats();
				corpusWords.put(w, stats.addLocation(doc, pos[0]));
			}
		});
	}
	
	/**
	 * Returns a stream containing all {@link Document}s within the available corpus
	 * 
	 */
	private static Stream<Document> corpus() {
		Builder<Document> builder = Stream.builder();
		Stream.of(new File("./corpus/").listFiles(folder -> folder.isDirectory()))
				.forEach(folder -> Stream.of(folder.listFiles(file -> file.getName().endsWith(".txt")))
						.forEach(file -> builder.add(new Document(file.getAbsolutePath(), file.getName()))));
		return builder.build();
	}
	

	/**
	 * Returns a String array containing the queries to test
	 * 
	 */
	private static String[] queriesToTest() {
		Builder<String> builder = Stream.builder();
		Stream.of(new File("./corpus/").listFiles(folder -> folder.isDirectory()))
				.map(folder -> folder.getName()).forEach(builder::add);
		return builder.build().toArray(String[]::new);
	}

	/**
	 * Process each {@link Document} in the corpus
	 */
	private void processCorpus() {
		List<Document> list = corpus().collect(Collectors.toList());
		corpusN = list.size();
		list.forEach(doc -> processDocument(doc.path));
	}

	/**
	 * Read the stopwords.txt file local to the project and store all words in a set
	 */
	private void createStopWordsSet() {
		stopwords = new HashSet<String>();
		try (Stream<String> stream = Files.lines(Paths.get("stopwords.txt"))) {
			stream.forEach(line -> stopwords.add(line));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a list of words based off of the words within the argument query.  The words are sanitized and only included in the list
	 * if they are non-empty, not stop words, are distinct within the query, and only if they are present somewhere within the corpus.
	 * 
	 * @param query
	 * @return
	 */
	private List<String> queryWords(String query) {
		return Stream.of(query.split("\\W+")).map(word -> word.trim().toLowerCase())
				.filter(w -> !w.equals("") && !stopwords.contains(w) && corpusWords.containsKey(Stemmer.stem(w)))
				.map(s -> Stemmer.stem(s.trim().toLowerCase())).distinct().collect(Collectors.toList());
	}

	/**
	 * Returns an ordered list of {@link Pair}s, containing the {@link Document}s and the Jaccard coefficient used to determine it's relevance
	 * to the argument query, which is calculated as follows.  First the total frequency of all query words within the corpus is determined.
	 * Then for each document which contains any of the query words, the sum of all query word occurrences is determined.  The Jaccard coefficient
	 * is the ratio of query word hits within the {@link Document} over total hits within the corpus.  Only {@link Document}s with a coefficient 
	 * of >= 0.01 are returned.
	 * 
	 * @param query
	 * @return
	 */
	public List<Pair<Document, Double>> queryCorpus(String query) {
		List<String> qwords = queryWords(query);
		final int union = qwords.stream().mapToInt(s -> corpusWords.get(s).freqC).sum();
		HashSet<Document> hits = qwords.stream().flatMap(str -> corpusWords.getOrDefault(str, new WordStats()).locations.keySet().stream()).collect(Collectors.toCollection(HashSet::new));
		final int df = hits.size();
		final double idf = Math.log10(((double)corpusN) / df);
//		System.out.format("corpusN: %d,  df:  %d%n", corpusN, df);
//		Function<Document, Pair<Document, Double>> docAndJaccardScore = doc -> new Pair<Document, Double>(doc, ((double) qwords.stream()
//				.mapToInt(str -> corpusWords.getOrDefault(str, new WordStats()).locations.getOrDefault(doc, Collections.emptyList()).size()).sum()) / union);
		Function<Document, Pair<Document, Double>> docAndTfIdfWeight = doc -> {
			double tf = ((double) qwords.stream().mapToInt(str -> corpusWords.getOrDefault(str, new WordStats()).locations.getOrDefault(doc, Collections.emptyList()).size()).sum());
			double dbl = Math.log10(1 + tf) * idf;
//			System.out.format("%s  [tf:  %.3f] = %.3f%n", doc, tf, dbl);
			return new Pair<Document, Double>(doc, dbl);
		};
		if (union == 0)
			return Collections.emptyList();
//		return hits.stream().map(docAndJaccardScore)
//				.filter(e -> e.second >= 0.01)
//				.sorted((a, b) -> Double.compare(b.second, a.second)).collect(Collectors.toList());
		return hits.stream().map(docAndTfIdfWeight)
				.filter(e -> e.second >= 0.14)
				.sorted((a, b) -> Double.compare(b.second, a.second))
//				.limit(15)
				.collect(Collectors.toList());
	}
	
	/**
	 * Returns a set of {@link Document}s which contains the Google search results for a given query
	 * 
	 * @param query
	 */
	private Set<Document> truePositives(String query) {
		return Stream.of(new File("./corpus/" + query + "/").listFiles(file -> file.getName().endsWith(".txt")))
				.map(file -> new Document(file.getAbsolutePath(), file.getName()))
				.collect(Collectors.toCollection(HashSet::new));
	}
	
	/**
	 * Reads the file at argument path fileName and returns it as a single string
	 * @param fileName
	 */
	private static String readFileAsString(String fileName) {
		try {
			return new String(Files.readAllBytes(Paths.get(fileName)));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Finds a text snippet on the fly, within {@link Document} argument doc which represents the argument query.
	 * The snippet location is found by combining and sorting the location of all query words,
	 * and has a size equal to argument snippet_len.
	 * 
	 * @param doc
	 * @param query
	 * @param snippet_len
	 * @return
	 */
	public String snippet(Document doc, String query, int snippet_len) {
		Function<String, String> format_snippet = str -> "..." + str + "...";
		String source = readFileAsString(doc.path).toLowerCase().replaceAll("\\W+", " ");
		List<Integer> positions = queryWords(query).stream().flatMap(str -> corpusWords.get(str).locations.getOrDefault(doc, Collections.emptyList()).stream()).sorted().collect(Collectors.toList());
		int pos = snippetPosition(positions, snippet_len);
		return format_snippet.apply(source.substring(pos, Math.min(source.length(), pos + snippet_len)));
	}
	
	/**
	 * Chooses and returns the position within the query which contains the most query word matches within argument window
	 * number of characters.
	 * 
	 * @param positions
	 * @param window
	 * @return
	 */
	private int snippetPosition(List<Integer> positions, int window) {
		int values[] = new int[positions.size()];
		Arrays.fill(values, 1);
		for (int i = 0; i < values.length; ++i) {
			int start = positions.get(i), j = i + 1;
			while(j < positions.size() && positions.get(j) <= start + window) {
				values[i]++;
				++j;
			}
		}
		int maxIndex = 0, max = 0;
		for (int i = 0; i < values.length; ++i) {
			if (values[i] > max) {
				max = values[i];
				maxIndex = i;
			}
		}
		return positions.get(maxIndex);
	}
	
	/**
	 * Test the accuracy of this search engine as compared to the Google query results which comprise the corpus.
	 * Iterates through each of the queries used to create the corpus and runs them through this engine.  The
	 * recall and precision of the engine's results is calculated when they are compared with the expected results.
	 * The expected results, actual results, and differences between the two are tracked for each query
	 * and the summary is output to a file.
	 */
	public void testCorpus() {
		final String[] queries = queriesToTest();
		StringBuilder sb = new StringBuilder();
		Stream.of(queries).forEach(query -> {
			Set<Document> tP = truePositives(query);
			List<Document> res = queryCorpus(query).stream().map(pair -> pair.first).collect(Collectors.toList());
			final double tPr = (double) res.stream().filter(str -> tP.contains(str)).count();
			final double recall = tPr / tP.size(), precision = tPr / res.size();
			final Consumer<Pair<Document, File>> printDoc = pair -> sb.append("    - ").append(pair.second.getName()).append(" \"").append(snippet(pair.first, query.toLowerCase(), 100)).append("\"\n");
			final Consumer<Stream<Document>> printAllDocs = stream -> stream.map(doc -> Pair.make(doc, new File(doc.path))).sorted((a, b) -> a.first.path.compareTo(b.first.path)).forEach(printDoc);
			sb.append("\n\"").append(query).append("\":\n");
			sb.append("\n  Expected Results (").append(tP.size()).append("):\n");
			printAllDocs.accept(tP.stream());
			sb.append("\n  Actual Results (").append(res.size()).append(", ").append((int)tPr).append(" true positive):\n");
			printAllDocs.accept(res.stream());
			Set<Document> diff = new HashSet<>(tP);
			res.forEach(doc -> {
				if (tP.contains(doc))
					diff.remove(doc);
				else
					diff.add(doc);
			});
			List<Document> fP = diff.stream().filter(doc -> res.contains(doc)).collect(Collectors.toList());
			List<Document> fN = diff.stream().filter(doc -> tP.contains(doc)).collect(Collectors.toList());
			sb.append("\n  False Positives (").append(fP.size()).append("):\n");
			printAllDocs.accept(fP.stream());
			sb.append("\n  False Negatives (").append(fN.size()).append("):\n");
			printAllDocs.accept(fN.stream());
			sb.append("\n  Precision: ").append(String.format("%.4f", precision)).append(",  Recall: ").append(String.format("%.4f", recall)).append("\n");
		});
		writeFile("output.txt", sb);
    	try {
			Desktop.getDesktop().edit(new File("output.txt"));
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Write a file at location filename containing the string contents of argument {@link StringBuilder}
	 * 
	 * @param sb
	 */
	private static void writeFile(String filename, StringBuilder sb) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
		    final int len = sb.length(), kb = 1024;
		    final char[] buff = new char[kb];
		    for (int i = 0; i < len; i += kb) {
				final int end = Math.min(i + kb, len);
				sb.getChars(i, end, buff, 0);
				bw.write(buff, 0, end - i);
			}
		    bw.flush();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}

	/**
	 * Execute corpus test
	 */
	public static void main(String[] args) {
		Indexer indexer = new Indexer();
		indexer.testCorpus();
	}
}