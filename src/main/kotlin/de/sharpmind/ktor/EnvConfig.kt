package de.sharpmind.ktor

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Environment config main class
 *
 * todo description
 */
@KtorExperimentalAPI
object EnvConfig {
    private const val ROOT_NODE = "envConfig"
    private const val ENVIRONMENT_NODE = "env"
    private const val DEFAULT_ENVIRONMENT = "default"
    private const val EC_CONFIGFILE = "EC_CONFIGFILE"

    /* todo  in case of an override use ConfigFactory.load("conf/url_short").withFallback(ConfigFactory.defaultApplication()).resolve()
    private val urlShortConfig: Config =
        ConfigFactory.load("conf/url_short").withFallback(ConfigFactory.defaultApplication()).resolve()*/
    private lateinit var config: ApplicationConfig
    private lateinit var externalConfig: Config
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var environment: String = DEFAULT_ENVIRONMENT

    /**
     * Initialized env config with the ktor config object
     *
     * @param config the application config (hacoon format)
     * @return EnvConfig object itself for call chaining
     */
    fun initConfig(config: ApplicationConfig): EnvConfig =
        apply {
            this.config = config
            // if the environment property is available, we set it, if not use default
            this.environment = config.propertyOrNull("$ROOT_NODE.$ENVIRONMENT_NODE")?.getString() ?: DEFAULT_ENVIRONMENT

            logger.info("use environment: $environment")

            setExternalConfig()
        }

    /**
     * Gets boolean property from config with default fallback
     *
     * @return the default property for provided key or null if it doesn't exist
     */
    fun getBoolean(propertyKey: String): Boolean? {
        var value: Boolean? = getBooleanExternal(propertyKey)

        return value ?:
               getBooleanInternal(environment, propertyKey) ?: getBooleanInternal(DEFAULT_ENVIRONMENT, propertyKey)
    }

    /**
     * Gets int property from config with default fallback
     *
     * @return the int property for provided key or null if it doesn't exist
     */
    fun getInt(propertyKey: String): Int? {
        var value: Int? = getIntExternal(propertyKey)

        return value ?:
               (getIntInternal(environment, propertyKey) ?: getIntInternal(DEFAULT_ENVIRONMENT, propertyKey))
    }

    /**
     * Gets list property from config with default fallback
     *
     * @return the list property for provided key or null if it doesn't exist
     */
    fun getList(propertyKey: String): List<String>? {
        var value: List<String>? = getListExternal(propertyKey)

        return value ?:
               getListInternal(environment, propertyKey) ?: getListInternal(DEFAULT_ENVIRONMENT, propertyKey)
    }

    /**
     * Gets string property from config with default fallback
     *
     * @return the string property for provided key or null if it doesn't exist
     */
    fun getString(propertyKey: String): String? {
        var value: String? = getStringExternal(propertyKey)

        return value ?:
               getStringInternal(environment, propertyKey) ?: getStringInternal(DEFAULT_ENVIRONMENT, propertyKey)
    }

    /**
     * Gets boolean property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the default property for provided key or defaultVal if it doesn't exist
     */
    fun getBoolean(propertyKey: String, defaultVal: Boolean): Boolean = getBoolean(propertyKey) ?: defaultVal

    /**
     * Gets int property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the int property for provided key or defaultVal if it doesn't exist
     */
    fun getInt(propertyKey: String, defaultVal: Int): Int = getInt(propertyKey) ?: defaultVal

    /**
     * Gets list property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the list property for provided key or defaultVal if it doesn't exist
     */
    fun getList(propertyKey: String, defaultVal: List<String>): List<String> = getList(propertyKey) ?: defaultVal

    /**
     * Gets string property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the string property for provided key or defaultVal if it doesn't exist
     */
    fun getString(propertyKey: String, defaultVal: String): String = getString(propertyKey) ?: defaultVal

    private fun getBooleanInternal(environment: String, propertyKey: String): Boolean? =
        getStringInternal(environment, propertyKey)?.toBoolean()

    private fun getIntInternal(environment: String, propertyKey: String): Int? =
        getStringInternal(environment, propertyKey)?.toInt()

    private fun getListInternal(environment: String, propertyKey: String): List<String>? =
        getPropertyInternal(environment, propertyKey)?.getList()

    private fun getStringInternal(environment: String, propertyKey: String): String? =
        getPropertyInternal(environment, propertyKey)?.getString()

    private fun getBooleanExternal(valuePath: String): Boolean? {
        // do we have an external config?
        if (::externalConfig.isInitialized) {
            try {
                return externalConfig.getBoolean(valuePath)
            }
            catch (e: ConfigException){
                logger.warn(e.message)
            }
        }
        return null
    }

    private fun getIntExternal(valuePath: String): Int? {
        // do we have an external config?
        if (::externalConfig.isInitialized) {
            try {
                return externalConfig.getInt(valuePath)
            }
            catch (e: ConfigException){
                logger.warn(e.message)
            }
        }
        return null
    }

    private fun getListExternal(valuePath: String): List<String>? {
        // do we have an external config?
        if (::externalConfig.isInitialized) {
            try {
                return externalConfig.getStringList(valuePath)
            }
            catch (e: ConfigException){
                logger.warn(e.message)
            }
        }
        return null
    }

    private fun getStringExternal(valuePath: String): String? {
        // do we have an external config?
        if (::externalConfig.isInitialized) {
            try {
                return externalConfig.getString(valuePath)
            }
            catch (e: ConfigException){
                logger.warn(e.message)
            }
        }
        return null
    }

    /**
     * Get property from a application.conf config file, for specified environment
     *
     * @return the application config value for the provided environment and key - or null if key is not found
     * @throws Exception if EnvConfig is not initialized yet
     */
    private fun getPropertyInternal(environment: String, propertyKey: String): ApplicationConfigValue? {
        // do we have a config?
        if (!::config.isInitialized) {
            logger.warn("EnvConfig not initialized yet")

            throw Exception("Not initialized yet")
        }

        val path = "${ROOT_NODE}.${environment}.$propertyKey"

        return config.propertyOrNull(path)
    }

    /**
     * used to create a config object from  an external .conf file,
     * provided via System environment values
     */
    private fun setExternalConfig() {
        try {
            val externalConfigFilePath = System.getenv(EC_CONFIGFILE)
            val configFile = File("", externalConfigFilePath)

            if (configFile.exists() && configFile.canRead()) {
                externalConfig = ConfigFactory.parseFile(configFile)

                logger.info("external config available")
            }
        } catch (e: NullPointerException) {
            logger.warn("no external config file provided")
        } catch (e: SecurityException) {
            logger.warn("access denied for the external config file's env. variable")
        } catch (e: Exception) {
            logger.warn("handleExtraConfigFile() exception: ${e.message}")
        }
    }
}

