package org.ovirt.engine.api.restapi.resource;

import static org.easymock.EasyMock.expect;
import static org.ovirt.engine.api.restapi.resource.AbstractBackendCdRomsResourceTest.CURRENT_ISO_PATH;
import static org.ovirt.engine.api.restapi.resource.AbstractBackendCdRomsResourceTest.ISO_PATH;
import static org.ovirt.engine.api.restapi.resource.AbstractBackendCdRomsResourceTest.PARENT_ID;
import static org.ovirt.engine.api.restapi.resource.AbstractBackendCdRomsResourceTest.setUpEntityExpectations;
import static org.ovirt.engine.api.restapi.resource.AbstractBackendCdRomsResourceTest.verifyModelWithIso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.ovirt.engine.api.model.Cdrom;
import org.ovirt.engine.api.model.Cdroms;
import org.ovirt.engine.api.model.File;
import org.ovirt.engine.core.common.action.ChangeDiskCommandParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendCdRomResourceTest
        extends AbstractBackendSubResourceTest<Cdrom, VM, BackendCdRomResource> {

    protected static final String EJECT_ISO = "";
    protected static BackendCdRomsResource collection = getCollection();

    public BackendCdRomResourceTest() {
        super(getResource(GUIDS[0]));
    }

    protected static BackendCdRomResource getResource(Guid id) {
        return new BackendCdRomResource(
            PARENT_ID,
            id,
            collection,
            VdcActionType.UpdateVm,
            collection.getUpdateParametersProvider(),
            collection.getRequiredUpdateFields()
        );
    }

    protected BackendDeviceResource<Cdrom, Cdroms, VM> getNotFoundResource() {
        BackendDeviceResource<Cdrom, Cdroms, VM> ret = getResource(new Guid("0d0264ef-40de-45a1-b746-83a0088b47a7"));
        ret.setUriInfo(setUpBasicUriExpectations());
        initResource(ret);
        initResource(ret.getCollection());
        return ret;
    }

    protected static BackendCdRomsResource getCollection() {
       return new BackendCdRomsResource(PARENT_ID,
                                        VdcQueryType.GetVmByVmId,
                                        new IdQueryParameters(PARENT_ID));
    }

    protected void init() {
        super.init();
        initResource(resource.getCollection());
    }

    @Test
    public void testGetNotFound() throws Exception {
        BackendDeviceResource<Cdrom, Cdroms, VM> resource = getNotFoundResource();
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(1);
        control.replay();
        try {
            resource.get();
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testGet() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(1);
        control.replay();

        Cdrom cdrom = resource.get();
        verifyModel(cdrom);
    }

    @Test
    public void testGetCurrent() throws Exception {
        setUriInfo(setUpUriMatrixExpectations(null));
        setUpEntityQueryExpectations(1);
        control.replay();

        Cdrom cdrom = resource.get();
        verifyModelWithCurrentCd(cdrom);
    }

    @Test
    public void testGetCurrentWithMatrixTrue() throws Exception {
        setUriInfo(setUpUriMatrixExpectations("true"));
        setUpEntityQueryExpectations(1);
        control.replay();

        Cdrom cdrom = resource.get();
        verifyModelWithCurrentCd(cdrom);
    }

    @Test
    public void testGetCurrentWithMatrixFalse() throws Exception {
        setUriInfo(setUpUriMatrixExpectations("false"));
        setUpEntityQueryExpectations(1);
        control.replay();

        Cdrom cdrom = resource.get();
        verifyModel(cdrom);
    }

    @Test
    public void testGetCurrentWithGarbledMatrixReturnsCurrent() throws Exception {
        setUriInfo(setUpUriMatrixExpectations("faSLe"));
        setUpEntityQueryExpectations(1);
        control.replay();

        Cdrom cdrom = resource.get();
        verifyModelWithCurrentCd(cdrom);
    }

    @Test
    public void testChangeCdNotFound() throws Exception {
        BackendDeviceResource<Cdrom, Cdroms, VM> resource = getNotFoundResource();
        setUriInfo(setUpBasicUriExpectations());
        resource.setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(1);
        control.replay();
        try {
            resource.update(getUpdate());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testEjectCd() throws Exception {
        setUriInfo(setUpChangeCdUriMatrixExpectations());
        setUpGetEntityExpectations(1, VMStatus.Up);
        setUpActionExpectations(VdcActionType.ChangeDisk,
                                ChangeDiskCommandParameters.class,
                                new String[] {"CdImagePath"},
                                new Object[] {EJECT_ISO},
                                true,
                                true);
        Cdrom update = getUpdate();
        update.getFile().setId(EJECT_ISO);
        Cdrom cdrom = resource.update(update);
        assertEquals(EJECT_ISO, cdrom.getFile().getId());
    }

    @Test
    public void testChangeCdUsingQueryParameter() throws Exception {
        resource.setUriInfo(setUpChangeCdUriQueryExpectations());
        setUpGetEntityExpectations(1, VMStatus.Up);
        setUpActionExpectations(VdcActionType.ChangeDisk,
                                ChangeDiskCommandParameters.class,
                                new String[] {"CdImagePath"},
                                new Object[] {ISO_PATH},
                                true,
                                true);
        Cdrom cdrom = resource.update(getUpdate());
        assertTrue(cdrom.isSetFile());
    }

    @Test
    public void testChangeCdUsingMatrixParameter() throws Exception {
        resource.setUriInfo(setUpChangeCdUriMatrixExpectations());
        setUpGetEntityExpectations(1, VMStatus.Up);
        setUpActionExpectations(VdcActionType.ChangeDisk,
                                ChangeDiskCommandParameters.class,
                                new String[] {"CdImagePath"},
                                new Object[] {ISO_PATH},
                                true,
                                true);
        Cdrom cdrom = resource.update(getUpdate());
        assertTrue(cdrom.isSetFile());
    }

    protected UriInfo setUpChangeCdUriQueryExpectations() {
        UriInfo uriInfo = setUpBasicUriExpectations();
        MultivaluedMap<String, String> queries = control.createMock(MultivaluedMap.class);
        expect(queries.containsKey("current")).andReturn(true).anyTimes();
        expect(uriInfo.getQueryParameters()).andReturn(queries).anyTimes();
        return uriInfo;
    }

    protected UriInfo setUpChangeCdUriMatrixExpectations() {
        return setUpUriMatrixExpectations(null);
    }

    protected UriInfo setUpUriMatrixExpectations(String matrixValue) {
        UriInfo uriInfo = setUpBasicUriExpectations();

        List<PathSegment> psl = new ArrayList<>();

        PathSegment ps = control.createMock(PathSegment.class);
        MultivaluedMap<String, String> matrixParams = control.createMock(MultivaluedMap.class);
        expect(matrixParams.isEmpty()).andReturn(false).anyTimes();
        expect(matrixParams.containsKey("current")).andReturn(true).anyTimes();
        expect(matrixParams.get("current")).andReturn(Collections.singletonList(matrixValue)).anyTimes();
        expect(ps.getMatrixParameters()).andReturn(matrixParams).anyTimes();

        psl.add(ps);

        expect(uriInfo.getPathSegments()).andReturn(psl).anyTimes();

        return uriInfo;
    }

    @Test
    public void testUpdateNotFound() throws Exception {
        BackendDeviceResource<Cdrom, Cdroms, VM> resource = getNotFoundResource();
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(1, VMStatus.Down);
        control.replay();
        try {
            resource.update(getUpdate());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        setUpGetEntityExpectations(3, VMStatus.Down);
        setUriInfo(setUpActionExpectations(VdcActionType.UpdateVm,
                                           VmManagementParametersBase.class,
                                           new String[] { "VmStaticData.IsoPath" },
                                           new Object[] { ISO_PATH },
                                           true,
                                           true));

        Cdrom cdrom = resource.update(getUpdate());
        assertTrue(cdrom.isSetFile());
    }

    @Test
    public void testUpdateIncompleteParameters() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        Cdrom update = new Cdrom();
        update.setFile(null);
        control.replay();
        try {
            resource.update(update);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
             verifyIncompleteException(wae, "Cdrom", "update", "file");
        }
    }

    @Test
    public void testRemove() throws Exception {
        setUpEntityQueryExpectations(
            VdcQueryType.GetVmByVmId,
            IdQueryParameters.class,
            new String[] { "Id" },
            new Object[] { PARENT_ID },
            getEntity(1)
        );
        setUriInfo(
            setUpActionExpectations(
                VdcActionType.UpdateVm,
                VmManagementParametersBase.class,
                new String[] { "VmStaticData.IsoPath" },
                new Object[] { null },
                true,
                true
            )
        );
        verifyRemove(resource.remove());
    }

    @Test
    public void testRemoveNonExistant() throws Exception{
        setUpEntityQueryExpectations(
            VdcQueryType.GetVmByVmId,
            IdQueryParameters.class,
            new String[] { "Id" },
            new Object[] { PARENT_ID },
            null
        );
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            resource.remove();
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(wae.getResponse().getStatus(), 404);
        }
    }

    @Test
    public void testRemoveCantDo() throws Exception {
        doTestBadRemove(false, true, CANT_DO);
    }

    @Test
    public void testRemoveFailed() throws Exception {
        doTestBadRemove(true, false, FAILURE);
    }

    private void doTestBadRemove(boolean canDo, boolean success, String detail) throws Exception {
        setUpEntityQueryExpectations(
            VdcQueryType.GetVmByVmId,
            IdQueryParameters.class,
            new String[]{"Id"},
            new Object[]{PARENT_ID},
            getEntity(1)
        );
        setUriInfo(
            setUpActionExpectations(
                VdcActionType.UpdateVm,
                VmManagementParametersBase.class,
                new String[] { "VmStaticData.IsoPath" },
                new Object[] { null },
                canDo,
                success
            )
        );
        try {
            resource.remove();
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    protected Cdrom getUpdate() {
        Cdrom update = new Cdrom();
        update.setFile(new File());
        update.getFile().setId(ISO_PATH);
        return update;
    }

    @Override
    protected VM getEntity(int index) {
        return setUpEntityExpectations();
    }

    protected VM getEntity(int index, VMStatus status) {
        return setUpEntityExpectations(status);
    }

    protected List<VM> getEntityList(VMStatus status) {
        List<VM> entities = new ArrayList<VM>();
        for (int i = 0; i < NAMES.length; i++) {
            if (status != null) {
                entities.add(getEntity(i, status));
            } else {
                entities.add(getEntity(i));
            }
        }
        return entities;

    }

    protected List<VM> getEntityList() {
        return getEntityList(null);

    }

    protected void setUpEntityQueryExpectations(int times, VMStatus status) throws Exception {
        while (times-- > 0) {
            setUpEntityQueryExpectations(VdcQueryType.GetVmByVmId,
                                         IdQueryParameters.class,
                                         new String[] { "Id" },
                                         new Object[] { PARENT_ID },
                                         getEntityList(status));
        }
    }

    protected void setUpEntityQueryExpectations(int times) throws Exception {
        setUpGetEntityExpectations(times, null);
    }

    protected void setUpGetEntityExpectations(int times, VMStatus status) throws Exception {
        while (times-- > 0) {
            setUpGetEntityExpectations(VdcQueryType.GetVmByVmId,
                                       IdQueryParameters.class,
                                       new String[] {"Id"},
                                       new Object[] {PARENT_ID},
                                       status != null ? getEntity(0, status)
                                               :
                                               getEntity(0));
        }
    }

    protected void verifyModel(Cdrom model) {
        verifyModelWithIso(model, ISO_PATH);
        verifyLinks(model);
    }

    protected void verifyModelWithCurrentCd(Cdrom model) {
        verifyModelWithIso(model, CURRENT_ISO_PATH);
        verifyLinks(model);
    }

}
