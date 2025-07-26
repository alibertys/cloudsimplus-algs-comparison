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
 * Homogeneous system comparison implementation of the abstract class.\
 * Represents Homogeneous system simulation where all resources have identical or uniform characteristics
 * Implements necessary methods required to create and configure homogeneous datacenter, VMs, and cloudlets
 * Manages homogeneous system simulation by leveraging the base class functionality for results analysis of CSV writing
 * 
 * The class utilizes the base class's features:
 * Simulation Execution
 * Oversubscription Detection
 * Metric Calculation
 */
public class HomogeneousSystemComparison extends SystemComparisonBase {
	
    public HomogeneousSystemComparison(int runId, ConfigLoader configLoader, boolean displayOversubTable,
    		String vmAllocationPolicy, String vmScheduler, String cloudletScheduler) {
        super(runId, configLoader, displayOversubTable, vmAllocationPolicy, vmScheduler, cloudletScheduler);
    }

    // Create 4 hosts in the datacenter, apply vmAllocationPolicy which is loaded dynamically from the config.json via configLoader
    @Override
    protected Datacenter createDatacenter() {
        List<Host> hostList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            hostList.add(createHost());
        }
        
        VmAllocationPolicy allocationPolicy = (VmAllocationPolicy) configLoader.createInstance("homogeneous", "vmAllocationPolicy");
        
        return new DatacenterSimple(simulation, hostList, allocationPolicy);
    }
    //host parameters with a dynamically configured vmScheduler
    private Host createHost() {
        List<Pe> peList = new ArrayList<>();
        int hostPes = 8;
        int hostMips = 2000;
        int hostRam = 1024 * 12;
        int hostBw = 8000;
        int hostStorage = 1_000_000;

        for (int i = 0; i < hostPes; i++) {
            peList.add(new PeSimple(hostMips));
        }
        
        VmScheduler vmScheduler = (VmScheduler) configLoader.createInstance("homogeneous", "vmScheduler");
        
        return new HostSimple(hostRam, hostBw, hostStorage, peList)
                .setVmScheduler(vmScheduler);
    }

    //vm parameters with a dynamically configured cloudletScheduler
    @Override
    protected List<Vm> createVms() {
        List<Vm> vms = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            vms.add(createVm());
        }
        return vms;
    }

    private Vm createVm() {
        int vmPes = 4;
        int vmMips = 1500;
        int vmRam = 4048;
        int vmBw = 500;
        int vmStorage = 100_000;

        CloudletScheduler cloudletScheduler = (CloudletScheduler) configLoader.createInstance("homogeneous", "cloudletScheduler");
        
        return new VmSimple(vmMips, vmPes)
                .setRam(vmRam).setBw(vmBw).setSize(vmStorage)
                .setCloudletScheduler(cloudletScheduler);
    }

    //cloudlet parameters
    @Override
    protected List<Cloudlet> createCloudlets() {
        List<Cloudlet> cloudlets = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            cloudlets.add(createCloudlet());
        }
        return cloudlets;
    }

    private Cloudlet createCloudlet() {
        int cloudletPes = 2;
        int cloudletLength = 10_000;
        int fileSize = 100;
        int outputSize = 50;

        return new CloudletSimple(cloudletLength, cloudletPes)
                .setFileSize(fileSize)
                .setOutputSize(outputSize)
                .setUtilizationModelCpu(new UtilizationModelFull())
                .setUtilizationModelRam(new UtilizationModelDynamic(0.25)) //average
                .setUtilizationModelBw(new UtilizationModelStochastic());
    }

    //Csv specifications
    @Override
    protected String getDetailedCsvFilePath() {
        return "Showcase_Homogeneous_Detailed.csv";
    }

    @Override
    protected String getMetricsCsvFilePath() {
        return "Showcase_Homogeneous_Metrics.csv";
    }
}
