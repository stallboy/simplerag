package simplerag.data;

import simplerag.utils.TokenCounter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Splitter {
    public record SplitterConf(int segmentTriggerSplitLength,
                               int segmentBestLength,
                               int splitBestMin,
                               int splitBestMax) {
    }


    public record SplitterChunk(String markdown,
                                int token) {

        public static SplitterChunk of(List<Segment> segments) {
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
            return new SplitterChunk(md, token);
        }

        public static String dump(List<SplitterChunk> chunks) {
            StringBuilder sb = new StringBuilder();
            for (SplitterChunk chunk : chunks) {
                sb.append("-----token:").append(chunk.token).append(" (").append(chunk.markdown.length()).append(")-----\n")
                        .append(chunk.markdown).append("\n");
            }
            return sb.toString();
        }
    }


    private final TokenCounter tokenCounter;
    private final SplitterConf conf;
    private final SegmentSplitter parser = new SegmentSplitter();

    public Splitter(TokenCounter tokenCounter, SplitterConf conf) {
        this.tokenCounter = tokenCounter;
        this.conf = conf;
    }

    public List<SplitterChunk> splitMarkdown(String markdownText, String title) {
        List<Segment> segments = parser.removeLinkAndSplitByHeadings(markdownText, title);
        Segment.estimateTokenNum(segments, tokenCounter);
        return splitSegments(segments);
    }


    public List<SplitterChunk> splitSegments(List<Segment> segments) {
        List<Segment> refined = cutLargeSegments(segments);

        int[] tokens = new int[refined.size()];
        int[] levels = new int[refined.size()];
        int i = 0;
        for (Segment seg : refined) {
            tokens[i] = seg.getTokenNum();
            levels[i] = seg.getLevel();
            i++;
        }
        int[] bestSplit = findBestSplit(tokens, levels, conf.splitBestMin, conf.splitBestMax);

        if (bestSplit == null || bestSplit.length == 0) {
            return List.of(SplitterChunk.of(segments));
        }

        List<SplitterChunk> result = new ArrayList<>(bestSplit.length + 1);
        int from = 0;
        for (int splitPoint : bestSplit) {
            result.add(SplitterChunk.of(refined.subList(from, splitPoint)));
            from = splitPoint;
        }
        result.add(SplitterChunk.of(refined.subList(from, refined.size())));
        return result;
    }


    private List<Segment> cutLargeSegments(List<Segment> segments) {
        List<Segment> refined = segments;
        boolean needRefine = false;
        for (Segment segment : segments) {
            if (segment.getBodyTokenNum() > conf.segmentTriggerSplitLength) {
                needRefine = true;
                break;
            }
        }

        if (needRefine) {
            refined = new ArrayList<>(segments.size() + 2);
            for (Segment segment : segments) {
                if (segment.getBodyTokenNum() <= conf.segmentTriggerSplitLength) {
                    refined.add(segment);
                } else {
                    cutLargeSegmentTo(segment, refined);
                }
            }
        }

        return refined;
    }

    private void cutLargeSegmentTo(Segment segment, List<Segment> refined) {
        int n = (segment.getBodyTokenNum() + conf.segmentBestLength - 10) / conf.segmentBestLength;
        int limit = segment.getBodyTokenNum() / n;

        List<String> lines = segment.getBody().lines().toList();
        List<String> cur = new ArrayList<>();
        int curTokens = 0;

        for (String line : lines) {
            if (line.isBlank()) { //顺便去除了
                continue;
            }

            int lineTokens = tokenCounter.countTokens(line);

            if (curTokens + lineTokens < limit) {
                cur.add(line);
                curTokens += lineTokens;
            } else {
                if (!cur.isEmpty()) {
                    // curTokens差不多准确就行
                    Segment seg = new Segment(segment, String.join("\n", cur), curTokens);
                    refined.add(seg);
                }

                cur = new ArrayList<>(4);
                cur.add(line);
                curTokens = lineTokens;
            }
        }

        if (!cur.isEmpty()) {
            Segment seg = new Segment(segment, String.join("\n", cur), curTokens);
            refined.add(seg);
        }
    }

    public static int[] findBestSplit(int[] tokens, int[] levels, int limitMin, int limitMax) {
        int bestNum = (limitMin + limitMax) / 2;
        int numSpace = (limitMax - limitMin) / 2;
        if (numSpace == 0) {
            numSpace = limitMin / 2;
        }

        int n = tokens.length;
        int[][] dp = new int[n][];
        double[] dpScore = new double[n];

        for (int i = n - 1; i >= 0; i--) {
            double minScore = Double.MAX_VALUE;
            int[] bestSplit = null;
            int level = levels[i];
            double levelScore = 2 * Math.pow(level - 1, 2);
            if (level == 0) {
                levelScore = 0;
            }

            double currentSum = 0;
            for (int j = i; j < n; j++) {
                currentSum += tokens[j];
                double diff = Math.abs(currentSum - bestNum) / numSpace;
                double deviationScore = (diff <= 1) ? 0.5 * diff * diff : diff * diff;

                if (j == n - 1) {
                    double totalScore = deviationScore + levelScore;
                    if (totalScore < minScore) {
                        minScore = totalScore;
                        bestSplit = new int[]{};
                    }
                } else if (dp[j + 1] != null) {
                    double totalScore = dpScore[j + 1] + deviationScore + levelScore + 0.1 * dp[j + 1].length;
                    if (totalScore < minScore) {
                        minScore = totalScore;

                        int[] d = dp[j + 1];
                        bestSplit = new int[d.length + 1];
                        bestSplit[0] = j + 1; // 在这之前分割
                        System.arraycopy(d, 0, bestSplit, 1, d.length);
                    }
                }
            }
            dp[i] = bestSplit;
            dpScore[i] = minScore;
//            System.out.printf("%d: %.04f %s\n", i, minScore, Arrays.stream(bestSplit).boxed().toList().toString());
        }

        return dp[0];
    }


    public static void main(String[] args) throws IOException {
        Path mdFile = Path.of("doc/dify_svn_woa/dify_svn_woa/2025-05-22_17_14/诛仙/策划文档/7功能设计/【公测版本】坐骑系统.md");
        String md = Files.readString(mdFile);
        Splitter splitter = new Splitter(TokenCounter.getDeepSeekR10528(), new SplitterConf(
                2000, 1200, 750, 1250));
        List<SplitterChunk> result = splitter.splitMarkdown(md, "坐骑系统");
        System.out.println(SplitterChunk.dump(result));

    }

}