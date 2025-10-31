package ttt_game_service_tests;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import common.exagonal.Adapter;
import common.exagonal.InBoundPort;
import common.exagonal.OutBoundPort;

import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTests {
    
	@Test
    public void cleanArchitecture() {
    	JavaClasses importedClasses = new ClassFileImporter().importPackages("ttt_game_service");

    	var domainPackage = "..domain..";
    	var applicationPackage = "..application..";
    	var infrastructurePackage = "..infrastructure..";
    	
    	/* the domain should not depend on application/infrastructure */ 

    	var domainModelWithNoDeps = 
    			noClasses().that().resideInAPackage(domainPackage)
    			.should().dependOnClassesThat().resideInAPackage(applicationPackage)
    			.orShould().dependOnClassesThat().resideInAPackage(infrastructurePackage);
    	domainModelWithNoDeps.check(importedClasses);    

    	/* it must have a layered architecture */
    	
    	var layeredRule = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Domain").definedBy(domainPackage)
        .layer("Application").definedBy(applicationPackage)
        .layer("Infrastructure").definedBy(infrastructurePackage)
        .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
        .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application","Infrastructure");
    	layeredRule.check(importedClasses);    
    }	
	
	@Test
    public void hexagonalArchitecture() {
    
		/* it must have a clean architecture */
		cleanArchitecture();
		
		JavaClasses importedClasses = new ClassFileImporter().importPackages("ttt_game_service");

    	var domainPackage = "..domain..";
    	var applicationPackage = "..application..";
    	var infrastructurePackage = "..infrastructure..";
    	
    	/* all ports should be defined either in the application layer or in the domain layer */
    	
    	var portsRule = classes().that()
    				.areAnnotatedWith(InBoundPort.class).or()
    			  	.areAnnotatedWith(OutBoundPort.class)
    			  	.should().resideInAPackage(applicationPackage)
    			  	.orShould().resideInAPackage(domainPackage);
    	portsRule.check(importedClasses);
  
    	/* all adapters should be defined in the infrastructure layer */
    	
    	var adaptersRule = classes().that()
    				.areAnnotatedWith(Adapter.class)
    			  	.should().resideInAPackage(infrastructurePackage);
    	adaptersRule.check(importedClasses);
      	
    	
    }
}
