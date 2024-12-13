plugins {
  id("qupath.common-conventions")
  id("qupath.publishing-conventions")
  id("qupath.javafx-conventions")
  `java-library`
}

extra["moduleName"] = "qupath.gui.fx"
base {
    archivesName = "qupath-gui-fx"
    description = "Main QuPath user interface."
}

dependencies {
  api(project(":qupath-core"))
  api(project(":qupath-core-processing"))

  api(libs.controlsfx)

  implementation(libs.qupath.fxtras)

  implementation(libs.guava)

  implementation(libs.snakeyaml)
  implementation(libs.picocli)
  
  implementation(libs.jfxtras)
  implementation(libs.commons.text)
  implementation(libs.commons.math)

  implementation(libs.bundles.ikonli)
  
  implementation(libs.bundles.markdown)

  implementation(libs.bundles.logviewer)


  implementation(libs.javadocviewer)
  
  implementation("org.json:json:20210307")
  implementation("com.squareup.okhttp3:okhttp:4.9.3")
  implementation("com.google.code.gson:gson:2.8.8")

}