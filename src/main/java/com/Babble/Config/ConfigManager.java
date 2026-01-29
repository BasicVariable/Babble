package com.Babble.Config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final String FILE_NAME = "settings.yml";
    private static Config currentConfig;

    public static Config load() {
        if (currentConfig != null) return currentConfig;

        Path path = Paths.get(FILE_NAME);
        if (!Files.exists(path)) {
            System.out.println("Config file not found, creating default.");
            currentConfig = new Config();
            save(currentConfig);
            return currentConfig;
        }

        try (FileInputStream inputStream = new FileInputStream(FILE_NAME)) {
            org.yaml.snakeyaml.LoaderOptions loaderOptions = new org.yaml.snakeyaml.LoaderOptions();
            org.yaml.snakeyaml.inspector.TagInspector tagInspector = tag -> tag.getClassName()
                .equals(Config.class.getName())
            ;
            loaderOptions.setTagInspector(tagInspector);

            Yaml yaml = new Yaml(new Constructor(Config.class, loaderOptions));

            currentConfig = yaml.load(inputStream);
            if (currentConfig == null)
                currentConfig = new Config();
        } catch (Exception e) {
            System.err.println("Failed to load settings.yml: " + e.getMessage());
            e.printStackTrace();
            currentConfig = new Config();
        }
        return currentConfig;
    }

    public static void save(Config config) {
        currentConfig = config;
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        org.yaml.snakeyaml.representer.Representer representer = new org.yaml.snakeyaml.representer.Representer(
            options
        );
        representer.addClassTag(Config.class, org.yaml.snakeyaml.nodes.Tag.MAP); // Write as simple map

        Yaml yaml = new Yaml(representer, options);
        try (FileWriter writer = new FileWriter(FILE_NAME)) {
            yaml.dump(config, writer);
            System.out.println("Settings saved to " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }
}
