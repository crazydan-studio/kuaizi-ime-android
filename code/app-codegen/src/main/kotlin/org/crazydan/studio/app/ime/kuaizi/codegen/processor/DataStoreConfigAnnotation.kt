package org.crazydan.studio.app.ime.kuaizi.codegen.processor

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DataStoreConfig(
    val prefix: String = "",
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class DataStoreKey(
    val name: String = "",
)
