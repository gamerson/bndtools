package org.bndtools.api;

import aQute.bnd.build.Project;

public interface LaunchListener {

    void buildForLaunch(Project project) throws Exception;

    void cleanup(Project project) throws Exception;

}
