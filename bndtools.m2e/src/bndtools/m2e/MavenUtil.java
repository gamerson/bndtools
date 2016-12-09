package bndtools.m2e;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;

public class MavenUtil {

    public static final String JAR_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    public static final String JAR_PLUGIN_ARTIFACT_ID = "maven-jar-plugin";

    static void execJarMojo(MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
        final IMaven maven = MavenPlugin.getMaven();

        final MavenExecutionPlan plan = maven.calculateExecutionPlan(mavenProject, Arrays.asList("jar:jar"), true, monitor);
        final List<MojoExecution> mojoExecutions = plan.getMojoExecutions();

        if (!mojoExecutions.isEmpty()) {
            Iterator<MojoExecution> mojos = mojoExecutions.iterator();

            while (mojos.hasNext()) {
                MojoExecution mojo = mojos.next();
                MavenPlugin.getMaven().execute(mavenProject, mojo, monitor);
            }
        }
    }
}
