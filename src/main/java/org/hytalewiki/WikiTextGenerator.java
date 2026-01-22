package org.hytalewiki;

import au.ellie.hyui.builders.UIElementBuilder;
import com.lucaskjaerozhang.wikitext_parser.WikiTextParser;
import com.lucaskjaerozhang.wikitext_parser.ast.base.WikiTextNode;
import org.hytalewiki.ast.HytaleWikiVisitor;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiTextGenerator {

    private static final Pattern WIKILINK_PATTERN = Pattern.compile("\\[\\[(.+?)\\|(.+?)]]");

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{.+?}}", Pattern.DOTALL);

    private static final Pattern WIKILINK_IN_SECTION_HEADER_PATTERN = Pattern.compile("(=+?) \\[\\[(.+)]] (=+)");

    private final HytaleWikiVisitor visitor = new HytaleWikiVisitor();

    public WikiTextGenerator() {
        //
    }

    public String sanitize(String source) {

        StringBuilder rebuilt = new StringBuilder();

        for (byte b : source.getBytes(StandardCharsets.UTF_8)) {
            if (b >= 32) {
                rebuilt.append((char) b);
            } else if (b == 10) {
                rebuilt.append('\n');
            } else {
                rebuilt.append(' ');
            }
        }

        source = rebuilt.toString().trim();

        // trim templates
        Matcher templateMatcher = TEMPLATE_PATTERN.matcher(source);
        source = templateMatcher.replaceAll("");

        Matcher wikilinkSectionHeaderMatcher = WIKILINK_IN_SECTION_HEADER_PATTERN.matcher(source);
        source = wikilinkSectionHeaderMatcher.replaceAll("$1 $2 $3");

        Matcher wikilinkMatcher = WIKILINK_PATTERN.matcher(source);

        // do some random bull-shittery to get rid of pipes outside wikilinks
        return wikilinkMatcher.replaceAll("[[$1!!!!$2]]")
                .replace("|", "")
                .replace("!!!!", "|")
                // remove categories
                .replaceAll("\\[\\[Category:.+]]", "")
                // cap newline sequences at just 2
                .replaceAll("\n{2,}", "\n\n")
                // cap spaces at 1
                .replaceAll(" +", " ")
                // the parser doesn't like underscores
                .replace("_", " ");
    }

    public UIElementBuilder<?> generate(String source) {
        String cleaned = sanitize(source);

        WikiTextNode root = (WikiTextNode) WikiTextParser.parse(cleaned);

        this.visitor.reset();

        return root.accept(this.visitor).orElse(null);
    }
}
