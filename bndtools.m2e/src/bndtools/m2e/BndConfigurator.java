package bndtools.m2e;

import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class BndConfigurator extends AbstractProjectConfigurator {

    @Override
    public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {}

    @Override
    public AbstractBuildParticipant getBuildParticipant(final IMavenProjectFacade projectFacade, MojoExecution execution, IPluginExecutionMetadata executionMetadata) {
        return new MojoExecutionBuildParticipant(execution, true, true) {
            @Override
            public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
                final Set<IProject> build = super.build(kind, monitor);

                // must do this in a workspace jar, doing it during existing m2e build will throw lifecycle errors
                new WorkspaceJob("rebuild jar") {
                    @Override
                    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                        MavenUtil.execJarMojo(projectFacade.getMavenProject(monitor), monitor);
                        projectFacade.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);

                        return Status.OK_STATUS;
                    }
                }.schedule();

                return build;
            }
        };
    }

}
