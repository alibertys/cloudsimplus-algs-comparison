package org.bsc.cloudsimulations.configurations;

import com.google.gson.*; 
import java.io.*;
import java.io.InputStreamReader;

/*
 * Utility methods loading, managing, and using configuration data from config.json
 * Dynamically create instances instances of classes based on the configuration
 * Validate class existence and update configuration sections at runtime
 * */
public class ConfigLoader {
    private JsonObject config;

    //constructor
    public ConfigLoader(String resourceFileName) {
        try (Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resourceFileName))) {
            config = JsonParser.parseReader(reader).getAsJsonObject(); //parse into JsonObject for easy access
            //Error handling
        } catch (NullPointerException e) {
            throw new RuntimeException("Resource file not found: " + resourceFileName, e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource file: " + resourceFileName, e);
        }
    }
    //retrieve a fully qualified class name from the json configuration based on the provided section and key
    //dynamically create instance of the class using reflection
    //in other words, enables dynamic instantiation of policies or schedulers by specifying their class names in the json configuration 
    public Object createInstance(String section, String key) {
        try {
            String className = config.getAsJsonObject(section).get(key).getAsString();
            return Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance for " + key + " in " + section, e); //the class cannot be found, and/or instance cannot be created due to refl error
        }
    }
    //updates specified section of the configuration with new key-value pairs from the newConfig json object
    //replaces or adds properties within section dynamically
    //facilitates runtime updates to the configuration (applying user-defined settings for allocation policies or schedulers)
    public void updateSection(String section, JsonObject newConfig) {
        JsonObject sectionConfig = config.getAsJsonObject(section);
        for (String key : newConfig.keySet()) {
            sectionConfig.addProperty(key, newConfig.get(key).getAsString());
        }
    }
    //checks if class with the given name exists in the classpath
    public boolean isValidClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}