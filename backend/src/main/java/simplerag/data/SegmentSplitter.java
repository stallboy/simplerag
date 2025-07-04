package simplerag.data;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SegmentSplitter {

    private final TextContentRenderer textRenderer = TextContentRenderer.builder().build();
    private final Parser parser = Parser.builder().build();

    public List<Segment> removeLinkAndSplitByHeadings(String markdownText, String title) {
        String markdown = removeImageForMarkItDownBug(markdownText);
        Node document = parser.parse(markdown);
        removeLink(document);
        List<Segment> segments = new ArrayList<>();

        Segment curSegment = new Segment(title, 0);
        segments.add(curSegment);

        Node node = document.getFirstChild();
        while (node != null) {

            if (node instanceof Heading heading) {
                String headingText = textRenderer.render(heading).trim();
                curSegment = new Segment(headingText, heading.getLevel());
                segments.add(curSegment);

            } else {
                curSegment.addBodyPart(node);
            }
            node = node.getNext();
        }

        for (Segment segment : segments) {
            segment.endBodyPart();
        }

        return segments;
    }


    public static final Pattern IMG_PATTERN = Pattern.compile("!\\[.*]\\(data:image/[^)]+\\)");

    public static String removeImageForMarkItDownBug(String markdownText) {
        Matcher matcher = IMG_PATTERN.matcher(markdownText);
        return matcher.replaceAll(""); // 移除匹配内容
    }


    public static void removeLink(Node node) {
        if (node instanceof Link || node instanceof LinkReferenceDefinition || node instanceof Image) {
            node.unlink();
            return;
        }

        Node c = node.getFirstChild();
        while (c != null) {
            Node nextChild = c.getNext();
            removeLink(c);
            c = nextChild;
        }
    }

}
