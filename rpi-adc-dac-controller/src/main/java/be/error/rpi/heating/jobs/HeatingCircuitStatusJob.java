package be.error.rpi.heating.jobs;

import static be.error.rpi.config.RunConfig.getInstance;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.GroupAddress;

import be.error.rpi.ebus.EbusdTcpCommunicatorImpl;
import be.error.rpi.ebus.commands.GetHeatingCircuitHeatingDemand;
import be.error.rpi.knx.Support;

public class HeatingCircuitStatusJob implements Job {

	private static final Logger logger = LoggerFactory.getLogger(HeatingCircuitStatusJob.class);

	private GroupAddress heatingCircuitStatusGa = Support.createGroupAddress("10/3/0");

	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		try {
			logger.debug("Requesting heating circuit status");
			GetHeatingCircuitHeatingDemand getHeatingCircuitHeatingDemand = new GetHeatingCircuitHeatingDemand();
			boolean result = getHeatingCircuitHeatingDemand.convertResult(new EbusdTcpCommunicatorImpl().send(getHeatingCircuitHeatingDemand));
			logger.debug("Communicating requesting heating circuit status " + result + " " + heatingCircuitStatusGa);
			getInstance().getKnxConnectionFactory().createProcessCommunicator().write(heatingCircuitStatusGa, result);
		} catch (Exception e) {
			logger.error(HeatingCircuitStatusJob.class.getSimpleName() + " exception", e);
			throw new JobExecutionException(e);
		}
	}
}