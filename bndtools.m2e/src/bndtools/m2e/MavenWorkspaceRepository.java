package bndtools.m2e;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.builder.BuildDebugHook;
import org.eclipse.m2e.core.internal.builder.MavenBuilder;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.collections.SortedList;

public class MavenWorkspaceRepository extends BaseRepository implements BuildDebugHook, RepositoryPlugin, Refreshable {

    private final IMavenProjectRegistry mavenProjectRegistry = MavenPlugin.getMavenProjectRegistry();

    public MavenWorkspaceRepository() throws Exception {
        MavenBuilder.addDebugHook(this);
    }

    @Override
    public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
        return null;
    }

    @Override
    public void buildStart(IMavenProjectFacade projectFacade, int kind, Map<String,String> args, Map<MojoExecutionKey,List<AbstractBuildParticipant>> participants, IResourceDelta delta, IProgressMonitor monitor) {}

    @Override
    public void buildParticipant(final IMavenProjectFacade projectFacade, MojoExecutionKey mojoExecutionKey, AbstractBuildParticipant participant, Set<File> files, IProgressMonitor monitor) {
        final String arifactId = mojoExecutionKey.getArtifactId();
        final String goal = mojoExecutionKey.getGoal();

        if (BndMavenPluginConstants.BND_MAVEN_PLUGIN_ARTIFACT_ID.equals(arifactId) && BndMavenPluginConstants.BND_MAVEN_PLUGIN_GOAL.equals(goal)) {
            try {
                IMaven maven = MavenPlugin.getMaven();
                MavenProject mavenProject = projectFacade.getMavenProject(monitor);
                final MavenExecutionPlan plan = maven.calculateExecutionPlan(mavenProject, Arrays.asList("jar:jar"), true, monitor);
                final List<MojoExecution> mojos = plan.getMojoExecutions();

                for (MojoExecution mojo : mojos) {
                    maven.execute(mavenProject, mojo, monitor);
                }

                projectFacade.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean refresh() throws Exception {
        return false;
    }

    @Override
    public File getRoot() throws Exception {
        File root = new File(".");
        return root;
    }

    @Override
    public PutResult put(InputStream stream, PutOptions options) throws Exception {
        return null;
    }

    @Override
    public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners) throws Exception {
        for (IMavenProjectFacade projectFacade : mavenProjectRegistry.getProjects()) {
            MavenProject mavenProject = projectFacade.getMavenProject(new NullProgressMonitor());

            File outputDir = new File(mavenProject.getBuild().getOutputDirectory());

            if (outputDir.exists()) {
                try (Jar jar = new Jar(outputDir)) {
                    if (bsn.equals(jar.getBsn())) {
                        List<MojoExecution> jarMojo = projectFacade.getMojoExecutions("org.apache.maven.plugins", "maven-jar-plugin", new NullProgressMonitor(), "jar");
                        MavenPlugin.getMaven().execute(mavenProject, jarMojo.get(0), new NullProgressMonitor());
                        File bundleFile = new File(mavenProject.getBuild().getDirectory() + "/" + mavenProject.getBuild().getFinalName() + ".jar");

                        if (bundleFile.exists()) {
                            if (listeners != null && listeners.length > 0) {
                                for (DownloadListener listener : listeners) {
                                    try {
                                        listener.success(bundleFile);
                                    } catch (Throwable t) {}
                                }
                            }

                            return bundleFile;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public List<String> list(String pattern) throws Exception {
        return null;
    }

    @Override
    public SortedSet<Version> versions(String bsn) throws Exception {
        List<Version> versions = new ArrayList<>();

        for (IMavenProjectFacade projectFacade : mavenProjectRegistry.getProjects()) {
            File outputDir = new File(projectFacade.getMavenProject(new NullProgressMonitor()).getBuild().getOutputDirectory());

            if (outputDir.exists()) {
                try (Jar jar = new Jar(outputDir)) {
                    if (bsn.equals(jar.getBsn())) {
                        Version version = new Version(jar.getVersion());
                        versions.add(version);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (versions.isEmpty()) {
            return SortedList.empty();
        }

        return new SortedList<Version>(versions);
    }

    @Override
    public String getName() {
        return "Maven Workspace";
    }

    @Override
    public String getLocation() {
        return null;
    }

    public void cleanup() {
        MavenBuilder.removeDebugHook(this);
    }

}
