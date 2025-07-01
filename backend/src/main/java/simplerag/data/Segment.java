package simplerag.data;

import org.commonmark.node.Document;
import org.commonmark.node.Node;
import org.commonmark.renderer.Renderer;
import org.commonmark.renderer.markdown.MarkdownRenderer;

import java.util.ArrayList;
import java.util.List;

public class Segment {
    private static final Renderer renderer = MarkdownRenderer.builder().build();

    private final String header;
    private final int level;
    private List<Node> bodyNodes;
    private String body;

    private int headerTokenNum;
    private int bodyTokenNum;

    Segment(String header, int level) {
        this.header = header;
        this.level = level;
        bodyNodes = new ArrayList<>();
    }

    Segment(Segment original, String body, int bodyTokenNum) {
        this.header = original.header;
        this.level = original.level;
        this.headerTokenNum = original.headerTokenNum;
        this.body = body;
        this.bodyTokenNum = bodyTokenNum;
    }

    void addBodyPart(Node node) {
        bodyNodes.add(node);
    }

    void endBodyPart() {
        Document tempDoc = new Document();
        for (Node n : bodyNodes) {
            tempDoc.appendChild(n);
        }
        body = renderer.render(tempDoc);
    }

    public void estimateTokenNum(TokenCounter tokenCounter) {
        headerTokenNum = tokenCounter.countTokens(header);
        bodyTokenNum = tokenCounter.countTokens(body);
    }

    public String getHeader() {
        return header;
    }

    public int getLevel() {
        return level;
    }

    public String getBody() {
        return body;
    }

    public int getHeaderTokenNum() {
        return headerTokenNum;
    }

    public int getBodyTokenNum() {
        return bodyTokenNum;
    }

    public int getTokenNum() {
        return headerTokenNum + bodyTokenNum;
    }

    public String toMarkdownStr() {
        if (level == 0) {
            return body;
        }
        return String.format("%s %s\n%s", "#".repeat(level), header, body);
    }

    public static void estimateTokenNum(List<Segment> segments, TokenCounter counter) {
        for (Segment segment : segments) {
            segment.estimateTokenNum(counter);
        }
    }

}
