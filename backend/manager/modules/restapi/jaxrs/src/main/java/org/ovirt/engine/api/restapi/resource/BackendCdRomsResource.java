package org.ovirt.engine.api.restapi.resource;

import static org.ovirt.engine.api.restapi.types.CdRomMapper.CDROM_ID;

import java.util.List;

import org.ovirt.engine.api.common.util.QueryHelper;
import org.ovirt.engine.api.model.Cdrom;
import org.ovirt.engine.api.model.Cdroms;
import org.ovirt.engine.api.resource.DeviceResource;
import org.ovirt.engine.api.resource.DevicesResource;
import org.ovirt.engine.api.restapi.resource.AbstractBackendSubResource.ParametersProvider;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendCdRomsResource
        extends AbstractBackendDevicesResource<Cdrom, Cdroms, VM>
        implements DevicesResource<Cdrom, Cdroms> {

    public BackendCdRomsResource(Guid parentId,
                                 VdcQueryType queryType,
                                 VdcQueryParametersBase queryParams) {
        super(Cdrom.class,
              Cdroms.class,
              VM.class,
              parentId,
              queryType,
              queryParams,
              VdcActionType.UpdateVm,
              VdcActionType.UpdateVm);
    }

    @Override
    protected <T> boolean matchEntity(VM entity, T id) {
        return (id == null || id.equals(CDROM_ID)) && parentId.equals(entity.getQueryableId());
    }

    @Override
    protected boolean matchEntity(VM entity, String name) {
        return false;
    }

    @Override
    protected String[] getRequiredAddFields() {
        return new String[] { "file.id" };
    }

    @Override
    protected String[] getRequiredUpdateFields() {
        return new String[] { "file" };
    }

    @Override
    protected VdcActionParametersBase getAddParameters(VM mapped, Cdrom cdrom) {
        return new VmManagementParametersBase(getUpdatable(mapped.getStaticData().getIsoPath()));
    }

    @Override
    protected Cdroms mapCollection(List<VM> entities) {
        if (QueryHelper.hasCurrentConstraint(getUriInfo())) {
            for (VM entity : entities) {
                // change the iso path so the result of 'map' will contain current cd instead of the
                // persistent configuration
                entity.setIsoPath(entity.getCurrentCd());
            }
        }
        return super.mapCollection(entities);
    }

    protected VmStatic getUpdatable(String isoPath) {
        VmStatic updatable = getEntity(VM.class,
                                       VdcQueryType.GetVmByVmId,
                                       new IdQueryParameters(parentId),
                                       parentId.toString()).getStaticData();
        updatable.setIsoPath(isoPath);
        return updatable;
    }

    @Override
    protected ParametersProvider<Cdrom, VM> getUpdateParametersProvider() {
        return new UpdateParametersProvider();
    }

    protected class UpdateParametersProvider implements ParametersProvider<Cdrom, VM> {
        @Override
        public VdcActionParametersBase getParameters(Cdrom incoming, VM entity) {
            return new VmManagementParametersBase(getUpdatable(incoming.getFile().getId()));
        }
    }

    @Override
    public DeviceResource<Cdrom> getDeviceSubResource(String id) {
        return inject(
            new BackendCdRomResource(
                parentId,
                asGuidOr404(id),
                this,
                updateType,
                getUpdateParametersProvider(),
                getRequiredUpdateFields()
            )
        );
    }
}
