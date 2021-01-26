package pl.klawoj.helpers.cassandra

import akka.actor.{ActorSystem, Scheduler}
import akka.pattern.retry
import com.datastax.driver.core._
import com.datastax.driver.core.policies.{ConstantReconnectionPolicy, LatencyAwarePolicy, RoundRobinPolicy, TokenAwarePolicy}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

object CassandraSession {
  private val cassandraConfig = CassandraConfig.load()
  private val CheckSessionRetries = 10
  private val CheckSessionRetryInterval = 10.seconds

  private val poolingOptions: PoolingOptions =
    new PoolingOptions()
      .setConnectionsPerHost(HostDistance.LOCAL, 32, 64)
      .setConnectionsPerHost(HostDistance.REMOTE, 32, 64)
      .setMaxRequestsPerConnection(HostDistance.LOCAL, 4096)
      .setMaxRequestsPerConnection(HostDistance.REMOTE, 4096)
      .setMaxQueueSize(512)

  private val queryOptions =
    new QueryOptions()
      .setFetchSize(1000)

  def cluster: Cluster = Cluster.builder
    .addContactPoints(cassandraConfig.seeds: _*)
    .withPort(cassandraConfig.port)
    .withCredentials(cassandraConfig.user, cassandraConfig.password)
    .withProtocolVersion(ProtocolVersion.V4)
    .withPoolingOptions(poolingOptions)
    .withQueryOptions(queryOptions)
    .withReconnectionPolicy(new ConstantReconnectionPolicy(1000))
    .withLoadBalancingPolicy(LatencyAwarePolicy.builder(new TokenAwarePolicy(new RoundRobinPolicy()))
      .withExclusionThreshold(2.0)
      .withScale(100, TimeUnit.MILLISECONDS)
      .withRetryPeriod(10, TimeUnit.SECONDS)
      .withUpdateRate(100, TimeUnit.MILLISECONDS)
      .withMininumMeasurements(50)
      .build())
    .build

  implicit lazy val session: Session =
    cluster.connect()

  def asyncSession(implicit system: ActorSystem, executionContext: ExecutionContext): Future[Session] = {
    implicit val scheduler: Scheduler = system.scheduler

    def check(): Future[Session] = {
      Future {
        session
      }
    }

    retry(() ⇒ check(), CheckSessionRetries, CheckSessionRetryInterval)
  }
}
