package simplerag.service;

import java.nio.file.Path;
import java.util.Map;

public class ImportService {

    public record DocPath(String docId,
                          Path path,
                          String project,
                          String title,
                          String url) {

    }


    public static Map<String, DocPath> getDocPaths(Path confDir, Path svnDir, Path woaDir) {
        return null;
    }
}
