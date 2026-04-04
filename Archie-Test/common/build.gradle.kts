architectury {
	common("fabric", "neoforge")
}

dependencies {
	modImplementation(libs.fabric.loader)
	modCompileOnly(libs.catalogue.common)
	modCompileOnly(libs.clothConfig.common)
	modCompileOnly(libs.yacl.common)
	modImplementation(libs.architectury.common)
	modImplementation(libs.storage.common)
	modImplementation(libs.storage.resources.common)
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-archie-test")
}
