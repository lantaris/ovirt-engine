package org.ovirt.engine.core.bll.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CompensationContext;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskType;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.utils.VmDeviceCommonUtils;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VmDeviceDAO;

public class VmDeviceUtils {
    private final static String LOW_VIDEO_MEM = "32768";
    private final static String HIGH_VIDEO_MEM = "65536";
    private static VmBase vmBaseInstance;
    private static VmDeviceDAO dao = DbFacade.getInstance().getVmDeviceDAO();
    /**
     * Update the vm devices according to changes made in vm static for existing VM
     */
    public static <T extends VmBase> void updateVmDevices(T entity, VmBase oldVmBase) {
        VmBase newVmBase = getBaseObject(entity, oldVmBase.getId());
        if (newVmBase != null) {
            updateCdInVmDevice(oldVmBase, newVmBase);
            if (oldVmBase.getdefault_boot_sequence() != newVmBase
                    .getdefault_boot_sequence()) {
                updateBootOrderInVmDevice(newVmBase);
            }
            if (oldVmBase.getnum_of_monitors() != newVmBase
                    .getnum_of_monitors()) {
                updateNumOfMonitorsInVmDevice(oldVmBase, newVmBase);
            }
        }
    }

    /**
     * Update the vm devices according to changes made in vm static for new VM
     */

    public static <T extends VmBase> void updateVmDevices(T entity, Guid newId) {
        VmBase newVmBase = getBaseObject(entity, newId);
        if (newVmBase != null) {
            updateCdInVmDevice(newVmBase);
            updateBootOrderInVmDevice(newVmBase);
            updateNumOfMonitorsInVmDevice(null, newVmBase);
        }
    }

    /**
     * Copies relevamt entries on Vm from Template or Tempalte from VM creation.
     * @param srcId
     * @param dstId
     */
    public static  void copyVmDevices(Guid srcId, Guid dstId) {
        Guid id;
        VmBase VmBase = DbFacade.getInstance().getVmStaticDAO().get(dstId);
        List<DiskImage> disks = DbFacade.getInstance().getDiskImageDAO().getAllForVm(dstId);
        List<VmNetworkInterface> ifaces;
        int diskCount=0;
        int ifaceCount=0;
        boolean isVm = (VmBase != null);
        if (isVm) {
            ifaces = DbFacade.getInstance().getVmNetworkInterfaceDAO().getAllForVm(dstId);
        }
        else {
            ifaces = DbFacade.getInstance().getVmNetworkInterfaceDAO().getAllForTemplate(dstId);
        }
        List<VmDevice> devices = dao.getVmDeviceByVmId(srcId);
        for (VmDevice device : devices) {
            id = Guid.NewGuid();
            if (srcId.equals(Guid.Empty)) {
                // only update number of monitors if this is a desktop
                if (VmBase.getvm_type() == VmType.Desktop) {
                    updateNumOfMonitorsInVmDevice(null, VmBase);
                }
                continue; // skip Blank template devices
            }
            if (device.getType().equalsIgnoreCase(VmDeviceType.DISK.name()) && device.getDevice().equalsIgnoreCase(VmDeviceType.DISK.name())) {
                if (diskCount < disks.size()) {
                    id = (disks.get(diskCount++)).getimage_group_id();
                }
            }
            else if (device.getType().equalsIgnoreCase(VmDeviceType.INTERFACE.name())) {
                if (ifaceCount < ifaces.size()) {
                    id = ifaces.get(ifaceCount++).getId();
                }
            }
            device.setId(new VmDeviceId(id, dstId));
            dao.save(device);
        }
        // if destination is a VM , update devices boot order
        if (isVm) {
            updateBootOrderInVmDevice(VmBase);
        }
    }

    /**
     * adds managed device to vm_device
     * @param id
     * @param type
     * @param device
     */
    public static void addManagedDevice(VmDeviceId id, VmDeviceType type, VmDeviceType device, String specParams, boolean is_plugged, boolean isReadOnly) {
        VmDevice managedDevice =
            new VmDevice(id,
                    VmDeviceType.getName(type),
                    VmDeviceType.getName(device),
                    "",
                    0,
                    specParams,
                    true,
                    is_plugged,
                    isReadOnly);
        dao.save(managedDevice);
    }

    public static void addManagedDevice(CompensationContext ctx,VmDeviceId id, VmDeviceType type, VmDeviceType device, String specParams, boolean is_plugged, boolean isReadOnly) {
        VmDevice managedDevice =
            new VmDevice(id,
                    VmDeviceType.getName(type),
                    VmDeviceType.getName(device),
                    "",
                    0,
                    specParams,
                    true,
                    is_plugged,
                    isReadOnly);
        dao.save(managedDevice);
        if (ctx != null) {
            ctx.snapshotNewEntity(managedDevice);
        }
    }

    /**
     * adds imported VM or Template devices
     * @param entity
     * @param id
     */
    public static <T extends VmBase> void addImportedDevices(T entity, Guid id) {
        addImportedDisks(entity);
        addImportedInterfaces(entity);
        updateVmDevices(entity, id);
    }

    /**
     * set device Id in special parameters
     *
     * @param deviceId
     * @param specParams
     * @return
     */
    public static String appendDeviceIdToSpecParams(Guid deviceId, String specParams) {
        final String SEP = ",";
        StringBuilder sb = new StringBuilder();
        if (specParams.length() > 0) {
            sb.append(specParams);
            sb.append(SEP);
        }
        sb.append("deviceId=");
        sb.append(deviceId);
        return sb.toString();
    }

    /**
     * updates existing VM CD ROM in vm_device
     *
     * @param oldVmBase
     * @param newVmBase
     *            NOTE : Only one CD is currently supported.
     */
    private static void updateCdInVmDevice(VmBase oldVmBase,
            VmBase newVmBase) {
        String newIsoPath = newVmBase.getiso_path();
        String oldIsoPath = oldVmBase.getiso_path();

        if (StringUtils.isEmpty(oldIsoPath) && StringUtils.isNotEmpty(newIsoPath)) {
            // new CD was added
            VmDevice cd = new VmDevice(new VmDeviceId(Guid.NewGuid(),
                    newVmBase.getId()),
                    VmDeviceType.getName(VmDeviceType.DISK),
                    VmDeviceType.getName(VmDeviceType.CDROM), "", 0,
                    newIsoPath, true, false, false);
            dao.save(cd);
        } else {
            if (StringUtils.isNotEmpty(oldIsoPath) && StringUtils.isEmpty(newIsoPath)) {
                // existing CD was removed
                List<VmDevice> list = DbFacade
                        .getInstance()
                        .getVmDeviceDAO()
                        .getVmDeviceByVmIdTypeAndDevice(newVmBase.getId(),
                                VmDeviceType.getName(VmDeviceType.DISK),
                                VmDeviceType.getName(VmDeviceType.CDROM));
                dao.remove(list.get(0).getId());
            } else if (StringUtils.isNotEmpty(oldIsoPath) && StringUtils.isNotEmpty(newIsoPath)
                    && !oldIsoPath.equals(newIsoPath)) {
                // CD was changed
                List<VmDevice> list = DbFacade
                        .getInstance()
                        .getVmDeviceDAO()
                        .getVmDeviceByVmIdTypeAndDevice(newVmBase.getId(),
                                VmDeviceType.getName(VmDeviceType.DISK),
                                VmDeviceType.getName(VmDeviceType.CDROM));
                VmDevice cd = list.get(0);
                cd.setSpecParams(newIsoPath);
                dao.update(cd);
            }
        }
    }

    /**
     * updates new VM CD ROM in vm_device
     * @param newVmBase
     */

    private static void updateCdInVmDevice(VmBase newVmBase) {
        if (StringUtils.isNotEmpty(newVmBase.getiso_path())) {
            // new CD was added
            VmDevice cd = new VmDevice(new VmDeviceId(Guid.NewGuid(),
                    newVmBase.getId()), VmDeviceType.getName(VmDeviceType.DISK),
                    VmDeviceType.getName(VmDeviceType.CDROM), "", 0,
                    newVmBase.getiso_path(), true, false, false);
            dao.save(cd);
        }
    }

    /**
     * Updates VM boot order in vm device according to the BootSequence enum value.
     * @param vmBase
     */
    private static void updateBootOrderInVmDevice(VmBase vmBase) {
        if (vmBase instanceof VmStatic) {
            vmBaseInstance = vmBase;
            List<VmDevice> devices = dao.getVmDeviceByVmId(vmBase.getId());
            int bootOrder = 1;
            switch (vmBase.getdefault_boot_sequence()) {
            case C:
                bootOrder = setDiskBootOrder(devices, bootOrder);
                break;
            case CD:
                bootOrder = setDiskBootOrder(devices, bootOrder);
                bootOrder = setCDBootOrder(devices, bootOrder);
                break;
            case CDN:
                bootOrder = setDiskBootOrder(devices, bootOrder);
                bootOrder = setCDBootOrder(devices, bootOrder);
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                break;
            case CN:
                bootOrder = setDiskBootOrder(devices, bootOrder);
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                break;
            case CND:
                bootOrder = setDiskBootOrder(devices, bootOrder);
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                bootOrder = setCDBootOrder(devices, bootOrder);
                break;
            case D:
                bootOrder = setCDBootOrder(devices, bootOrder);
                break;
            case DC:
                bootOrder = setCDBootOrder(devices, bootOrder);
                bootOrder = setDiskBootOrder(devices, bootOrder);
                break;
            case DCN:
                bootOrder = setCDBootOrder(devices, bootOrder);
                bootOrder = setDiskBootOrder(devices, bootOrder);
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                break;
            case DN:
                bootOrder = setCDBootOrder(devices, bootOrder);
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                break;
            case DNC:
                bootOrder = setCDBootOrder(devices, bootOrder);
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                bootOrder = setDiskBootOrder(devices, bootOrder);
                break;
            case N:
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                break;
            case NC:
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                bootOrder = setDiskBootOrder(devices, bootOrder);
                break;
            case NCD:
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                bootOrder = setDiskBootOrder(devices, bootOrder);
                bootOrder = setCDBootOrder(devices, bootOrder);
                break;
            case ND:
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                bootOrder = setCDBootOrder(devices, bootOrder);
                break;
            case NDC:
                bootOrder = setNetworkBootOrder(devices, bootOrder);
                bootOrder = setCDBootOrder(devices, bootOrder);
                bootOrder = setDiskBootOrder(devices, bootOrder);
                break;
            }
            // update boot order in vm device
            for (VmDevice device : devices) {
                dao.update(device);
            }
        }
    }

    /**
     * updates network devices boot order
     * @param devices
     * @param bootOrder
     * @return
     */
    private static int setNetworkBootOrder(List<VmDevice> devices, int bootOrder) {
        for (VmDevice device : devices) {
            if (device.getType().equals(
                    VmDeviceType.getName(VmDeviceType.INTERFACE))
                    && device.getDevice().equals(
                            VmDeviceType.getName(VmDeviceType.BRIDGE))) {
                device.setBootOrder(bootOrder++);
            }
        }
        return bootOrder;
    }

    /**
     * updates CD boot order
     * @param devices
     * @param bootOrder
     * @return
     */
    private static int setCDBootOrder(List<VmDevice> devices, int bootOrder) {
        for (VmDevice device : devices) {
            if (device.getType()
                    .equals(VmDeviceType.getName(VmDeviceType.DISK))
                    && device.getDevice().equals(
                            VmDeviceType.getName(VmDeviceType.CDROM))) {
                device.setBootOrder(bootOrder++);
                break; // only one CD is currently supported.
            }
        }
        return bootOrder;
    }

    /**
     * updates disk boot order
     * @param devices
     * @param bootOrder
     * @return
     */
    private static int setDiskBootOrder(List<VmDevice> devices, int bootOrder) {
        VM vm = DbFacade.getInstance().getVmDAO().get(vmBaseInstance.getId());
        boolean isOldCluster = VmDeviceCommonUtils.isOldClusterVersion(vm);
        for (VmDevice device : devices) {
            if (device.getType()
                    .equals(VmDeviceType.getName(VmDeviceType.DISK))
                    && device.getDevice().equals(
                            VmDeviceType.getName(VmDeviceType.DISK))) {
                Guid id = device.getDeviceId();
                Disk disk = DbFacade.getInstance().getDiskDao().get(id);
                if (id != null && !id.equals(Guid.Empty)) {
                    if (isOldCluster) { // Only one system disk can be bootable in
                                        // old version.
                        if (disk != null && disk.getDiskType().equals(DiskType.System)) {
                            device.setBootOrder(bootOrder++);
                            break;
                        }
                    } else { // supporting more than 1 bootable disk in 3.1 and up.
                        device.setBootOrder(bootOrder++);
                    }
                }
            }
        }
        return bootOrder;
    }

    /**
     * updates existing VM video cards in vm device
     * @param oldVmBase
     * @param newStatic
     */
    private static void updateNumOfMonitorsInVmDevice(VmBase oldVmBase,
            VmBase newStatic) {
        int prevNumOfMonitors=0;
        if (oldVmBase != null) {
            prevNumOfMonitors = oldVmBase.getnum_of_monitors();
        }
        if (newStatic.getnum_of_monitors() > prevNumOfMonitors) {
            Guid newId = Guid.NewGuid();
            String mem = (newStatic.getnum_of_monitors() > 2 ? LOW_VIDEO_MEM
                    : HIGH_VIDEO_MEM);
            StringBuilder sb = new StringBuilder();
            sb.append("vram=");
            sb.append(mem);
            sb.append(",deviceId=");
            sb.append(newId);
            // monitors were added
            for (int i = prevNumOfMonitors; i < newStatic
                    .getnum_of_monitors(); i++) {
                VmDevice cd = new VmDevice(new VmDeviceId(newId,
                        newStatic.getId()),
                        VmDeviceType.getName(VmDeviceType.VIDEO),
                        DisplayType.qxl.name(), "", 0, sb.toString(), true, false, false);
                dao.save(cd);
            }
        } else { // delete video cards
            List<VmDevice> list = DbFacade
                    .getInstance()
                    .getVmDeviceDAO()
                    .getVmDeviceByVmIdAndType(newStatic.getId(),
                            VmDeviceType.getName(VmDeviceType.VIDEO));
            for (int i = 0; i < (prevNumOfMonitors - newStatic
                    .getnum_of_monitors()); i++) {
                dao.remove(list.get(i).getId());
            }
        }
    }

    /**
     * Returns a VmBase object for the given entity and passed id.
     * @param entity
     *            the entity, may be VmStatic or VmTemplate
     * @param newId
     *            entity Guid
     * @return
     */
    private static <T extends VmBase> VmBase getBaseObject(T entity, Guid newId) {
        VmBase newVmBase = null;
        if (entity instanceof VmStatic) {
            newVmBase = DbFacade.getInstance().getVmDAO().get(newId).getStaticData();
        } else if (entity instanceof VmTemplate) {
            newVmBase = DbFacade.getInstance().getVmTemplateDAO().get(newId);
        }
        return newVmBase;
    }

    /**
     * Adds imported disks to VM devices
     * @param entity
     */
    private static <T extends VmBase> void addImportedDisks(T entity) {
        Guid id=Guid.Empty;
        List<DiskImage> disks = new ArrayList<DiskImage>();
        id = entity.getId();
        if (entity instanceof VmStatic) {
            disks = DbFacade.getInstance().getDiskImageDAO().getAllForVm(id);
        }
        else if (entity instanceof VmTemplate) {
            disks = ((VmTemplate)entity).getDiskList();
        }
        for (DiskImage disk : disks) {
            Guid deviceId = disk.getDisk().getId();
            String specParams = appendDeviceIdToSpecParams(deviceId, "");
            addManagedDevice(new VmDeviceId(deviceId,id) , VmDeviceType.DISK, VmDeviceType.DISK, specParams, true, false);
        }
    }

    /**
     * Adds imported interfaces to VM devices
     * @param entity
     */
    private static <T extends VmBase> void addImportedInterfaces(T entity) {
        Guid id=Guid.Empty;
        List<VmNetworkInterface> ifaces = new ArrayList<VmNetworkInterface>();
        id = entity.getId();
        if (entity instanceof VmStatic) {
            ifaces = DbFacade.getInstance().getVmNetworkInterfaceDAO().getAllForVm(id);
        }
        else if (entity instanceof VmTemplate) {
            ifaces = ((VmTemplate)entity).getInterfaces();
        }
        for (VmNetworkInterface iface : ifaces) {
            Guid deviceId = iface.getId();
            String specParams = appendDeviceIdToSpecParams(deviceId, "");
            addManagedDevice(new VmDeviceId(deviceId,id) , VmDeviceType.INTERFACE, VmDeviceType.BRIDGE, specParams, true, false);
        }
    }
}
