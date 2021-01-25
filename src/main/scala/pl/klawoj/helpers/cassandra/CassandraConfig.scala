package pl.klawoj.helpers.cassandra

import com.typesafe.config.Config
import pl.klawoj.helpers.cassandra.CassandraConfig._
import pl.klawoj.helpers.{ConfigLoader, DefaultConfigLoader}

import scala.jdk.CollectionConverters._

class CassandraConfig(config: Config) {
  def seeds: List[String] =
    config.getStringList(SeedsPath).asScala.toList

  def seedsString: String =
    seeds.mkString(SeedsSeparator)

  def port: Int =
    config.getInt(PortPath)

  def schemaName: String =
    config.getString(SchemaNamePath)

  def user: String =
    config.getString(UserPath)

  def password: String =
    config.getString(PasswordPath)
}

object CassandraConfig {
  val ConfigPath = "chat.cassandra"
  val SeedsPath = "seeds"
  val PortPath = "port"
  val SchemaNamePath = "schema.name"
  val UserPath = "user"
  val PasswordPath = "password"
  val SeedsSeparator = ","

  def load(configLoader: ConfigLoader = DefaultConfigLoader): CassandraConfig =
    new CassandraConfig(configLoader.load(ConfigPath))

  def load(config: Config): CassandraConfig =
    new CassandraConfig(config)
}