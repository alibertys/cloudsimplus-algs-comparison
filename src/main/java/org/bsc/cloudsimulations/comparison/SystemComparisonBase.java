package org.bsc.cloudsimulations.comparison;

import com.opencsv.CSVWriter;   

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bsc.cloudsimulations.configurations.ConfigLoader;
import org.bsc.cloudsimulations.configurations.PolicyAndSchedulerShortCodes;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletExecution;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

/**
 * Abstract base class for system comparison (homogeneous or heterogeneous).
 * 
 * Handles common tasks like creating datacenter, VMs, and Cloudlets
 * Manages the simulation, gathers and writes metrics
 * Enables evaluation of oversubscription scenarios
 */
public abstract class SystemComparisonBase {
	//Load and Update json configurations
	protected ConfigLoader configLoader;
	//Main Variables for handling the simulation
    protected CloudSimPlus simulation; //Simulation life-cycle
    protected DatacenterBroker broker; //VM and Cloudlets submissions
    protected List<Vm> vmList; //Store VMs
    protected List<Cloudlet> cloudletList; //Store Cloudlets
    protected Datacenter datacenter; //Simulation datacenter representation
    //Strings representing selected strategies
    protected String vmAllocationPolicy;
    protected String vmScheduler;
    protected String cloudletScheduler;
    //Maps cloudlet IDs to expected finishTimes
    protected final Map<Long, Double> expectedFinishTimes = new HashMap<>();

    protected int runId;
    
    /*Constructor
     * Initializes configuration values and simulation components
     * Creates and submits VMs and Cloudlets
     * Starts simulation
     * Process the results
     * */
    public SystemComparisonBase(int runId, ConfigLoader configLoader, boolean displayOversubscriptionTable, 
    		String vmAllocationPolicy, String vmScheduler, String cloudletScheduler) {
    	
    	this.configLoader = configLoader;
        this.runId = runId;
        this.vmAllocationPolicy = vmAllocationPolicy;
        this.vmScheduler = vmScheduler;
        this.cloudletScheduler = cloudletScheduler;
        
        simulation = new CloudSimPlus();
        datacenter = createDatacenter();
        broker = new DatacenterBrokerSimple(simulation);

        vmList = createVms();
        cloudletList = createCloudlets();

        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);

        simulation.start();

        final var finishedCloudlets = broker.getCloudletFinishedList();

        calculateAndDisplayOversubscribedCloudlets(displayOversubscriptionTable);
        demonstrateCloudletTable(finishedCloudlets);
        calculateAndWriteMetrics(finishedCloudlets);
    }

    protected abstract Datacenter createDatacenter();
    protected abstract List<Vm> createVms();
    protected abstract List<Cloudlet> createCloudlets();
    
    /*
     * Below are the Main methods and Helpers
     * demonstrateCloudletTable - write detailed metrics of finished cloudlets to a csv file including described information and also maps policy/scheduler names to short codes
     * calculateAndDisplayOversubscribedCloudlets - identify cloudlets with oversubscription issues, prints details (if enabled), and updates expectedFinishTimes map
     * calculateAndWriteMetric - Calculates and writes aggregate performance metrics
     * calculateStandardDeviation - compute Standard Deviation of load values (host and vms)
     * CSV File handling - ensures headers are written once per file using headerWrittenFlags map, appends simulation results to CSV files
     * */
    
    private String[] getPolicyShortCodes() {
        // Get allocation policy class name from datacenter
        String allocationPolicyClassName = datacenter.getVmAllocationPolicy().getClass().getName();

        // Get scheduler class names from first VM (assuming all VMs use same schedulers)
        Vm firstVm = vmList.get(0);
        String vmSchedulerClassName = firstVm.getHost().getVmScheduler().getClass().getName();
        String cloudletSchedulerClassName = firstVm.getCloudletScheduler().getClass().getName();

        // Convert to short codes using your mapping helper methods (you'll need to add these)
        String allocationPolicyShortCode = PolicyAndSchedulerShortCodes.getShortCodeForVmAllocationPolicy(allocationPolicyClassName);
        String vmSchedulerShortCode = PolicyAndSchedulerShortCodes.getShortCodeForVmScheduler(vmSchedulerClassName);
        String cloudletSchedulerShortCode = PolicyAndSchedulerShortCodes.getShortCodeForCloudletScheduler(cloudletSchedulerClassName);

        return new String[]{allocationPolicyShortCode, vmSchedulerShortCode, cloudletSchedulerShortCode};
    }

    protected void demonstrateCloudletTable(List<Cloudlet> cloudletFinishedList) {
        String filePath = getDetailedCsvFilePath();
        //why try and catch besides error handling?
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath, true))) {
            writeHeaderIfNecessary(writer, filePath, new String[]{
                "Run ID", "Vm Allocation Policy", "Vm Scheduler", "Cloudlet Scheduler", 
                "Cloudlet ID", "Host Id", "Host PEs", "VM ID", "VM PEs",
                "Status", "ExecTime", "StartTime", "FinishTime", "StartWaitTime", "ExpectedFinishTime"
            });	

            for (Cloudlet cloudlet : cloudletFinishedList) {
            	Vm vm = cloudlet.getVm();
                String allocationPolicyShortCode = PolicyAndSchedulerShortCodes.getShortCodeForVmAllocationPolicy(
                        vm.getHost().getDatacenter().getVmAllocationPolicy().getClass().getName());
                    String vmSchedulerShortCode = PolicyAndSchedulerShortCodes.getShortCodeForVmScheduler(
                        vm.getHost().getVmScheduler().getClass().getName());
                    String cloudletSchedulerShortCode = PolicyAndSchedulerShortCodes.getShortCodeForCloudletScheduler(
                        vm.getCloudletScheduler().getClass().getName());
                
                double expectedFinishTime = expectedFinishTimes.getOrDefault(cloudlet.getId(), Double.NaN);
                writer.writeNext(new String[]{
                    String.valueOf(runId),
                    allocationPolicyShortCode,
                    vmSchedulerShortCode,
                    cloudletSchedulerShortCode,
                    String.valueOf(cloudlet.getId()),
                    String.valueOf(cloudlet.getVm().getHost().getId()),
                    String.valueOf(cloudlet.getVm().getHost().getWorkingPesNumber()),
                    String.valueOf(cloudlet.getVm().getId()),
                    String.valueOf(cloudlet.getVm().getPesNumber()),
                    cloudlet.getStatus().name(),
                    String.format("%.1f", cloudlet.getTotalExecutionTime()),
                    String.format("%.1f", cloudlet.getStartTime()),
                    String.format("%.1f", cloudlet.getFinishTime()),
                    String.format("%.1f", cloudlet.getStartWaitTime()),
                    String.format("%.1f", expectedFinishTime)
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void calculateAndDisplayOversubscribedCloudlets(boolean displayTable) {
        List<CloudletExecution> oversubscribedExecutions = vmList.stream()
            .flatMap(vm -> vm.getCloudletScheduler().getCloudletFinishedList().stream())
            .filter(CloudletExecution::hasOverSubscription)
            .collect(Collectors.toList());

        if (displayTable && !oversubscribedExecutions.isEmpty()) {
            System.out.println("Oversubscribed Cloudlets Details:");
            System.out.printf("%-10s %-20s %-20s %-20s%n", "CloudletID", "ExpectedTimeToComplete", "ActualTime", "PercentageIncrease");
            for (CloudletExecution cle : oversubscribedExecutions) {
                double expectedFinishTime = cle.getExpectedFinishTime();
                double actualFinishTime = cle.getCloudlet().getFinishTime();
                long cloudletId = cle.getCloudlet().getId();

                expectedFinishTimes.put(cloudletId, expectedFinishTime);

                if (expectedFinishTime > 0) {
                    double percentageIncrease = ((actualFinishTime - expectedFinishTime) / expectedFinishTime) * 100;
                    System.out.printf("%-10d %-20.2f %-20.2f %-20.2f%n", cloudletId, expectedFinishTime, actualFinishTime, percentageIncrease);
                }
            }
        } else {
        	// Still update expectedFinishTimes map even if no display
            for (CloudletExecution cle : oversubscribedExecutions) {
                double expectedFinishTime = cle.getExpectedFinishTime();
                long cloudletId = cle.getCloudlet().getId();
                expectedFinishTimes.put(cloudletId, expectedFinishTime);
            }
        }
    }

    protected void calculateAndWriteMetrics(List<Cloudlet> finishedCloudlets) {
        double makespan = finishedCloudlets.stream()
            .mapToDouble(Cloudlet::getFinishTime)
            .max()
            .orElse(0);

        double throughput = finishedCloudlets.size() / makespan;

        Map<Host, Integer> hostLoadMap = finishedCloudlets.stream()
            .collect(Collectors.groupingBy(
                cl -> cl.getVm().getHost(),
                Collectors.summingInt(cl -> (int) cl.getVm().getPesNumber())
            ));
        double hostLoadStdDev = calculateStandardDeviation(hostLoadMap.values());

        Map<Vm, Integer> vmLoadMap = finishedCloudlets.stream()
            .collect(Collectors.groupingBy(
                Cloudlet::getVm,
                Collectors.summingInt(cl -> (int) cl.getPesNumber())
            ));
        double vmLoadStdDev = calculateStandardDeviation(vmLoadMap.values());

        OversubscriptionMetrics oversubMetrics = calculateOversubscriptionMetrics();

        int oversubscribedCount = oversubMetrics.getOversubscribedCount();
        double avgPercentageIncrease = oversubMetrics.getAvgPercentageIncrease();
        
        String[] policyShortCodes = getPolicyShortCodes();
        String allocationPolicyShortCode = policyShortCodes[0];
        String vmSchedulerShortCode = policyShortCodes[1];
        String cloudletSchedulerShortCode = policyShortCodes[2];

        String filePath = getMetricsCsvFilePath();

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath, true))) {
            writeHeaderIfNecessary(writer, filePath, new String[]{
                "Run ID", "Vm Allocation Policy", "Vm Scheduler", "Cloudlet Scheduler", 
                "Makespan", "Throughput", "HostLoadStdDev", "VMLoadStdDev", "OversubscribedCount",
                "AvgPercentageIncreaseInCloudletExecTime", "TotalCompletedTasks"
            });

            writer.writeNext(new String[]{
                String.valueOf(runId),
                allocationPolicyShortCode,
                vmSchedulerShortCode,
                cloudletSchedulerShortCode,
                String.format("%.2f", makespan),
                String.format("%.2f", throughput),
                String.format("%.2f", hostLoadStdDev),
                String.format("%.2f", vmLoadStdDev),
                String.valueOf(oversubscribedCount),
                String.format("%.2f", avgPercentageIncrease),
                String.valueOf(finishedCloudlets.size())
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double calculateStandardDeviation(Collection<Integer> values) {
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.sqrt(values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / values.size());
    }

    private OversubscriptionMetrics calculateOversubscriptionMetrics() {
        List<CloudletExecution> oversubscribedExecutions = vmList.stream()
            .flatMap(vm -> vm.getCloudletScheduler().getCloudletFinishedList().stream())
            .filter(CloudletExecution::hasOverSubscription)
            .collect(Collectors.toList());

        int count = oversubscribedExecutions.size();
        double avgIncrease = oversubscribedExecutions.stream()
            .mapToDouble(cle -> {
                double expectedFinish = cle.getExpectedFinishTime();
                double actualFinish = cle.getCloudlet().getFinishTime();
                return (expectedFinish > 0) ? ((actualFinish - expectedFinish) / expectedFinish) * 100 : 0;
            })
            .average().orElse(0);

        return new OversubscriptionMetrics(count, avgIncrease);
    }
    
    //Abstract methods that returns file paths for CSV output
    protected abstract String getDetailedCsvFilePath();
    protected abstract String getMetricsCsvFilePath();

    public int getRunId() {
        return runId;
    }

    // Helper class for oversubscription metrics
    protected static class OversubscriptionMetrics {
        private final int oversubscribedCount;
        private final double avgPercentageIncrease;

        public OversubscriptionMetrics(int count, double avgIncrease) {
            this.oversubscribedCount = count;
            this.avgPercentageIncrease = avgIncrease;
        }

        public int getOversubscribedCount() { return oversubscribedCount; }
        public double getAvgPercentageIncrease() { return avgPercentageIncrease; }
    }
    
    private static final Map<String, Boolean> headerWrittenFlags = new HashMap<>();

    protected void writeHeaderIfNecessary(CSVWriter writer, String filePath, String[] header) {
        headerWrittenFlags.putIfAbsent(filePath, false);

        if (!headerWrittenFlags.get(filePath)) {
            writer.writeNext(header);
            headerWrittenFlags.put(filePath, true);
        }  
    }
}
