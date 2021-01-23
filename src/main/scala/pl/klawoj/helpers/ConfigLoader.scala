package pl.klawoj.helpers

import com.typesafe.config.{Config, ConfigFactory}

trait ConfigLoader {
  def load(path: String): Config
}

object DefaultConfigLoader extends ConfigLoader {
  def load(path: String): Config = ConfigFactory.load().getConfig(path)
}
