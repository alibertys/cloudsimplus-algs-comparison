package org.bsc.cloudsimulations.comparison;

import java.util.ArrayList; 
import java.util.List;

import org.bsc.cloudsimulations.configurations.ConfigLoader;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicy;
import org.cloudsimplus.schedulers.vm.VmScheduler;
import org.cloudsimplus.schedulers.cloudlet.CloudletScheduler;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.utilizationmodels.UtilizationModelStochastic;

/**
 * Heterogeneous system comparison implementation of SystemComparisonBase.
 * Designed to simulate a heterogeneous system where hosts, vms, and cloudlets have varying characteristics
 * Operates similarly to HomogeneousSystemComparison
 */
public class HeterogeneousSystemComparison extends SystemComparisonBase {
	
	
    public HeterogeneousSystemComparison(int runId, ConfigLoader configLoader, boolean displayOversubTable,
    		String vmAllocationPolicy, String vmScheduler, String cloudletScheduler) {
        super(runId, configLoader, displayOversubTable, vmAllocationPolicy, vmScheduler, cloudletScheduler);
    }

    @Override
    protected Datacenter createDatacenter() {
        List<Host> hostList = new ArrayList<>();
        //4 hosts with "light" characteristics
        for (int i = 0; i < 4; i++) {
            hostList.add(createHost(1));
        }
        //2 hosts with "medium" characteristics
        for (int i = 0; i < 2; i++) {
            hostList.add(createHost(2));
        }
        //4 hosts with "strong" characteristics
        for (int i = 0; i < 4; i++) {
            hostList.add(createHost(0));
        }
        //Dynamically load vmAllocationPolicy from user input
        VmAllocationPolicy allocationPolicy = (VmAllocationPolicy) configLoader.createInstance("heterogeneous", "vmAllocationPolicy");
        
        return new DatacenterSimple(simulation, hostList, allocationPolicy);
    }
    //host parameters
    private Host createHost(int type) {
        List<Pe> peList = new ArrayList<>();
        int hostPes, hostMips, hostRam, hostBw, hostStorage;
        switch (type) {
            case 1: // Light
                hostPes = 4;
                hostMips = 1000;
                hostRam = 4096;
                hostBw = 1000;
                hostStorage = 1_000_000;
                break;
            case 2: // Medium
                hostPes = 8;
                hostMips = 2500;
                hostRam = 16384;
                hostBw = 5000;
                hostStorage = 2_000_000;
                break;
            default: // Strong
                hostPes = 16;
                hostMips = 4000;
                hostRam = 32768;
                hostBw = 10_000;
                hostStorage = 2_000_000;
        }
        for (int i = 0; i < hostPes; i++) {
            peList.add(new PeSimple(hostMips));
        }
        //Dynamically load vmScheduler
        VmScheduler vmScheduler = (VmScheduler) configLoader.createInstance("heterogeneous", "vmScheduler");
        
        return new HostSimple(hostRam, hostBw, hostStorage, peList)
                .setVmScheduler(vmScheduler);
    }

    //Parameters for vms
    @Override
    protected List<Vm> createVms() {
        List<Vm> vms = new ArrayList<>();
        //3 "light"
        for (int i = 0; i < 3; i++) {
            vms.add(createVm(1));
        }
        //4 "medium"
        for (int i = 0; i < 4; i++) {
            vms.add(createVm(2));
        }
        //3 "strong"
        for (int i = 0; i < 3; i++) {
            vms.add(createVm(0));
        }
        return vms;
    }

    private Vm createVm(int type) {
        int vmPes, vmMips, vmRam, vmBw, vmStorage;
        switch (type) {
            case 1: // Small/light
                vmPes = 1;
                vmMips = 500;
                vmRam = 1024;
                vmBw = 200;
                vmStorage = 40_000;
                break;
            case 2: // Medium
                vmPes = 2;
                vmMips = 1500;
                vmRam = 2048;
                vmBw = 300;
                vmStorage = 60_000;
                break;
            default: // Large/strong
                vmPes = 4;
                vmMips = 3000;
                vmRam = 4096;
                vmBw = 1000;
                vmStorage = 100_000;
        }
        //Dynamically load cloudletScheduler
        CloudletScheduler cloudletScheduler = (CloudletScheduler) configLoader.createInstance("heterogeneous", "cloudletScheduler");
        
        return new VmSimple(vmMips, vmPes)
                .setRam(vmRam).setBw(vmBw).setSize(vmStorage)
                .setCloudletScheduler(cloudletScheduler);
    }
    //parameters for cloudlets
    @Override
    protected List<Cloudlet> createCloudlets() {
        List<Cloudlet> cloudlets = new ArrayList<>();
        //15 "light"
        for (int i = 0; i < 15; i++) {
            cloudlets.add(createCloudlet(1));
        }
        //20 "medium"
        for (int i = 0; i < 20; i++) {
            cloudlets.add(createCloudlet(2));
        }
        //15 "strong"
        for (int i = 0; i < 15; i++) {
            cloudlets.add(createCloudlet(0));
        }
        return cloudlets;
    }

    private Cloudlet createCloudlet(int type) {
        int cloudletPes, cloudletLength, fileSize, outputSize;
        switch (type) {
            case 1:
                cloudletPes = 1;
                cloudletLength = 1000;
                fileSize = 10;
                outputSize = 1;
                break;
            case 2:
                cloudletPes = 2;
                cloudletLength = 10000;
                fileSize = 100;
                outputSize = 50;
                break;
            default:
                cloudletPes = 4;
                cloudletLength = 50_000;
                fileSize = 4000;
                outputSize = 100;
        }
        return new CloudletSimple(cloudletLength, cloudletPes)
                .setFileSize(fileSize)
                .setOutputSize(outputSize)
                .setUtilizationModelCpu(new UtilizationModelFull())
                .setUtilizationModelRam(new UtilizationModelDynamic(0.25))
                .setUtilizationModelBw(new UtilizationModelStochastic());
    }

    //handle csv
    @Override
    protected String getDetailedCsvFilePath() {
        return "Showcase_Heterogeneous_Detailed.csv";
    }

    @Override
    protected String getMetricsCsvFilePath() {
        return "Showcase_Heterogeneous_Metrics.csv";
    }
}
