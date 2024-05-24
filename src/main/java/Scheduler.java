import constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

public class Scheduler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    public static void main(String[] args)
    {
        try (ZContext context = new ZContext())
        {
            ZMQ.Socket responder = context.createSocket(SocketType.REP);

            responder.bind("tcp://*:5555");

            while (!Thread.currentThread().isInterrupted())
            {
                byte[] request = responder.recv(0);

                String provisionedProfiles = new String(request);

                LOGGER.info("Received provisioned profiles: {}", provisionedProfiles);

                // Send the provisioned profiles to the poller
                Poller poller = new Poller(provisionedProfiles);

                String polledData = poller.pollData();

                // Encrypt the polled data
                byte[] dataBytes = polledData.getBytes();

                Key aesKey = new SecretKeySpec(Constants.AES_KEY.getBytes(), "AES");

                Cipher cipher = Cipher.getInstance("AES");

                cipher.init(Cipher.ENCRYPT_MODE, aesKey);

                byte[] encryptedBytes = cipher.doFinal(dataBytes);

                String encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);

                responder.send(encryptedData.getBytes(), 0);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error in scheduler: {}", e.getMessage());
        }
    }
}