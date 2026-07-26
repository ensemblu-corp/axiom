package com.ensemblu.axiom.core.config;

import com.ensemblu.axiom.api.TargetNavigator;
import com.ensemblu.axiom.core.validation.If;
import com.ensemblu.axiom.core.validation.Result;
import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.foundation.DataCast;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;
import java.util.function.Function;


public final class ConfigSource {

    private final Result<Properties> properties;
    private final String source;

    private ConfigSource(final Result<Properties> properties, final String source) {
        this.properties = properties;
        this.source = source;
    }

    public static ConfigSource fileConfigSource(final String fileName) {
        return new ConfigSource(readFile2String(//
                OriginSource.FILE_NAME, //
                fileName, cfn -> ConfigSource.readPropertiesFromFile(cfn).prependFailureMessage("Classpath Resource Error: ")), //
                String.format("File: %s", fileName));//
    }

    public static ConfigSource stringConfigSource(final String propString) {
        return new ConfigSource(readFile2String(OriginSource.PROP_STRING, propString, //
                ConfigSource::readPropertiesFromString), //
                String.format("String: %s", propString));//
    }

    private static Result<Properties> readFile2String(final OriginSource originSource, final String path,
                                                      final Function<String, Result<Properties>> loader) {
        final var label = originSource.getOrigin();

        return If.givenObject(path)//
                .isNonNull(label)//
                .andIsNot(String::isBlank, "The " + label + " must not be blank.")//
                .will()//
                .flatMapTo(__ -> loader.apply(path))//
                .getResult();//
    }

    private static Result<Properties> readPropertiesFromFile(final String configFileName) {
        final var resource = ConfigSource.class.getClassLoader().getResourceAsStream(configFileName);
        if (resource == null) {
            return Result.failure("Configuration breach: File [" + configFileName + "] not found in classpath.");
        }

        try (resource) {
            final var props = new Properties();
            props.load(resource);
            return Result.success(props);
        } catch (IOException e) {
            return Result.failure("Effect Breach reading config [" + configFileName + "]", e);
        } catch (Exception e) {
            return Result.failure("AXIOM BREACH | Unexpected error reading config [" + configFileName + "]\nRAW ERROR: " + e.getMessage());
        }
    }

    private static Result<Properties> readPropertiesFromString(final String propString) {
        final var content = unescapeSubstance(propString);
        try (final var reader = new StringReader(content)) {
            final var properties = new Properties();
            properties.load(reader);
            return Result.success(properties); //
        } catch (final Exception e) {
            return Result.failure("AXIOM BREACH | Exception reading property string\nRAW ERROR: " + e.getMessage());
        }
    }

    private static String unescapeSubstance(String input) {
        return input.replace("\\n", "\n");
    }

    public TargetNavigator targetField(String key) {
        return new TargetNavigator() {
            @Override
            public <T> Result<T> execute(DataCast.Protocol protocol) {
                return properties.flatMap(props -> {
                    final var val = props.getProperty(key);
                    if (val == null || val.isBlank()) {
                        return Result.failure("Property [" + key + "] not found or empty in " + source);
                    }
                    return DataCast.cast(val, protocol);
                });
            }
        };
    }

    /**
     * Replaces GetAs.asList logic: Stitches a semicolon-separated list into sub-readers.
     */
    public <T> Result<PersistentList<T>> asMappedList(String key, Function<ConfigSource, T> mapper) {
        return targetField(key).toStringResult()//
                .map(rawString -> PersistentList.list(rawString.split(";")).map(String::trim))//
                .flatMap(rawList -> rawList.fold(//
                        Result.success(PersistentList.<T>empty()),//
                        (acc, element) -> acc.flatMap(list -> {//
                            final var subReader = stringConfigSource(element.replace(",", "\n"));//

                            return Result.of(() -> mapper.apply(subReader))//
                                    .map(list::append)//
                                    .prependFailureMessage("Error mapping element [" + element + "]: ");//
                        })//
                ))//
                .prependFailureMessage("Parsing breach for [" + key + "]: ");
    }

    private enum OriginSource {
        FILE_NAME("fileName"), PROP_STRING("propString");

        private String origin;

        OriginSource(final String origin) {
            this.origin = origin;
        }

        public String getOrigin() {
            return origin;
        }

    }
}