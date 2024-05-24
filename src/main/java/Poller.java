import constants.Constants;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class Poller
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Poller.class);

    private final String provisionedProfiles;

    public Poller(String provisionedProfiles)
    {
        this.provisionedProfiles = provisionedProfiles;
    }

    public String pollData()
    {
        LOGGER.info("Received provisioned profiles: {}", provisionedProfiles);

        var encodedData=Base64.getEncoder().encodeToString(provisionedProfiles.getBytes(StandardCharsets.UTF_8));

        // Poll data from devices using the plugin engine
        String polledData = executePluginEngine(encodedData);

        LOGGER.info("Polled data: {}", polledData);

        return polledData;
    }

    private String executePluginEngine(String encodedJson)
    {
        try
        {
            var process = new ProcessBuilder(Constants.PLUGIN_ENGINE_PATH, encodedJson)
                    .redirectErrorStream(true)
                    .start();

            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            var output = new StringBuilder();

            if (!process.waitFor(Constants.WAITING_TIME, TimeUnit.SECONDS))
            {
                process.destroyForcibly();
            }

            String line;

            while ((line = reader.readLine()) != null)
            {
                output.append(line);
            }

            var decodedString = new String(Base64.getDecoder().decode(output.toString()), StandardCharsets.UTF_8);

            LOGGER.debug("Decoded Result: {}", decodedString);

            return new JsonArray(decodedString).encode();
        }
        catch (Exception exception)
        {
            LOGGER.error("Error executing plugin engine: {}", exception.getMessage());

            return null;
        }
    }
}