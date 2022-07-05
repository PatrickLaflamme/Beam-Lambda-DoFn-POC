package ai.triton.platform.dofns

import org.apache.beam.sdk.transforms.Flatten
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PCollectionList

inline fun <reified T> PCollectionList<T>.flatten(name: String? = null): PCollection<T> {
    val resolvedName = name ?: "Flatten to PCollection of ${T::class.simpleName}"
    return this.apply(resolvedName, Flatten.pCollections())
}