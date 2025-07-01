package simplerag.data;

import java.util.ArrayList;
import java.util.List;

public record SplitChunk(String markdown,
                         int token) {

    public static SplitChunk of(List<Segment> segments) {
        List<String> result = new ArrayList<>();
        int token = 0;
        for (Segment segment : segments) {
            String str = segment.toMarkdownStr();
            if (!str.isBlank()) {
                result.add(str);
            }
            token += segment.getTokenNum();
        }
        String md = String.join("", result);
        return new SplitChunk(md, token);
    }

    public static String dump(List<SplitChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (SplitChunk chunk : chunks) {
            sb.append("-----token:").append(chunk.token).append(" (").append(chunk.markdown.length()).append(")-----\n")
                    .append(chunk.markdown).append("\n");
        }
        return sb.toString();
    }
}
