package cloud.fengdu.ticketing.eventregistry.autoconfigure;

import java.io.IOException;
import java.security.GeneralSecurityException;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for NATS.
 */
@ConditionalOnClass({ Connection.class })
@EnableConfigurationProperties(NatsProperties.class)
/**
 * NatsAutoConfiguration will create a NATS connection from an instance of NatsProperties.
 * A default connection and error handler is provided with basic logging.
 */
public class NatsAutoConfiguration {
	private static final Log logger = LogFactory.getLog(NatsAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	/**
	 * @return NATS connection created with the provided properties. If no server URL is set the method will return null.
	 * @throws IOException when a connection error occurs
	 * @throws InterruptedException in the unusual case of a thread interruption during connect
	 * @throws GeneralSecurityException if there is a problem authenticating the connection
	 */
	public Connection natsConnection(NatsProperties properties) throws IOException, InterruptedException, GeneralSecurityException {
		Connection nc = null;
		String serverProp = (properties != null) ? properties.getServer() : null;

		if (serverProp == null || serverProp.length() == 0) {
			return null;
		}

		try {
			logger.info("autoconnecting to NATS with properties - " + properties);
			Options.Builder builder = properties.toOptionsBuilder();

			builder = builder.connectionListener(new ConnectionListener() {
				public void connectionEvent(Connection conn, Events type) {
						logger.info("NATS connection status changed " + type);
				}
			});

			builder = builder.errorListener(new ErrorListener() {
				@Override
				public void slowConsumerDetected(Connection conn, Consumer consumer) {
					logger.info("NATS connection slow consumer detected");
				}

				@Override
				public void exceptionOccurred(Connection conn, Exception exp) {
					logger.info("NATS connection exception occurred", exp);
				}

				@Override
				public void errorOccurred(Connection conn, String error) {
					logger.info("NATS connection error occurred " + error);
				}
			});

			nc = Nats.connect(builder.build());
		}
		catch (Exception e) {
			logger.info("error connecting to nats", e);
			throw e;
		}
		return nc;
	}

}

