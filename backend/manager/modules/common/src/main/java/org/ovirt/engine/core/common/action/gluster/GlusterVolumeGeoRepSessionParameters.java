package org.ovirt.engine.core.common.action.gluster;

import org.ovirt.engine.core.compat.Guid;

public class GlusterVolumeGeoRepSessionParameters extends GlusterVolumeParameters{

    private static final long serialVersionUID = -881348048838907389L;

    private Guid geoRepSessionId;
    private String slaveVolumeName;
    private String slaveHost;
    private String userName;
    private String userGroup;
    private boolean force;

    public GlusterVolumeGeoRepSessionParameters() {
        super();
    }

    public GlusterVolumeGeoRepSessionParameters(Guid volumeId, Guid geoRepSessionId) {
        super(volumeId);
        this.geoRepSessionId = geoRepSessionId;
    }

    public GlusterVolumeGeoRepSessionParameters(Guid volumeId, String slaveVolumeName, String slaveHost) {
        this(volumeId, slaveVolumeName, slaveHost, "root", null, false);
    }

    public GlusterVolumeGeoRepSessionParameters(Guid volumeId,
            String slaveVolumeName,
            String slaveHost,
            String userName,
            String userGroup,
            boolean force) {
        super(volumeId);
        this.slaveVolumeName = slaveVolumeName;
        this.slaveHost = slaveHost;
        this.userName = userName;
        this.userGroup = userGroup;
        this.force = force;
    }

    public GlusterVolumeGeoRepSessionParameters(Guid volumeId, String slaveVolumeName,
            String slaveHost,
            String userName,
            String userGroup) {
        this(volumeId, slaveVolumeName, slaveHost, userName, userGroup, false);
    }

    public String getSlaveVolumeName() {
        return slaveVolumeName;
    }

    public void setSlaveVolumeName(String slaveVolumeName) {
        this.slaveVolumeName = slaveVolumeName;
    }

    public String getSlaveHost() {
        return slaveHost;
    }

    public void setSlaveHost(String slaveHost) {
        this.slaveHost = slaveHost;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public Guid getGeoRepSessionId() {
        return geoRepSessionId;
    }

    public void setGeoRepSessionId(Guid geoRepSessionId) {
        this.geoRepSessionId = geoRepSessionId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(String userGroup) {
        this.userGroup = userGroup;
    }
}
