package org.bsc.cloudsimulations;

import org.bsc.cloudsimulations.comparison.HeterogeneousSystemComparison;  
import org.bsc.cloudsimulations.comparison.HomogeneousSystemComparison;
import org.bsc.cloudsimulations.configurations.ConfigLoader;
import org.bsc.cloudsimulations.configurations.PolicyAndSchedulerShortCodes;
import org.cloudsimplus.util.Log;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Scanner;

/*
 * Main class for running the simulations
 * Capture user inputs to customize the simulation (configuration type, allocation polices, scheduler, and etc.)
 * Loads JSON file for configuration of policies
 * */

public class Main {
    public static void main(String[] args) {
        int totalRuns = 100;
        
        String configPath = args.length > 0 ? args[0] : "org/bsc/cloudsimulations/configurations/config.json";
        ConfigLoader configLoader = new ConfigLoader(configPath);

        try {
            configLoader = new ConfigLoader(configPath);
            System.out.println("Loaded configuration from: " + configPath);
        } catch (RuntimeException e) {
            System.err.println("Failed to load configuration from: " + configPath);
            throw e; // Exit if configuration is mandatory
        }
        
        try (Scanner scanner = new Scanner(System.in)) {
            // Logging Level
            System.out.println("Select logging level: [ALL, DEBUG, INFO, WARN, ERROR, OFF]");
            while (true) {
                String logLevel = scanner.nextLine().trim().toUpperCase();
                if (setLogLevel(logLevel)) {
                    System.out.println("Logging level set to: " + logLevel);
                    break;
                } else {
                    System.out.println("Invalid input! Please enter one of [ALL, DEBUG, INFO, WARN, ERROR, OFF].");
                }
            }

            // Number of Runs
            System.out.println("Enter the number of simulation runs (default: 100):");
            while (true) {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("Using default number of runs: " + totalRuns);
                    break;
                }
                try {
                    totalRuns = Integer.parseInt(input);
                    if (totalRuns > 0) {
                        break;
                    } else {
                        System.out.println("Please enter a positive integer.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input! Please enter a positive integer.");
                }
            }

            // Configuration Type
            System.out.println("Select Configuration Type: [1] Homogeneous [2] Heterogeneous");
            int configType = getValidatedInputInt(scanner, "1|2", 2);

            // VM Allocation Policy
            System.out.println("Enter VM Allocation Policy: [S, FF, BF]");
            String vmAllocationPolicy = getValidatedPolicy(scanner, PolicyAndSchedulerShortCodes.VM_ALLOCATION_POLICIES);

            // VM Scheduler
            System.out.println("Enter VM Scheduler: [TS, SS]");
            String vmScheduler = getValidatedPolicy(scanner, PolicyAndSchedulerShortCodes.VM_SCHEDULERS);

            // Cloudlet Scheduler
            System.out.println("Enter Cloudlet Scheduler: [TS, SS]");
            String cloudletScheduler = getValidatedPolicy(scanner, PolicyAndSchedulerShortCodes.CLOUDLET_SCHEDULERS);

            // Display Oversubscription Table
            System.out.println("Display oversubscription table? [yes/no]");
            boolean displayOversubTable = getValidatedInputString(scanner, "yes|no", "no").equalsIgnoreCase("yes");

            // Update ConfigLoader
            JsonObject userConfig = new JsonObject();
            userConfig.addProperty("vmAllocationPolicy", vmAllocationPolicy);
            userConfig.addProperty("vmScheduler", vmScheduler);
            userConfig.addProperty("cloudletScheduler", cloudletScheduler);
            configLoader.updateSection(configType == 1 ? "homogeneous" : "heterogeneous", userConfig);

            // Run Simulation
            for (int i = 1; i <= totalRuns; i++) {
                if (configType == 1) {
                    System.out.println("Running Homogeneous System - Run " + i);
                    new HomogeneousSystemComparison(i, configLoader, displayOversubTable, vmAllocationPolicy, vmScheduler, cloudletScheduler);
                } else {
                    System.out.println("Running Heterogeneous System - Run " + i);
                    new HeterogeneousSystemComparison(i, configLoader, displayOversubTable, vmAllocationPolicy, vmScheduler, cloudletScheduler);
                }
            }
        }
    }

    private static boolean setLogLevel(String level) {
        try {
            switch (level) {
                case "ALL":
                    Log.setLevel(ch.qos.logback.classic.Level.ALL);
                    break;
                case "DEBUG":
                    Log.setLevel(ch.qos.logback.classic.Level.DEBUG);
                    break;
                case "INFO":
                    Log.setLevel(ch.qos.logback.classic.Level.INFO);
                    break;
                case "WARN":
                    Log.setLevel(ch.qos.logback.classic.Level.WARN);
                    break;
                case "ERROR":
                    Log.setLevel(ch.qos.logback.classic.Level.ERROR);
                    break;
                case "OFF":
                    Log.setLevel(ch.qos.logback.classic.Level.OFF);
                    break;
                default:
                    return false;
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error setting log level: " + e.getMessage());
            return false;
        }
    }

    private static String getValidatedPolicy(Scanner scanner, Map<String, String> policies) {
        String policy;
        while (true) {
            policy = policies.get(scanner.nextLine().trim());
            if (policy != null) return policy;
            System.out.println("Invalid input! Please try again.");
        }
    }

    private static String getValidatedInputString(Scanner scanner, String regex, String defaultValue) {
        while (true) {
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.matches(regex)) {
                return input;
            } else if (input.isEmpty()) {
                return defaultValue;
            } else {
                System.out.println("Invalid input! Please enter a valid option (e.g., yes or no).");
            }
        }
    }

    private static int getValidatedInputInt(Scanner scanner, String regex, int defaultValue) {
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.matches(regex)) {
                return Integer.parseInt(input);
            } else if (input.isEmpty()) {
                return defaultValue; // Use default if no input is given
            } else {
                System.out.println("Invalid input! Please enter a valid option.");
            }
        }
    }
}
