package org.aksw.limes.core.measures.mapper.string;

import org.aksw.limes.core.io.cache.Cache;
import org.aksw.limes.core.io.mapping.Mapping;
import org.aksw.limes.core.io.mapping.MemoryMapping;
import org.aksw.limes.core.measures.mapper.Mapper;
import org.aksw.limes.core.measures.mapper.PropertyFetcher;
import org.aksw.limes.core.measures.measure.string.SoundexMeasure;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;

import javax.sound.midi.Soundbank;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Kevin Dreßler
 */
public class SoundexMapper extends Mapper {



	static class TrieNode {

		private Map<Character, TrieNode> children;
		private List<Integer> references;

		public static TrieNode recursiveAddAll(Map<String, List<Integer>> code2References) {
			TrieNode root = new TrieNode(null);
			TrieNode.recursiveAddAll(root, code2References);
			return root;
		}

		public static void recursiveAddAll(TrieNode root, Map<String, List<Integer>> code2References) {
			for (Map.Entry<String, List<Integer>> entry : code2References.entrySet())
				TrieNode.recursiveAdd(root, entry.getKey(), entry.getValue());
		}

		private static void recursiveAdd(TrieNode node, String code, List<Integer> references) {
			if (code.length() > 1)
				TrieNode.recursiveAdd(node.addChild(code.charAt(0), null), code.substring(1), references);
			else
				node.addChild(code.charAt(0), references);
		}

		public TrieNode(List<Integer> references) {
			this.references = references;
			this.children = new HashMap<>();
		}

		public TrieNode addChild(char symbol, List<Integer> references) {
			TrieNode child;
			if (!this.children.containsKey(symbol)) {
				child = new TrieNode(references);
				this.children.put(symbol, child);
			} else {
				child = this.children.get(symbol);
			}
			return child;
		}

		public List<Integer> getReferences() {
			return this.references;
		}

		public Set<Map.Entry<Character, TrieNode>> getChildren() {
			return this.children.entrySet();
		}
	}

	static Logger logger = Logger.getLogger("LIMES");


	static class TrieSearchState {
		private int distance;
		private int position;
		private TrieNode node;

		public TrieSearchState(int distance, int position, TrieNode node) {
			this.distance = distance;
			this.position = position;
			this.node = node;
		}

		public int getDistance() {
			return distance;
		}

		public int getPosition() {
			return position;
		}

		public TrieNode getNode() {
			return node;
		}
	}

	/**
	 * Computes a mapping between a source and a target.
	 *
	 * @param source
	 *            Source cache
	 * @param target
	 *            Target cache
	 * @param sourceVar
	 *            Variable for the source dataset
	 * @param targetVar
	 *            Variable for the target dataset
	 * @param expression
	 *            Expression to process.
	 * @param threshold
	 *            Similarity threshold
	 * @return A mapping which contains links between the source instances and
	 *         the target instances
	 */
	@Override
	public Mapping getMapping(Cache source, Cache target, String sourceVar, String targetVar, String expression,
							  double threshold) {

		logger.info("Running SoundexMapper with code length " + String.valueOf(SoundexMeasure.codeLength));

		List<String> listA, listB;
		Map<String, List<Integer>> invListA, invListB;
		List<String> properties = PropertyFetcher.getProperties(expression, threshold);
		Map<String, Set<String>> sourceMap = getValueToUriMap(source, properties.get(0));
		Map<String, Set<String>> targetMap = getValueToUriMap(target, properties.get(1));
		listA = new ArrayList<>(sourceMap.keySet());
		listB = new ArrayList<>(targetMap.keySet());
        // create inverted lists (code=>index of original list)
		invListA = getInvertedList(listA);
		invListB = getInvertedList(listB);
		Deque<Triple<Integer, List<Integer>, List<Integer>>> similarityBook = new ArrayDeque<>();
		// construct trie from smaller list
		TrieNode trie = TrieNode.recursiveAddAll(invListB);
		int maxDistance = getMaxDistance(threshold);
		// iterate over other list
		for (Map.Entry<String, List<Integer>> entry : invListA.entrySet()) {
			// for each entry do trie search
			Deque<TrieSearchState> queue = new ArrayDeque<>();
			queue.add(new TrieSearchState(0, 0, trie));
			while (!queue.isEmpty()) {
				TrieSearchState current = queue.pop();
				Set<Map.Entry<Character, TrieNode>> childs = current.getNode().getChildren();
				if (childs.isEmpty() && !current.getNode().getReferences().isEmpty()) {
					similarityBook.push(new MutableTriple<>(current.getDistance(), entry.getValue(),
							current.getNode().getReferences()));
				}
				for (Map.Entry<Character, TrieNode> nodeEntry : childs) {
					if (nodeEntry.getKey().equals(entry.getKey().charAt(current.getPosition()))) {
						queue.push(new TrieSearchState(current.getDistance(), current.getPosition() + 1,
								nodeEntry.getValue()));
					} else if (current.getDistance() < maxDistance) {
						queue.push(new TrieSearchState(current.getDistance() + 1, current.getPosition() + 1,
								nodeEntry.getValue()));
					}
				}
			}
		}
		Mapping result = new MemoryMapping();
		while (!similarityBook.isEmpty()) {
			Triple<Integer, List<Integer>, List<Integer>> t = similarityBook.pop();
			for (Integer i : t.getMiddle()) {
				String a = listA.get(i);
				for (Integer j : t.getRight()) {
					String b = listB.get(j);
					for (String sourceUri : sourceMap.get(a)) {
						for (String targetUri : targetMap.get(b)) {
							result.add(sourceUri, targetUri,
									(1.0d - (t.getLeft().doubleValue() / (double) SoundexMeasure.codeLength)));
						}
					}
				}
			}
		}
		return result;
	}

	private Map<String, List<Integer>> getInvertedList (List<String> list) {
        Map<String, List<Integer>> result = new HashMap<>(list.size());
        for (int i = 0, listASize = list.size(); i < listASize; i++) {
            String s = list.get(i);
            String code = SoundexMeasure.getCode(s);
            List<Integer> ref;
            if (!result.containsKey(code)) {
                ref = new LinkedList<>();
                result.put(code, ref);
            } else {
                ref = result.get(code);
            }
            ref.add(i);
        }
        return result;
    }

	public String getName() {
		return "soundex";
	}

	public double getRuntimeApproximation(int sourceSize, int targetSize, double theta, Language language) {
		return 1000d;
	}

	public double getMappingSizeApproximation(int sourceSize, int targetSize, double theta, Language language) {
		return 1000d;
	}

	private int getMaxDistance(double threshold) {
		return new Double(Math.floor(SoundexMeasure.codeLength * (1 - threshold))).intValue();
	};

}