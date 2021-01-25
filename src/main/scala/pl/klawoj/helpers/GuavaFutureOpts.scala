package pl.klawoj.helpers

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.{Future, Promise}

object GuavaFutureOpts {

  implicit final class guavaFutureOpts[A](val guavaFut: ListenableFuture[A]) extends AnyVal {
    def asScala(): Future[A] = {
      val p = Promise[A]()
      val callback = new FutureCallback[A] {
        override def onSuccess(a: A): Unit = p.success(a)

        override def onFailure(err: Throwable): Unit = p.failure(err)
      }
      Futures.addCallback(guavaFut, callback)
      p.future
    }
  }

}