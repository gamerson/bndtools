package bndtools.m2e;

import java.io.File;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import org.apache.maven.project.MavenProject;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.collections.SortedList;

public class MavenBuildRepository extends BaseRepository implements Repository, RepositoryPlugin, Refreshable, IMavenProjectChangedListener {

    private final ILogger logger = Logger.getLogger(MavenBuildRepository.class);

    private final IMavenProjectRegistry mavenProjectRegistry = MavenPlugin.getMavenProjectRegistry();
    //    private final Map<String,IMavenProjectFacade> bsnProjectFacadeMap = new HashMap<>();
    //    private final Map<String,MavenProject> bsnMavenProjectMap = new HashMap<>();

    private boolean inited = false;

    private final Map<String,Entry<IMavenProjectFacade,MavenProject>> bsnMap = new HashMap<>();

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
        if (!inited) {
            init();
        }

        File returnVal = null;

        final Entry<IMavenProjectFacade,MavenProject> entry = bsnMap.get(bsn);

        if (entry != null) {
            final MavenProject mavenProject = entry.getValue();

            final File bundleFile = new File(mavenProject.getBuild().getDirectory() + "/" + mavenProject.getBuild().getFinalName() + ".jar");

            if (!bundleFile.exists()) {
                MavenUtil.execJarMojo(mavenProject, new NullProgressMonitor());
            }

            if (bundleFile.exists()) {
                try (Jar bundle = new Jar(bundleFile)) {
                    if (bsn.equals(bundle.getBsn()) && version.compareTo(new Version(bundle.getVersion())) == 0) {
                        returnVal = bundleFile;
                    }
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

    private String getBsnFromMavenProject(MavenProject mavenProject) throws Exception {
        final File outputDir = new File(mavenProject.getBuild().getOutputDirectory());

        if (outputDir.exists()) {
            try (Jar jar = new Jar(outputDir)) {
                String bsn = jar.getBsn();

                return bsn;
            }
        }

        return null;
    }

    private void init() {
        inited = true;

        final IProgressMonitor monitor = new NullProgressMonitor();

        for (IMavenProjectFacade projectFacade : mavenProjectRegistry.getProjects()) {
            final IProject project = projectFacade.getProject();

            try {
                final MavenProject mavenProject = getMavenProject(projectFacade, monitor);

                final String bsn = getBsnFromMavenProject(mavenProject);

                if (bsn != null) {
                    Entry<IMavenProjectFacade,MavenProject> entry = new AbstractMap.SimpleImmutableEntry<>(projectFacade, mavenProject);
                    bsnMap.put(bsn, entry);
                }
            } catch (Exception e) {
                logger.logError("Unable to get bundle symbolic name for " + project.getName(), e);
            }
        }

        mavenProjectRegistry.addMavenProjectChangedListener(this);
    }

    private MavenProject getMavenProject(final IMavenProjectFacade projectFacade, final IProgressMonitor monitor) throws CoreException {
        MavenProject mavenProject = projectFacade.getMavenProject();

        if (mavenProject == null) {
            mavenProject = projectFacade.getMavenProject(monitor);
        }

        return mavenProject;
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
        if (!inited) {
            init();
        }

        List<Version> versions = new ArrayList<>();

        final Entry<IMavenProjectFacade,MavenProject> entry = bsnMap.get(bsn);

        if (entry != null) {
            final MavenProject mavenProject = entry.getValue();
            final File outputDir = new File(mavenProject.getBuild().getOutputDirectory());

            if (outputDir.exists()) {
                try (Jar jar = new Jar(outputDir)) {
                    Version version = new Version(jar.getVersion());
                    versions.add(version);
                } catch (Exception e) {
                    logger.logError("Unable to get version from " + mavenProject.getArtifactId(), e);
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
        return "Maven Build";
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
        if (events != null && events.length > 0) {
            for (MavenProjectChangedEvent event : events) {
                final IMavenProjectFacade oldProject = event.getOldMavenProject();

                final Iterator<Entry<String,Entry<IMavenProjectFacade,MavenProject>>> entries = bsnMap.entrySet().iterator();

                while (entries.hasNext()) {
                    final Entry<String,Entry<IMavenProjectFacade,MavenProject>> entry = entries.next();

                    if (entry.getValue().getKey().equals(oldProject)) {
                        String bsn = entry.getKey();

                        bsnMap.remove(bsn);
                        break;
                    }
                }

                final IMavenProjectFacade newProject = event.getMavenProject();

                try {
                    final MavenProject newMavenProject = getMavenProject(newProject, monitor);

                    final String newBsn = getBsnFromMavenProject(newMavenProject);
                    final Entry<IMavenProjectFacade,MavenProject> newEntry = new SimpleImmutableEntry<>(newProject, newMavenProject);

                    bsnMap.put(newBsn, newEntry);
                } catch (Exception e) {
                    logger.logError("Error getting bsn for new project " + newProject.getProject().getName(), e);
                }
            }
        }
    }

}
