package ai.triton.platform

import org.apache.beam.sdk.transforms.Flatten
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PCollectionList

operator fun <T> PCollection<T>.plus(other: PCollection<T>): PCollection<T> {
    return PCollectionList.of(this)
        .and(other)
        .apply("Concatenate ${this.name} and ${other.name} PCollections", Flatten.pCollections())
}