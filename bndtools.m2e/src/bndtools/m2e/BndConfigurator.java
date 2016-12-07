package bndtools.m2e;

import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
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
                Set<IProject> build = super.build(kind, monitor);

                //                IMaven maven = MavenPlugin.getMaven();
                MavenProject mavenProject = projectFacade.getMavenProject(monitor);
                List<MojoExecution> jarMojo = projectFacade.getMojoExecutions("org.apache.maven.plugins", "maven-jar-plugin", monitor, "jar");

                if (!jarMojo.isEmpty()) {
                    MavenPlugin.getMaven().execute(mavenProject, jarMojo.get(0), monitor);
                    projectFacade.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
                }
                //                final MavenExecutionPlan plan = maven.calculateExecutionPlan(mavenProject, Arrays.asList("jar:jar"), true, monitor);
                //                final List<MojoExecution> mojos = plan.getMojoExecutions();

                //                for (MojoExecution mojo : mojos) {
                //                    maven.execute(mavenProject, mojo, monitor);
                //                }

                return build;
            }
        };
    }
}
