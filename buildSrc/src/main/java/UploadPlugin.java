import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.internal.ErroringAction;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Uploads each publication of the project to bintray. Uses the bintray API directly rather than the bintray plugin, as the plugin does not
 * handle multiple packages per project.
 */
public class UploadPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        project.getPlugins().apply(NativePlatformComponentPlugin.class);
        project.getPlugins().apply("maven-publish");
        final BintrayCredentials credentials = project.getExtensions().create("bintray", BintrayCredentials.class);
        if (project.hasProperty("bintrayUserName")) {
            credentials.setUserName(project.property("bintrayUserName").toString());
        }
        if (project.hasProperty("bintrayApiKey")) {
            credentials.setApiKey(project.property("bintrayApiKey").toString());
        }

        final Callable<File> repoDir = new Callable<File>() {
            public File call() {
                return project.getRootProject().getLayout().getBuildDirectory().dir("repo").get().getAsFile();
            }
        };
        project.getExtensions().configure(PublishingExtension.class, new Action<PublishingExtension>() {
            @Override
            public void execute(PublishingExtension extension) {
                extension.getRepositories().maven(new ErroringAction<MavenArtifactRepository>() {
                    @Override
                    public void doExecute(MavenArtifactRepository repo) throws Exception {
                        repo.setUrl(repoDir.call());
                    }
                });
                extension.getPublications().withType(MavenPublication.class, new Action<MavenPublication>() {
                    @Override
                    public void execute(MavenPublication publication) {
                        UpdatePackageMetadataTask update = project.getTasks().create("updatePackage" + UploadTask.capitalize(publication.getName()), UpdatePackageMetadataTask.class);
                        update.setPublication(publication);

                        UploadTask upload = project.getTasks().create("uploadPackage" + UploadTask.capitalize(publication.getName()), UploadTask.class);
                        upload.setGroup("Upload");
                        upload.setDescription("Upload publication " + publication.getName());
                        upload.setLocalRepoDir(repoDir);
                        upload.setPublication(publication);
                        upload.dependsOn(update);
                        if (!publication.getName().equals("main")) {
                            Task uploadLifecycle = project.getTasks().maybeCreate("uploadJni");
                            uploadLifecycle.setGroup("Upload");
                            uploadLifecycle.setDescription("Upload all JNI publications");
                            uploadLifecycle.dependsOn(upload);
                        }

                    }
                });
            }
        });
        project.getGradle().getTaskGraph().whenReady(new Action<TaskExecutionGraph>() {
            @Override
            public void execute(TaskExecutionGraph graph) {
                for (Task task : graph.getAllTasks()) {
                    if (task instanceof BintrayTask) {
                        credentials.assertPresent();
                        BintrayTask bintrayTask = (BintrayTask) task;
                        bintrayTask.setUserName(credentials.getUserName());
                        bintrayTask.setApiKey(credentials.getApiKey());
                    }
                }
            }
        });
    }
}
