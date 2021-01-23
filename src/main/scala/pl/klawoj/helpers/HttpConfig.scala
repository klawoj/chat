package pl.klawoj.helpers

import com.typesafe.config.Config
import pl.klawoj.helpers.HttpConfig.{BindingHostPath, BindingPortPath, MaxSegmentLengthPath}

class HttpConfig(config: Config) {
  val bindingHost: String = config.getString(BindingHostPath)
  val bindingPort: Int = config.getInt(BindingPortPath)
  val maxSegmentLength: Int = config.getInt(MaxSegmentLengthPath)
}

object HttpConfig {
  val BindingHostPath = "binding.host"
  val BindingPortPath = "binding.port"
  val MaxSegmentLengthPath = "parsing.max-segment-length"
  val ConfigPath = "chat.http"

  def load(configLoader: ConfigLoader = DefaultConfigLoader): HttpConfig =
    new HttpConfig(configLoader.load(ConfigPath))

  def load(config: Config): HttpConfig =
    new HttpConfig(config)
}
