package org.bndtools.api;

import org.eclipse.core.resources.IResource;

import aQute.bnd.build.Run;

public interface RunListener {

    void create(Run run, IResource targetResource) throws Exception;

    void end(Run run) throws Exception;

}
