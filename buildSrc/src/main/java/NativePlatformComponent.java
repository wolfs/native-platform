import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Usage;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.component.ComponentWithCoordinates;
import org.gradle.api.component.ComponentWithVariants;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.component.SoftwareComponentInternal;
import org.gradle.api.internal.component.UsageContext;
import org.gradle.internal.component.external.model.ImmutableCapability;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class NativePlatformComponent implements SoftwareComponentInternal, ComponentWithVariants {

    private final SoftwareComponentInternal javaComponent;
    private final Set<String> variants;
    private final ImmutableAttributesFactory attributesFactory;

    public NativePlatformComponent(SoftwareComponentInternal javaComponent, Set<String> variants, ImmutableAttributesFactory attributesFactory) {
        this.javaComponent = javaComponent;
        this.variants = variants;
        this.attributesFactory = attributesFactory;
    }

    @Override
    public Set<? extends SoftwareComponent> getVariants() {
        return variants.stream()
                .map(this::createVariant)
                .collect(Collectors.toSet());
    }

    private SoftwareComponentInternal createVariant(String variant) {
        return new NativePlatformSoftwareComponentInternal(variant);
    }


    @Override
    public Set<? extends UsageContext> getUsages() {
        return javaComponent.getUsages();
    }

    @Override
    public String getName() {
        return "nativePlatform";
    }

    private class NativePlatformSoftwareComponentInternal implements SoftwareComponentInternal, ComponentWithCoordinates {
        private final String variant;

        public NativePlatformSoftwareComponentInternal(String variant) {
            this.variant = variant;
        }

        @Override
        public Set<? extends UsageContext> getUsages() {
            HashSet<UsageContext> usages = new HashSet<>();
            usages.add(new UsageContext() {
                @Override
                public Usage getUsage() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Set<? extends PublishArtifact> getArtifacts() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Set<? extends ModuleDependency> getDependencies() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Set<? extends DependencyConstraint> getDependencyConstraints() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Set<? extends Capability> getCapabilities() {
                    return Collections.singleton(new ImmutableCapability("net.rubygrapefruit", "native-platform-" + variant, "0.19-dev"));
                }

                @Override
                public Set<ExcludeRule> getGlobalExcludes() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getName() {
                    return variant + "Elements";
                }

                @Override
                public AttributeContainer getAttributes() {
                    return attributesFactory.of(Attribute.of(Usage.USAGE_ATTRIBUTE.getName(), String.class), "variant");
                }
            });
            return usages;
        }

        @Override
        public String getName() {
            return variant + "Elements";
        }

        @Override
        public ModuleVersionIdentifier getCoordinates() {
            return DefaultModuleVersionIdentifier.newId("net.rubygrapefruit", "native-platform-" + variant, "0.19-dev");
        }
    }
}
