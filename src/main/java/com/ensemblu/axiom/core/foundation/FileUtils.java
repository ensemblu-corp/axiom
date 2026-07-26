package com.ensemblu.axiom.core.foundation;

import com.ensemblu.axiom.core.validation.If;
import com.ensemblu.axiom.core.validation.Result;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

public interface FileUtils {

     static Result<String> readFile2String(final String path) {
        return If.givenObject(path)//
                .isNonNull("The file path")//
                .andIsNot(String::isEmpty, "The file path cannot be empty.")//
                .andIsNot(String::isBlank, "The file path cannot contains only white space codepoints.")
                .will()//
                .flatMapTo(FileUtils::readFileContent)//
                .getResult();
    }

    private static Result<String> readFileContent(final String path) {
        try (var inputStream = FileUtils.class.getClassLoader().getResourceAsStream(path)) {
            return If.givenObject(inputStream)//
                    .is(Objects::nonNull, "Resource '" + path + "' not found in classpath.")//
                    .will()//
                    .flatMapTo(inputS -> {
                        try (var scanner = new Scanner(inputS, StandardCharsets.UTF_8)) {
                            return (scanner.useDelimiter("\\A").hasNext())
                                ? Result.success(scanner.next())
                                : Result.failure("empty content is invalid"); //
                        }
                    }).getResult();
        } catch (final Exception e) {
            return Result.failure("AXIOM BREACH | Error reading file [" + path + "]\nRAW ERROR: " + e.getMessage());
        }
    }
}
