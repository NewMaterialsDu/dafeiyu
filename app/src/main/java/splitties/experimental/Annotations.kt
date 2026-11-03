/*
 * Copyright 2019 Louis Cognault Ayeva Derman. Use of this source code is governed by the Apache 2.0 license.
 */

package splitties.experimental

@RequiresOptIn
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
annotation class ExperimentalSplittiesApi

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
annotation class InternalSplittiesApi
