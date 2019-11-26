import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.component.SoftwareComponentInternal;

import javax.inject.Inject;
import java.util.Collections;

public abstract class NativePlatformComponentPlugin implements Plugin<Project> {
    @Inject
    public abstract ImmutableAttributesFactory getAttributesFactory();

    @Override
    public void apply(Project project) {
        project.getPlugins().apply("java");
        SoftwareComponent javaComponent = project.getComponents().getByName("java");
        project.getComponents().add(new NativePlatformComponent((SoftwareComponentInternal) javaComponent, Collections.singleton("osx-amd64"), getAttributesFactory()));
    }
}
