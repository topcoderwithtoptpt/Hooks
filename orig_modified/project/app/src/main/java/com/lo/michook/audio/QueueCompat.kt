package com.lo.michook.audio

import java.util.concurrent.LinkedBlockingQueue

/** Build-time compatibility shim for the queue API used by this project. */
internal fun LinkedBlockingQueue<ByteArray>.offerFirst(element: ByteArray): Boolean = offer(element)
