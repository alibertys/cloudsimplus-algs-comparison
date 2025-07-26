package org.bsc.cloudsimulations.configurations;

import java.util.Map;
import java.util.HashMap;

//utility for mapping short codes to lengthy class names
//reverse the mappings for retrieving short codes from class names

public class PolicyAndSchedulerShortCodes {
    public static final Map<String, String> VM_ALLOCATION_POLICIES = Map.of(
            "S", "org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple",
            "FF", "org.cloudsimplus.allocationpolicies.VmAllocationPolicyFirstFit",
            "BF", "org.cloudsimplus.allocationpolicies.VmAllocationPolicyBestFit"
        );

        public static final Map<String, String> VM_SCHEDULERS = Map.of(
            "TS", "org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared",
            "SS", "org.cloudsimplus.schedulers.vm.VmSchedulerSpaceShared"
        );

        public static final Map<String, String> CLOUDLET_SCHEDULERS = Map.of(
            "TS", "org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared",
            "SS", "org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared"
        );
        
        // Reverse mappings for short code retrieval
        //reverseMap() helper method iterates over forward mappings and swaps keys and values
        private static final Map<String, String> REVERSE_VM_ALLOCATION_POLICIES = reverseMap(VM_ALLOCATION_POLICIES);
        private static final Map<String, String> REVERSE_VM_SCHEDULERS = reverseMap(VM_SCHEDULERS);
        private static final Map<String, String> REVERSE_CLOUDLET_SCHEDULERS = reverseMap(CLOUDLET_SCHEDULERS);

        private static Map<String, String> reverseMap(Map<String, String> map) {
            Map<String, String> reversed = new HashMap<>();
            map.forEach((key, value) -> reversed.put(value, key));
            return reversed;
        }
        //return unknown if the class name is not found
        /*Example:
         * String vmPolicyClassName = PolicyAndSchedulerShortCodes.VM_ALLOCATION_POLICIES.get("S");
         * Output: "org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple"
         * */
        public static String getShortCodeForVmAllocationPolicy(String className) {
            return REVERSE_VM_ALLOCATION_POLICIES.getOrDefault(className, "UNKNOWN");
        }

        public static String getShortCodeForVmScheduler(String className) {
            return REVERSE_VM_SCHEDULERS.getOrDefault(className, "UNKNOWN");
        }

        public static String getShortCodeForCloudletScheduler(String className) {
            return REVERSE_CLOUDLET_SCHEDULERS.getOrDefault(className, "UNKNOWN");
        }
}
