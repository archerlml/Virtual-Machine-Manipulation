package edu.sjsu.cmpe.hw1;


import java.net.URL;
import java.rmi.RemoteException;
import java.util.Scanner;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;



/**
 * Created by archer on 10/1/16.
 */
public class test {
    public static void main(String[] args) throws Exception{
       Scanner scanner = new Scanner(System.in);
        System.out.println("Enter IP, username, password, and VM name in one line, separated by space:");
        String strIP = scanner.next();
        String strName = scanner.next();
        String strPass = scanner.next();
        String strVM = scanner.next();
//        Enter IP, username, password, and Vm name:
//        https://172.16.55.135/sdk root 081152016 MingluLiu-CentOS6.8i386-794-2
//        https://130.65.159.14/sdk cmpe283_sec3_student@vsphere.local cmpe-W6ik Minglu-ub1404-794-1

        ServiceInstance si = new ServiceInstance(new URL(strIP),strName,strPass,true);
//      ServiceInstance si = new ServiceInstance(new URL("https://172.16.55.135/sdk"),"root","081152016",true);
        //https://130.65.159.14:9443/vsphere-client/
        //cmpe283_sec3_student@vsphere.local
        //cmpe-W6ik
        //MingluLiu-Ubuntukylin1604-794-1, MingluLiu-CentOS6.8i386-794-2
        //two VM templates on vCenter
        //cmperoot
        //ch@ngeSJSU1!


        System.out.println("CMPE283 HW2 from Minglu Liu");

        try {
            ManagedEntity[] hostManagedEntities = new InventoryNavigator(si.getRootFolder()).searchManagedEntities("HostSystem");
            int k = 0;
            for (ManagedEntity hme : hostManagedEntities) {
                HostSystem hostSystemObj = (HostSystem) hme;
                System.out.println("host[" + k + "]:");
                //print host name
                String ESXhostname = hostSystemObj.getName();
                System.out.println("Name = " + ESXhostname);
                //print product full name
                String productName = hostSystemObj.getConfig().getProduct().name;
                System.out.println("ProductFullName = " + productName);
                k++;
            }
            ManagedEntity[] hostSpecificEntities = new InventoryNavigator(si.getRootFolder()).searchManagedEntities("VirtualMachine");
            ManagedEntity managedEntityVM = new InventoryNavigator(si.getRootFolder()).searchManagedEntity("VirtualMachine", strVM);
            ManagedEntity[] managedEntityDC = new InventoryNavigator(si.getRootFolder()).searchManagedEntities("Datacenter");
            VirtualMachine vm = (VirtualMachine) managedEntityVM;


            System.out.println("VM Name = " + vm.getName());

            System.out.println("GuestOS = " + vm.getConfig().getGuestFullName());
            System.out.println("Guest State = " + vm.getGuest().getGuestState());
            //guestinfo.getGuestState()
            System.out.println("Power State = " + vm.getRuntime().getPowerState());

            //Folder folderCMPE283 = (Folder)vmFolder.getParent().getParent();

            HostSystem esxiHost = new HostSystem(vm.getServerConnection(), vm.getRuntime().getHost());
            System.out.println("Host = " + esxiHost.getName());

           //snapshot
           Task taskSnapshot = vm.createSnapshot_Task("snapshotTask", "take a snapshot", true, true);
           taskSnapshot.waitForTask();
           String statusSnapshot = taskSnapshot.getTaskInfo().getState().toString();
           System.out.println("Snapshot: status = " + statusSnapshot);

           //clone
           Datacenter dc = (Datacenter) managedEntityDC[0];
           VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
           cloneSpec.setLocation(new VirtualMachineRelocateSpec());
           cloneSpec.setPowerOn(false);
           cloneSpec.setTemplate(false);
           Task taskC = vm.cloneVM_Task((Folder)vm.getParent(), vm.getName() + "-clone", cloneSpec);
           taskC.waitForTask();
           String statusClone = taskC.getTaskInfo().getState().toString();
           System.out.println("Clone: status = " + statusClone);

           //migrate
           if(hostManagedEntities.length <= 1){
               System.out.println("Migration skipped: only one ESXi host");
           }else {
               for (int i = 0; i < hostManagedEntities.length; i++) {
                   HostSystem newHost = (HostSystem) hostManagedEntities[i];
                   HostSystem nextHost = newHost;
                   if (i + 1 < hostManagedEntities.length) {
                       nextHost = (HostSystem) hostManagedEntities[i + 1];
                   } else {
                       nextHost = (HostSystem) hostManagedEntities[0];
                   }
                   if (newHost.getName().equals(exsiHost.getName())) {
                       Task taskM = vm.migrateVM_Task(null, nextHost, VirtualMachineMovePriority.highPriority, null);
                       taskM.waitForTask();
                       String statusM = taskM.getTaskInfo().getState().toString();
                       System.out.println("Migrate to " + nextHost.getName() +": status = "+ statusM);

                       break;
                   } else {
                       Task taskM = vm.migrateVM_Task(null, nextHost, VirtualMachineMovePriority.highPriority, null);
                       taskM.waitForTask();
                       String statusM = taskM.getTaskInfo().getState().toString();
                       System.out.println("Migrate to " + nextHost.getName() +": status = "+ statusM);

                       break;
                   }
               }
           }
            Task taskM = vm.migrateVM_Task(null, null, VirtualMachineMovePriority.highPriority, null);
            String statusM = taskM.getTaskInfo().getState().toString();
            HostSystem vmHost = new HostSystem(vm.getServerConnection(), vm.getRuntime().getHost());
            System.out.println("Migrate to " + vmHost.getName() +": status = "+ statusM);

            //extra credit1 migration
           if(hostManagedEntities.length <= 1){
               System.out.println("Migration skipped: only one ESXi host");
           }else {
               for (int i = 0; i < hostManagedEntities.length; i++) {
                   HostSystem newHost = (HostSystem) hostManagedEntities[i];
                   HostSystem nextHost = newHost;
                   if (i + 1 < hostManagedEntities.length) {
                       nextHost = (HostSystem) hostManagedEntities[i + 1];
                   } else {
                       nextHost = (HostSystem) hostManagedEntities[0];
                   }
                   if (nextHost.getName().equals(esxiHost.getName())) {
                       Task taskM = vm.migrateVM_Task(null, newHost, VirtualMachineMovePriority.highPriority, null);
                       while(!taskM.getTaskInfo().getState().toString().equals("success")){
                           while(taskM.getTaskInfo().getState().toString().equals("running")){
                               System.out.print("o");
                           }
                           System.out.println("x");
                       }
                       String statusM = taskM.getTaskInfo().getState().toString();
                       System.out.println("Migrate to " + nextHost.getName() +": status = "+ statusM);

                       break;
                   } else {
                       Task taskM = vm.migrateVM_Task(null, nextHost, VirtualMachineMovePriority.highPriority, null);
                       while(!taskM.getTaskInfo().getState().toString().equals("success")){
                           while(taskM.getTaskInfo().getState().toString().equals("running")){
                               System.out.print("o");
                           }
                           System.out.println("x");
                       }
                       String statusM = taskM.getTaskInfo().getState().toString();
                       System.out.println("Migrate to " + nextHost.getName() +": status = "+ statusM);

                       break;
                   }
               }
           }

           //print all recent tasks
           Task[] tasks = vm.getRecentTasks();
           int m = 0;
           for(Task task : tasks) {
               System.out.print("task[" + m + "]: " + "operation name: " + tasks[m].getTaskInfo().getName() +
                       ", startTime: " + tasks[m].getTaskInfo().getStartTime().getTime() +
                       ", endTime: " + tasks[m].getTaskInfo().getCompleteTime().getTime());
               if (task.getTaskInfo().getState().toString() != Task.SUCCESS) {
                   System.out.println(" status: " + task.getTaskInfo().getError().getLocalizedMessage());
               }else{
                   System.out.println(" status: " + tasks[m].getTaskInfo().getState().toString());
               }
               m++;
           }

        }
        catch (InvalidProperty e) {
            e.printStackTrace();
        } catch (RuntimeFault e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        si.getServerConnection().logout();
    }

}


//below are from homework1
//For each datastore, print out its name, capacity, and available space
//                ManagedEntity[] ESXdatastoreEntities = hostSystemObj.getDatastores();
//                int i = 0;
//                for(ManagedEntity dse : ESXdatastoreEntities){
//
//                    Datastore datastore = (Datastore)dse;
//                    System.out.print("DataStore[" + i + "]: name = " + datastore.getName());
//                    System.out.print(" , capacity = " + datastore.getInfo().getMaxVirtualDiskCapacity());
//                    System.out.println(" , FreeSpace = " + datastore.getInfo().freeSpace);
//                    i++;
//                }
//Enumerate all networks of the host. print each network name
//                ManagedObject[] ESXnetworkObjects = hostSystemObj.getNetworks();
//                int j = 0;
//                for(ManagedObject nwo : ESXnetworkObjects){
//                    Network network = (Network) nwo;
//                    System.out.println("Network[" + j + "]: name = " +network.getName());
//                    j++;
//                }

//VirtualMachinePowerState.poweredOff()
//                if(vm.getRuntime().getPowerState().toString().equals("poweredOn")){
//                    vm.powerOffVM_Task();
//                    int len = vm.getRecentTasks().length;
//                    System.out.println("Power off vm: status = " + vm.getRecentTasks()[len - 1].getTaskInfo().getState().toString());
//                    System.out.print("task: target=" + vm.getName() + ", op = " + vm.getRecentTasks()[len - 1].getTaskInfo().getName());
//                    System.out.println(", startTime = " + vm.getRecentTasks()[len-1].getTaskInfo().getStartTime().getTime());
//
//                }else{
//                    vm.powerOnVM_Task(myhost);
//                    int len = vm.getRecentTasks().length;
//                    System.out.println("Power on vm: status = " + vm.getRecentTasks()[len-1].getTaskInfo().getState().toString());
//                    System.out.print("task: target=" + vm.getName() + ", op = " + vm.getRecentTasks()[len - 1].getTaskInfo().getName());
//                    System.out.println(" ,startTime = " + vm.getRecentTasks()[len-1].getTaskInfo().getStartTime().getTime());
//                }

//                        int m = 0;
//                        for (Task task : tasks) {
//                            System.out.println("task[" + m + "]: " + "operation name: " + task.getTaskInfo().getName() +
//                                    ", startTime: " + task.getTaskInfo().getStartTime().getTime() +
//                                    ", state: " + task.getTaskInfo().getState().toString());
//                            m++;
//                        }