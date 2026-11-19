package ro.wasteless;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  @Test
  void commonMustNotDependOnFeatures() {
    var classes = new ClassFileImporter().importPackages("ro.wasteless");
    noClasses()
        .that()
        .resideInAPackage("ro.wasteless.common..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "ro.wasteless.auth..",
            "ro.wasteless.user..",
            "ro.wasteless.business..",
            "ro.wasteless.offer..",
            "ro.wasteless.order..",
            "ro.wasteless.payment..",
            "ro.wasteless.media..")
        .check(classes);
  }
}
