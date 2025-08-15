package org.dbdesktop.generator;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.nio.file.Paths;

public class MySQLtilGenerator implements IClassesGenerator{

    private final String host;
    private final Integer port;
    private final String database;
    private final String packageName;

    public MySQLtilGenerator(String host, Integer port, String database, String packageName) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.packageName = packageName;
    }

    @Override
    public void generateClasses(String outFolder) throws Exception {

        generateIMessageSender(outFolder);
    }

    private void generateIMessageSender(String outFolder) throws IOException {
        TypeSpec iMessageSender = TypeSpec.classBuilder("IMessageSender").build();
        JavaFile javaFile = JavaFile.builder(this.packageName, iMessageSender)
                .build();
        javaFile.writeTo(Paths.get(outFolder));
    }
}
