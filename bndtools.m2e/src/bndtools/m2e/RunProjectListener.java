package bndtools.m2e;

import org.bndtools.api.LaunchListener;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;

@Component(immediate = true, service = LaunchListener.class)
public class RunProjectListener implements LaunchListener {

    @Override
    public void buildForLaunch(Project project) throws Exception {
        if (project instanceof Run) {
            Run run = (Run) project;

            run.getWorkspace().addBasicPlugin(new MavenWorkspaceRepository());
        }
    }

    @Override
    public void cleanup(Project project) throws Exception {
        if (project instanceof Run) {
            Run run = (Run) project;

            MavenWorkspaceRepository repo = run.getWorkspace().getPlugin(MavenWorkspaceRepository.class);

            if (repo != null) {
                repo.cleanup();
                run.getWorkspace().removeBasicPlugin(repo);
            }
        }
    }
}
