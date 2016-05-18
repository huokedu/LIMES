package org.aksw.limes.core.measures.mapper;

import org.aksw.limes.core.io.parser.Parser;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class PropertyFetcher {

    public static List<String> getProperties(String expression, double threshold) {
        // get property labels
        Parser p = new Parser(expression, threshold);
        return Arrays.asList(getPropertyLabel(p.getTerm1()), getPropertyLabel(p.getTerm2()));
    }

    private static String getPropertyLabel(String term) {
        String propertyLabel;
        if (term.contains(".")) {
            String split[] = term.split("\\.");
            propertyLabel = split[1];
            if (split.length >= 2) {
                for (int part = 2; part < split.length; part++) {
                    propertyLabel += "." + split[part];
                }
            }
        } else {
            propertyLabel = term;
        }
        return propertyLabel;
    }
}