package simplerag.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class ImageExtractor {

    private final static Logger logger = LoggerFactory.getLogger(ImageExtractor.class.getName());

    private static final Pattern IMG_PATTERN = Pattern.compile(
            "!\\[(.*?)]\\(data:image/(?<type>[\\w+-]+);base64,(?<data>[a-zA-Z0-9+/=]+)\\)",
            Pattern.CASE_INSENSITIVE
    );

    public record ImageInfo(String type,
                            String id) {
    }

    public static String saveBase64AsImage(String base64Data, String imageType)  {
        // 解码 Base64
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        String id = generateBase64Filename(imageBytes, 12);
        System.out.println(id);

        // 确保目录存在
        Path path = Paths.get("img/" + id + "." + imageType);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, imageBytes);
        }catch (IOException e) {
            logger.error("save image {} error: {}", path, e.getMessage());
        }

        return id;
    }


    public static String generateBase64Filename(byte[] imageBytes, int length) {
        // 1. 计算MD5哈希（作为Base64输入）
        byte[] hashBytes;
        try {
            hashBytes = MessageDigest.getInstance("SHA-256").digest(imageBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }

        // 2. 转换为Base64并移除填充字符
        String base64 = Base64.getEncoder().withoutPadding().encodeToString(hashBytes);

        // 3. 替换URL不安全的字符
        base64 = base64.replace('+', '-').replace('/', '_');

        // 4. 截取指定长度
        return base64.substring(0, Math.min(length, base64.length()));
    }


    public static void extractAllImages(String content, List<ImageInfo> result) {
        Matcher matcher = IMG_PATTERN.matcher(content);

        while (matcher.find()) {
            String type = matcher.group("type");
            String base64 = matcher.group("data");
            String md5 = saveBase64AsImage(base64, type);
            result.add(new ImageInfo(type, md5));
        }
    }


    public static void extractImages(Path path) throws IOException {
        List<ImageInfo> result = new ArrayList<>();
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> {
                extractAllImages(line, result);
            });
        }

    }

    public static void main(String[] args) throws IOException {
        extractImages(Path.of("data/87854843_Houdini仿Gfur生成毛发模型工具.md"));

    }
}
