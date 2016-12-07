package bndtools.m2e;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.collections.SortedList;

public class MavenBuildRepository extends BaseRepository implements Repository, RepositoryPlugin, Refreshable {

    private final IMavenProjectRegistry mavenProjectRegistry = MavenPlugin.getMavenProjectRegistry();

    @Override
    public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
        return null;
    }

    @Override
    public boolean refresh() throws Exception {
        return false;
    }

    @Override
    public File getRoot() throws Exception {
        return null;
    }

    @Override
    public PutResult put(InputStream stream, PutOptions options) throws Exception {
        return null;
    }

    @Override
    public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners) throws Exception {
        File returnVal = null;

        final IProgressMonitor monitor = new NullProgressMonitor();

        for (IMavenProjectFacade projectFacade : mavenProjectRegistry.getProjects()) {
            final MavenProject mavenProject = projectFacade.getMavenProject(monitor);

            final File bundleFile = new File(mavenProject.getBuild().getDirectory() + "/" + mavenProject.getBuild().getFinalName() + ".jar");

            if (bundleFile.exists()) {
                try (Jar jar = new Jar(bundleFile)) {
                    if (bsn.equals(jar.getBsn())) {
                        returnVal = bundleFile;
                        break;
                    }
                }
            }

            final File outputDir = new File(mavenProject.getBuild().getOutputDirectory());

            if (outputDir.exists()) {
                try (Jar jar = new Jar(outputDir)) {
                    if (bsn.equals(jar.getBsn())) {
                        // build the jar
                        List<MojoExecution> jarMojo = projectFacade.getMojoExecutions("org.apache.maven.plugins", "maven-jar-plugin", monitor);
                        MavenPlugin.getMaven().execute(mavenProject, jarMojo.get(0), monitor);

                        if (bundleFile.exists()) {
                            returnVal = bundleFile;
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (returnVal != null && returnVal.exists()) {
            if (listeners != null && listeners.length > 0) {
                for (DownloadListener listener : listeners) {
                    try {
                        listener.success(returnVal);
                    } catch (Throwable t) {}
                }
            }
        }

        return returnVal;
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
        return "maven build";
    }

    @Override
    public String getLocation() {
        return null;
    }

}
