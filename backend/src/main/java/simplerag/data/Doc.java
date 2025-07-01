package simplerag.data;

import java.time.LocalDateTime;
import java.util.List;

public record Doc(String id,  // id = path
                  String title,
                  String body,
                  String project,
                  String url,
                  List<String> writers,
                  LocalDateTime createdTime,
                  LocalDateTime lastModifiedTime) {
}
